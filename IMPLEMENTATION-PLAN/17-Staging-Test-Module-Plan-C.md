# 18 - 方案C：GitHub Actions Staging Job 实施计划

> 日期：2026-06-02
> 状态：📋 待实施

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
  needs: build                          # 依赖 build job，先跑完 H2 测试
  runs-on: ubuntu-latest

  services:                             # GitHub Actions Service Containers
    postgres:
      image: postgres:16
      env:
        POSTGRES_USER: postgres
        POSTGRES_PASSWORD: postgres     # CI 环境，非生产密码
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
      uses: actions/upload-artifact@v4
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
| Kafka health check | `kafka-topics --list` | 确认 broker 完全就绪 |
| `needs: build` | build 通过后才跑 staging | 节省资源，快速反馈优先 |
| 密码 `postgres` + 环境变量覆盖 | CI 临时环境 | `application.yml` 默认 `dev_pg_2026`，CI step 通过 `DB_PASSWORD` 环境变量覆盖为 `postgres` |
| `if: always()` 上传 reports | 失败也能看到报告 | 便于排查 |

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

## 六、风险评估

| 风险 | 概率 | 缓解措施 |
|------|------|---------|
| Kafka service container 启动慢 | 中 | health check + retries (5次) |
| Kafka `localhost:29092` 在 container 内不通 | 低 | `KAFKA_ADVERTISED_LISTENERS` 已配置为 `PLAINTEXT://localhost:29092` |
| staging 测试超时 | 低 | Service Container 资源充足，本地 20s 即可完成 |
| Zookeeper 与 Kafka 启动顺序 | 中 | Kafka 依赖 Zookeeper，GitHub Actions 会并行启动所有 services 但 health check 确保就绪 |
