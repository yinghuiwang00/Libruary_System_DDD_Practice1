# 18 - Staging Test 模块实施记录

> 日期：2026-06-01
> 状态：✅ 已完成

---

## 一、背景

当前 `library-e2e-test` 和 `library-integration-test` 都使用 H2 + EmbeddedKafka 运行测试，无法验证真实 PostgreSQL 的 JSON 序列化差异、Kafka 在真实 broker 上的消息路由、以及连接池/事务行为。

新建独立模块 `library-staging-test`，使用 Docker 运行的真实基础设施进行 E2E 测试，同时**不影响任何现有模块**。

---

## 二、架构设计

### 2.1 Maven Profile 隔离

父 `pom.xml` 将 `<modules>` 重构为两个 profiles：

| Profile | 激活方式 | 包含模块 |
|---------|---------|---------|
| `default` | 默认激活 | 现有 10 个模块（无变化） |
| `staging` | `-Pstaging` | 现有 10 个 + `library-staging-test` |

- `mvn clean install` → 与之前完全一致
- `mvn clean install -Pstaging` → 额外包含 staging-test

### 2.2 依赖差异（vs library-integration-test）

| 依赖项 | integration-test | staging-test | 原因 |
|--------|-----------------|-------------|------|
| `spring-kafka-test` | ✅ (EmbeddedKafka) | ❌ | 直连真实 Kafka |
| `h2` | ✅ (内存数据库) | ❌ | 使用真实 PostgreSQL |
| `postgresql` | ❌ | ✅ (runtime) | 直连真实 PostgreSQL |
| `spring-kafka` | ✅ | ✅ | 共用 |
| Cucumber BDD | ✅ | ✅ | 共用 |
| `awaitility` | ✅ | ✅ | 共用 |

### 2.3 配置差异（application.yml）

| 项目 | integration-test | staging-test |
|------|-----------------|-------------|
| 数据库 | `jdbc:h2:mem:integration_test` | `jdbc:postgresql://localhost:5432/library_staging_test` |
| Hibernate dialect | `H2Dialect` | `PostgreSQLDialect` |
| Hibernate ddl-auto | `create-drop` | `create-drop` |
| Kafka bootstrap | `${spring.embedded.kafka.brokers}` | `localhost:29092` |
| Consumer group-id | `library.test.consumer` | `library.staging-test.consumer` |
| Producer value-serializer | `StringSerializer` | `StringSerializer` |

---

## 三、环境管理机制

### 3.1 生命周期

```
@Before (首轮 Scenario)         @Before (每个 Scenario)       @After (每个 Scenario)       @AfterAll (全部结束)
┌────────────────────────┐     ┌────────────────────────┐    ┌────────────────────────┐    ┌────────────────────────┐
│ 1. 删除旧 Kafka topics  │     │ 1. DELETE 所有表数据   │    │ 打印 AFTER 快照        │    │ 1. 停止 12 个 Kafka    │
│ 2. 重建 topics (3分区)  │     │ 2. 打印 BEFORE 快照   │    │ (PASSED/FAILED 状态)   │    │    consumer containers │
│ 3. 删除 consumer groups │     │    - PostgreSQL 行数   │    │    - PostgreSQL 行数   │    │ 2. 删除 5 个 topics    │
│ 4. 等待 partition 分配  │     │    - Kafka 消息数      │    │    - Kafka 消息数      │    │ 3. 删除 12 个 consumer │
│ 5. DELETE 所有表数据    │     │    - Redis 状态        │    │    - Redis 状态        │    │    groups              │
│ 6. 打印 BEFORE 快照    │     └────────────────────────┘    └────────────────────────┘    └────────────────────────┘
└────────────────────────┘
                                                    ↓ Spring Context 关闭 ↓
                                              Hibernate DROP 所有表 (create-drop)
```

### 3.2 环境恢复保证

| 基础设施 | 恢复机制 | 时机 |
|---------|---------|------|
| **PostgreSQL 表结构** | `ddl-auto: create-drop`：启动建表，关闭删表 | Spring 上下文生命周期 |
| **PostgreSQL 数据** | `@Before` 清空所有表数据 | 每个 Scenario 之前 |
| **Kafka 消息** | `@AfterAll` 删除 topics，下次 `@Before` 重建 | 全部测试结束后 |
| **Kafka Consumer Groups** | `@AfterAll` 停止 consumers 后删除 groups | 全部测试结束后 |
| **Redis** | 暂未使用（预留扩展） | — |

### 3.3 实时数据监控（StagingEnvironmentInspector）

测试运行时自动在每个 Scenario 前后打印环境快照到控制台：

