# 17 - Staging 环境验证策略

> 日期：2026-06-01
> 状态：✅ 方案 A + 方案 C 已实施

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
| PostgreSQL | `localhost:5432` | 7 个业务数据库 + 1 个测试数据库 |
| Kafka | `localhost:29092` | 消息 broker |
| Kafka UI | `http://localhost:9000` | 消息可视化 |
| Redis | `localhost:6379` | 缓存（预留，当前未使用） |
| Prometheus | `http://localhost:9090` | 监控 |
| Grafana | `http://localhost:3000` | 可视化 |
| Jaeger | `http://localhost:16686` | 链路追踪 |

---

## 二、策略选择

| 策略 | 描述 | 状态 |
|------|------|------|
| **A. 独立 staging-test 模块** | 新建 `library-staging-test`，直连真实 PostgreSQL + Kafka | ✅ 已实施 |
| B. Spring Profile 切换（原方案） | 每个模块添加 `staging` profile | ❌ 未实施（方案 A 更优） |
| C. GitHub Actions Staging Job | CI 增加 staging job，用 service containers | ✅ 已实施 |

**选择方案 A 的原因**：
- 完全独立模块，不修改任何现有模块代码
- Maven Profile 隔离，默认构建不受影响
- 测试 case 复用 `library-integration-test` 的 BDD feature 文件

---

## 三、已实施方案 A：独立 staging-test 模块

### 3.1 模块概览

| 属性 | 值 |
|------|------|
| 模块名 | `library-staging-test` |
| Maven Profile | `-Pstaging` 激活 |
| 测试框架 | Cucumber BDD（7 个 feature，9 个 scenario） |
| 数据库 | 真实 PostgreSQL `localhost:5432/library_staging_test` |
| 消息 | 真实 Kafka `localhost:29092` |
| 包名 | `com.library.staging` |

### 3.2 测试覆盖的场景

| Feature | 场景 | 涉及的 Bounded Context |
|---------|------|----------------------|
| borrow-book | 借书 → 更新 patron 贷款数 + 通知 | Circulation → Patron, Notification |
| return-book | 还书 → 减少 patron 贷款数 | Circulation → Patron, Notification |
| hold-book | 预约/取消预约 | Circulation → Patron |
| new-book | 新书 → 创建库存记录 | Catalog → Inventory |
| fine-payment | 罚款/缴费 → 更新 patron 罚款余额 | Circulation, Payment → Patron |
| low-stock-alert | 低库存预警 → 通知 | Inventory → Notification |
| patron-suspension | 停权 | Patron → Notification |

### 3.3 环境管理

#### 测试前（`@Before` 首轮场景）
1. Kafka：删除 5 个旧 topics → 重建 5 个 topics（3 partitions）→ 删除 consumer groups
2. 等待所有 Kafka consumer 获得 partition assignment
3. PostgreSQL：DELETE 所有表数据（reverse FK order）

#### 测试前（`@Before` 每个场景）
1. PostgreSQL：DELETE 所有表数据
2. 打印 BEFORE 快照（PostgreSQL 行数 + Kafka offset + Redis 状态）

#### 测试后（`@After`）
1. 打印 AFTER 快照（显示测试产生的数据变化 + PASSED/FAILED 状态）

#### 全部结束后（`@AfterAll`）
1. 停止所有 Kafka consumer containers
2. Kafka：删除 5 个 topics + 删除所有 consumer groups
3. PostgreSQL：`ddl-auto: create-drop` 自动删除所有表

#### 环境恢复保证

| 基础设施 | 恢复到测试前 |
|---------|-------------|
| PostgreSQL 表结构 | ✅ create-drop（启动建表，关闭删表） |
| PostgreSQL 数据 | ✅ @Before 每场景清空 |
| Kafka 消息 | ✅ @AfterAll 删除 topics，@Before 重建 |
| Kafka Consumer Groups | ✅ @AfterAll 停止 consumers 后删除 groups |
| Redis | — 暂未使用 |

### 3.4 实时数据监控

`StagingEnvironmentInspector` 在每个 Scenario 前后自动打印环境快照：

- **PostgreSQL**：11 张表的行数（`SELECT COUNT(*)`）
- **Kafka**：5 个 topic 的 end offset（消息总数）+ consumer group 状态
- **Redis**：placeholder（项目未使用，预留扩展）

### 3.5 使用方式

```bash
# 前置：确保 Docker infra 运行 + 创建数据库
docker ps | grep -E "postgres|kafka"
psql -h localhost -U postgres -c "CREATE DATABASE library_staging_test;" 2>/dev/null

# 运行 staging 测试
mvn test -Pstaging -pl library-staging-test

# staging 全量构建
mvn clean install -Pstaging

# 默认构建（不含 staging-test，与之前完全一致）
mvn clean install
```

---

## 四、已实施方案 C：GitHub Actions Staging Job

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
          POSTGRES_PASSWORD: postgres
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
          KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
        ports:
          - 29092:29092
        options: >-
          --health-cmd "kafka-topics --bootstrap-server localhost:29092 --list"
          --health-interval 10s
          --health-timeout 10s
          --health-retries 10

    steps:
      - name: Checkout repository
        uses: actions/checkout@v6

      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build all modules
        run: mvn clean install -DskipTests -B

      - name: Create staging database
        env:
          PGPASSWORD: postgres
        run: |
          psql -h localhost -U postgres -c "CREATE DATABASE library_staging_test;"

      - name: Run staging tests
        env:
          DB_USERNAME: postgres
          DB_PASSWORD: postgres
        run: mvn test -Pstaging -pl library-staging-test -B

      - name: Upload staging test reports
        uses: actions/upload-artifact@v7
        if: always()
        with:
          name: staging-test-reports
          path: '*/target/surefire-reports/*.txt'
          retention-days: 7
```

### 4.2 CI 流水线设计

```
build (H2 + EmbeddedKafka) → 快速反馈（现有测试）
  ↓
staging (PostgreSQL + Kafka) → 深度验证（新增 staging 测试）
```

两个 job 都通过才算成功。

---

## 五、验证检查清单

### 本地验证

- [x] `mvn clean install` — 默认构建不受影响
- [x] `mvn clean install -Pstaging -DskipTests` — staging 编译通过
- [x] `mvn test -pl library-integration-test` — 现有测试通过
- [x] `mvn test -Pstaging -pl library-staging-test` — staging 测试通过 ✅ (9/9)
- [x] 测试结束后 Kafka topics 已删除（@AfterAll 清理）
- [x] 测试结束后 consumer groups 已删除
- [x] 测试结束后 PostgreSQL 表已删除（create-drop）

### CI 验证

- [x] GitHub Actions staging job 通过 ✅ (build 3m59s + staging 1m44s)
- [x] staging test reports 上传成功

---

## 六、实施优先级

| 优先级 | 任务 | 状态 |
|:------:|------|:----:|
| P0 | 方案 A：独立 staging-test 模块 | ✅ 已完成 |
| P0 | 环境清理（@AfterAll Kafka 恢复） | ✅ 已完成 |
| P0 | 实时数据监控（StagingEnvironmentInspector） | ✅ 已完成 |
| P1 | 本地手动验证跑通 | ✅ 已完成 (9/9 tests, 20s) |
| P2 | 方案 C：GitHub Actions staging job | ✅ 已完成 |
| P3 | Redis 集成测试（当项目引入 Redis 后） | 📋 待实施 |
