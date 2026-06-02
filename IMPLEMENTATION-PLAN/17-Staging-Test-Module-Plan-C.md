# 18 - 方案C：GitHub Actions Staging Job 实施计划

> 日期：2026-06-02
> 状态：✅ 已完成

---

## 一、背景

当前 CI 流水线（`.github/workflows/ci.yml`）只有一个 `build` job，所有测试使用 H2 + EmbeddedKafka 运行。无法在 CI 中验证真实 PostgreSQL / Kafka 的行为差异。

**方案A**（`library-staging-test` 模块）已在本地实现并通过（9/9 tests）。但该模块需要 Docker 运行的 PostgreSQL + Kafka，目前只能在本地手动运行。

**方案C** 的目标：在 GitHub Actions CI 中增加一个 `staging` job，使用 GitHub Actions Service Containers 提供 PostgreSQL + Kafka，自动运行 `library-staging-test`，实现 CI 全自动的 staging 环境验证。

---

## 二、修改范围

### 2.1 修改文件：`.github/workflows/ci.yml`

**变更**：在现有 `build` job 之后新增 `staging` job。

#### 新增 staging job 结构

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

#### 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| Service Container 用 PostgreSQL 16 | 与本地 Docker 一致 | 避免 PG 版本差异 |
| Service Container 用 cp-kafka 7.5.0 | 与本地 Docker 一致 | 保持一致性 |
| Kafka 内部监听 `0.0.0.0:29092` | 与 advertised listeners 端口一致 | 避免 Kafka controller 无法连接自身 |
| Kafka health check 用 `localhost:29092` | 与容器内监听端口匹配 | health check 在容器内执行 |
| `needs: build` | build 通过后才跑 staging | 节省资源，快速反馈优先 |
| 密码 `postgres` + 环境变量覆盖 | CI 临时环境 | `application.yml` 默认 `dev_pg_2026`，CI step 通过 `DB_PASSWORD` 环境变量覆盖为 `postgres` |
| `if: always()` 上传 reports | 失败也能看到报告 | 便于排查 |
| Actions v6/v5/v7 | 升级到 Node.js 24 兼容版本 | 避免弃用警告 |

### 2.2 无需修改：`library-staging-test/src/test/resources/application.yml`

**已确认**：application.yml 使用环境变量 `${DB_USERNAME:postgres}` / `${DB_PASSWORD:dev_pg_2026}`。
CI step 中通过 `env` 块传入 `DB_PASSWORD: postgres` 即可覆盖默认值，无需修改 application.yml。

---

## 三、文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `.github/workflows/ci.yml` | 修改 | 新增 `staging` job（约 50 行 YAML） |
| `Architecture_Design/17-Staging-Test-Strategy.md` | 修改 | 更新方案C状态为 ✅ 已实施 |

### 不修改的现有模块

| 模块 | 说明 |
|------|------|
| library-shared ~ library-notification (×8) | 不修改 |
| library-e2e-test | 不修改 |
| library-integration-test | 不修改 |
| library-staging-test | 不修改（仅通过环境变量覆盖密码） |

---

## 四、CI 流水线最终结构

```
push / PR
  │
  ├── build (现有)                          → H2 + EmbeddedKafka 快速反馈
  │   ├── build all modules
  │   ├── test library-shared
  │   ├── test library-catalog
  │   ├── ...
  │   ├── test library-e2e-test
  │   └── test library-integration-test
  │
  └── staging (新增)                        → PostgreSQL + Kafka 深度验证
      ├── needs: build ✅
      ├── services: postgres + zookeeper + kafka
      ├── build all modules
      ├── create database
      ├── test library-staging-test (9 scenarios)
      └── upload reports
```

两个 job 都通过才算 CI 成功。

---

## 五、验证步骤

### 5.1 本地验证

1. **YAML 语法验证**：`yamllint .github/workflows/ci.yml`
2. **确认默认构建不受影响**：`mvn clean install -DskipTests`

### 5.2 CI 验证

1. **推送分支触发 CI**：
   ```bash
   git checkout -b feat/ci-staging-job
   git add .github/workflows/ci.yml
   git commit -m "ci: add staging job with PostgreSQL + Kafka service containers"
   git push -u origin feat/ci-staging-job
   ```
2. **检查 GitHub Actions**：
   - `build` job 应通过（与之前一致）
   - `staging` job 应通过（9/9 staging tests）
   - `staging-test-reports` artifact 应上传成功
3. **确认不影响现有流程**：`build` job 无变化

---

## 六、实际遇到的问题与解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Kafka container 初始化失败（第一次） | `KAFKA_LISTENERS` 未设置，默认监听 `0.0.0.0:9092`，但 health check 用 `localhost:9092`，而 `ADVERTISED_LISTENERS` 指向 `localhost:29092` 导致内部 controller 连接失败 | 添加 `KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092`，health check 改用 `localhost:29092` |
| Kafka container 初始化失败（第二次） | 容器内监听 `9092`，但 `ADVERTISED_LISTENERS` 指向 `29092`，Kafka controller 尝试连接 `localhost:29092` 失败 | 让 Kafka 容器内也监听 `29092`（`ports: 29092:29092`），内外端口一致 |
| YAML 缩进错误导致 workflow 解析失败 | `library-integration-test` step 的 `run:` 多了一个空格 | 修正缩进 |
| Node.js 20 弃用警告 | Actions v4 使用 Node.js 20 | 升级到 `checkout@v6`、`setup-java@v5`、`upload-artifact@v7` |

---

## 七、验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn clean install` | ✅ BUILD SUCCESS |
| `mvn clean install -Pstaging -DskipTests` | ✅ BUILD SUCCESS |
| `mvn test -pl library-integration-test` | ✅ Tests run: 9, Failures: 0 |
| `mvn test -Pstaging -pl library-staging-test` | ✅ Tests run: 9, Failures: 0 (20s) |
| GitHub Actions build job | ✅ 3m59s |
| GitHub Actions staging job | ✅ 1m44s (9/9 tests) |
| staging-test-reports artifact | ✅ 已上传 |