```
═══════════════════════════════════════════════════════
  STAGING ENV SNAPSHOT  ──  BEFORE: Borrow book updates patron loan count
═══════════════════════════════════════════════════════
  ┌─ PostgreSQL ─────────────────────────────────────
  │ Table               │ Rows
  │─────────────────────┼──────────
  │ analytics_reports   │ 0
  │ book_authors        │ 0
  │ book_copies         │ 0
  │ ...
  └─────────────────────────────────────────────────
  ┌─ Kafka ──────────────────────────────────────────
  │ Topic                          │ Messages
  │────────────────────────────────┼──────────
  │ library.catalog.events         │ 0
  │ library.circulation.events     │ 0
  │ ...
  └─────────────────────────────────────────────────
  ┌─ Redis ──────────────────────────────────────────
  │ Status: Not used in current tests
  └─────────────────────────────────────────────────
═══════════════════════════════════════════════════════

  ... (scenario 执行) ...

═══════════════════════════════════════════════════════
  STAGING ENV SNAPSHOT  ──  AFTER:  Borrow book ... [PASSED]
═══════════════════════════════════════════════════════
  │ patrons              │ 1
  │ notifications        │ 1
  │ library.circulation.events │ 1
  │ ...
═══════════════════════════════════════════════════════
```

监控内容包括：
- **PostgreSQL**：11 张表的行数（`SELECT COUNT(*)`）
- **Kafka**：5 个 topic 的 end offset（消息数）+ staging consumer group 状态
- **Redis**：当前为 placeholder（项目未使用，预留扩展）

---

## 四、文件清单

### 4.1 新建文件（23 个）

```
library-staging-test/
├── pom.xml
└── src/test/
    ├── java/com/library/staging/
    │   ├── StagingTestApplication.java
    │   ├── config/
    │   │   └── IntegrationTestBeansConfig.java
    │   └── bdd/
    │       ├── StagingCucumberConfig.java          # 核心配置 + 环境清理
    │       ├── StagingEnvironmentInspector.java    # 实时数据监控
    │       ├── StagingScenarioState.java
    │       ├── CucumberTestSuite.java
    │       ├── SharedSteps.java
    │       ├── BorrowBookSteps.java
    │       ├── FinePaymentSteps.java
    │       ├── HoldBookSteps.java
    │       ├── LowStockAlertSteps.java
    │       ├── NewBookSteps.java
    │       ├── PatronSuspensionSteps.java
    │       └── ReturnBookSteps.java
    └── resources/
        ├── application.yml
        └── features/integration/
            ├── borrow-book.feature
            ├── fine-payment.feature
            ├── hold-book.feature
            ├── low-stock-alert.feature
            ├── new-book.feature
            ├── patron-suspension.feature
            └── return-book.feature
```

### 4.2 修改的现有文件（1 个）

| 文件 | 修改内容 |
|------|---------|
| `pom.xml`（父 pom） | `<modules>` 重构为 `default` + `staging` 两个 Maven profiles |

### 4.3 不修改的现有模块

| 模块 | 说明 |
|------|------|
| library-shared ~ library-notification (×8) | 不修改 |
| library-e2e-test | 不修改 |
| library-integration-test | 不修改 |

---

## 五、关键适配点（vs library-integration-test）

| # | 差异 | 说明 |
|---|------|------|
| 1 | 去掉 `@EmbeddedKafka` | 直连真实 Kafka `localhost:29092` |
| 2 | 去掉 `EmbeddedKafkaBroker` | 不需要内嵌 broker |
| 3 | 去掉 `waitForAssignment` | 真实 Kafka 不需要手动等待 partition 分配 |
| 4 | `KafkaTemplate` 自动配置 | `@Autowired` 注入 Spring Boot 自动配置的 KafkaTemplate |
| 5 | 数据库改用 PostgreSQL | `jdbc:postgresql://localhost:5432/library_staging_test` |
| 6 | 包名隔离 | `com.library.staging` 替代 `com.library.integration` |
| 7 | `@AfterAll` 环境清理 | 停止 consumers → 删除 Kafka topics + 删除 consumer groups |
| 8 | `@Before` Kafka 初始化 | 首轮场景：删除旧 topics → 重建 → 删除 groups → 等待 partition 分配 |
| 9 | `StagingEnvironmentInspector` | 每个 Scenario 前后打印 PostgreSQL + Kafka + Redis 状态 |

---

## 六、前置条件

```bash
# 1. 确保 Docker 基础设施运行
docker ps | grep -E "postgres|kafka"

# 2. 创建测试数据库
psql -h localhost -U postgres -c "CREATE DATABASE library_staging_test;"

# 3. Kafka topics 自动创建（@KafkaListener 订阅时自动创建，无需手动操作）
```

---

## 七、运行与验证

```bash
# 默认构建（不含 staging-test）
mvn clean install

# staging 构建（包含 staging-test，需 Docker infra）
mvn clean install -Pstaging

# 仅运行 staging 测试
mvn test -Pstaging -pl library-staging-test

# 验证现有测试不受影响
mvn test -pl library-integration-test    # 9 tests passed ✅
mvn test -pl library-e2e-test
```

### 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn clean install -DskipTests` | ✅ BUILD SUCCESS |
| `mvn clean install -Pstaging -DskipTests` | ✅ BUILD SUCCESS |
| `mvn test -pl library-integration-test` | ✅ Tests run: 9, Failures: 0 |
| `mvn test -Pstaging -pl library-staging-test` | ✅ Tests run: 9, Failures: 0 (20s) |
