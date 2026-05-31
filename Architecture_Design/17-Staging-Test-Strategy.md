# Staging 环境验证方案

> 生成日期：2026-05-31
> 目的：利用本地 Docker 运行的真实基础设施（PostgreSQL + Kafka + Redis）进行 staging 验证

---

## 一、背景

当前所有测试使用 **H2 内存数据库 + EmbeddedKafka**，无法验证：
- 真实 PostgreSQL 的 JSON 序列化差异
- 连接池行为和事务超时
- Kafka 消息在真实 broker 上的路由和消费
- 多服务同时运行时的集成问题

用户已有 Docker 运行的 staging 基础设施：

| 服务 | 地址 | 用途 |
|------|------|------|
| PostgreSQL | `localhost:5432` | 7 个独立数据库 |
| Kafka | `localhost:29092` | 消息 broker |
| Kafka UI | `http://localhost:9000` | 消息可视化 |
| Redis | `localhost:6379` | 缓存（待用） |
| Prometheus | `http://localhost:9090` | 监控 |
| Grafana | `http://localhost:3000` | 可视化 |
| Jaeger | `http://localhost:16686` | 链路追踪 |

---

## 二、两种策略

| 策略 | 描述 | 适用场景 |
|------|------|---------|
| **A. Spring Profile 切换** | 每个模块添加 `staging` profile，指向真实 PostgreSQL + Kafka | 本地开发机手动验证 |
| **B. GitHub Actions Staging Job** | CI 增加 staging job，用 service containers 启动 PostgreSQL + Kafka | 自动化验证 |

**推荐路径：先 A 后 B**

1. 先加 staging profile，本地手动验证跑通
2. 再把 staging job 加到 GitHub Actions 自动化

---

## 三、方案 A：Spring Profile 切换（本地验证）

### 3.1 每个 main/application.yml 新增 `staging` profile

在每个模块的 `src/main/resources/application.yml` 末尾添加：

```yaml
---
# Staging 环境：连接真实 Docker infra
spring:
  config:
    activate:
      on-profile: staging
  datasource:
    url: jdbc:postgresql://localhost:5432/library_{module}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:dev_pg_2026}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  kafka:
    bootstrap-servers: localhost:29092
```

其中 `{module}` 替换为：`catalog`, `inventory`, `circulation`, `patron`, `payment`, `analytics`, `notification`

### 3.2 每个 test/application.yml 新增 `staging-kafka` profile

在已有的 `embedded-kafka` profile 后面添加：

```yaml
---
# Staging 测试：连接真实 Kafka（不启动 EmbeddedKafka）
spring:
  config:
    activate:
      on-profile: staging-kafka
  autoconfigure:
    exclude: []
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: library-{module}-staging-test
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
  datasource:
    url: jdbc:postgresql://localhost:5432/library_{module}_test
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:dev_pg_2026}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### 3.3 staging 测试类

为需要连接真实 Kafka 的测试创建 staging 版本，使用 `@ActiveProfiles("staging-kafka")`：
- 不需要 `@EmbeddedKafka` 注解（连接真实 broker）
- 不需要手动创建 KafkaTemplate（Spring Boot 自动配置）
- 测试数据写入真实 PostgreSQL

### 3.4 使用方式

```bash
# 前置：确保 Docker infra 运行中
docker ps | grep -E "postgres|kafka"

# 方式 1：运行单个模块的 staging 测试
cd library-patron
mvn test -Dtest=CirculationEventConsumerIntegrationTest -Dspring.profiles.active=staging-kafka

# 方式 2：启动单个服务，用 curl/Postman 手动验证
cd library-catalog
mvn spring-boot:run -Dspring-boot.run.profiles=staging

# 方式 3：启动所有 7 个服务（需要不同终端）
cd library-catalog && mvn spring-boot:run -Dspring-boot.run.profiles=staging &
cd library-inventory && mvn spring-boot:run -Dspring-boot.run.profiles=staging &
# ... 其他模块类似
```

### 3.5 需要修改的文件清单

| 文件 | 操作 |
|------|------|
| 7 个 `src/main/resources/application.yml` | 添加 `staging` profile 段 |
| 7 个 `src/test/resources/application.yml` | 添加 `staging-kafka` profile 段 |
| `library-e2e-test/src/test/resources/application.yml` | 添加 `staging` profile 段 |
| `library-integration-test/src/test/resources/application.yml` | 添加 `staging` profile 段 |

### 3.6 前置条件

运行 staging 测试前需要：

```bash
# 创建 7 个业务数据库
for db in library_catalog library_inventory library_circulation library_patron library_payment library_analytics library_notification; do
  psql -h localhost -U postgres -c "CREATE DATABASE $db;" 2>/dev/null || echo "$db already exists"
done

# 可选：创建测试专用数据库（带 _test 后缀，避免污染业务数据）
for db in library_catalog_test library_inventory_test library_circulation_test library_patron_test library_payment_test library_analytics_test library_notification_test; do
  psql -h localhost -U postgres -c "CREATE DATABASE $db;" 2>/dev/null || echo "$db already exists"
done
```

---

## 四、方案 B：GitHub Actions Staging Job（自动化）

### 4.1 在 `.github/workflows/ci.yml` 新增 staging job

```yaml
  staging:
    needs: build
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: dev_pg_2026
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      zookeeper:
        image: confluentinc/cp-zookeeper:7.5.0
        env:
          ZOOKEEPER_CLIENT_PORT: 2181
        ports:
          - 2181:2181

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        env:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
        ports:
          - 29092:29092
        options: >-
          --health-cmd "kafka-topics --bootstrap-server localhost:9092 --list"
          --health-interval 10s
          --health-timeout 10s
          --health-retries 5

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build all modules
        run: mvn clean install -DskipTests -B

      - name: Create databases
        run: |
          for db in library_catalog library_inventory library_circulation library_patron library_payment library_analytics library_notification; do
            PGPASSWORD=dev_pg_2026 psql -h localhost -U postgres -c "CREATE DATABASE $db;"
          done

      - name: Run E2E tests against staging infra
        run: mvn test -Dspring.profiles.active=staging -pl library-e2e-test -B

      - name: Run integration tests against staging infra
        run: mvn test -Dspring.profiles.active=staging -pl library-integration-test -B

      - name: Upload staging test reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: staging-test-reports
          path: '*/target/surefire-reports/*.txt'
          retention-days: 7
```

### 4.2 Service Containers 说明

| 容器 | 镜像 | 端口映射 | 用途 |
|------|------|---------|------|
| postgres | `postgres:16` | 5432:5432 | 真实数据库 |
| zookeeper | `cp-zookeeper:7.5.0` | 2181:2181 | Kafka 依赖 |
| kafka | `cp-kafka:7.5.0` | 29092:29092 | 真实消息 broker |

注意：service containers 之间通过 Docker 网络互通，所以 Kafka 的 `KAFKA_ZOOKEEPER_CONNECT` 用 `zookeeper:2181`（容器名），而 `KAFKA_ADVERTISED_LISTENERS` 用 `localhost:29092`（让测试代码能连上）。

---

## 五、验证步骤

### 本地手动验证（方案 A）

1. `docker ps` 确认 postgres + kafka 运行
2. 创建 7 个数据库（见 3.6）
3. `mvn test -Dspring.profiles.active=staging-kafka -pl library-patron` 跑单模块
4. Kafka UI (`http://localhost:9000`) 确认消息收发
5. `psql` 确认数据写入正确

### CI 自动化验证（方案 B）

1. Push 到 GitHub
2. `build` job 先跑 H2 + EmbeddedKafka 测试（快速反馈）
3. `staging` job 再跑真实 infra 测试（深度验证）
4. 两个 job 都通过才算成功

---

## 六、实施优先级

| 优先级 | 任务 | 预估工作量 |
|:------:|------|:---------:|
| P0 | 方案 A：7 个 main yml 加 staging profile | 30 分钟 |
| P0 | 方案 A：7 个 test yml 加 staging-kafka profile | 30 分钟 |
| P1 | 本地手动验证跑通 | 1 小时 |
| P2 | 方案 B：GitHub Actions staging job | 1 小时 |
| P3 | 补充 Jaeger/Grafana 集成 | 2 小时 |
