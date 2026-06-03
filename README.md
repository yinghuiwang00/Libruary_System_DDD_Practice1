# 🏛️ Enterprise Library Management System

企业级图书馆管理系统 — 基于 Domain-Driven Design (DDD) 的微服务架构。

[![CI](https://github.com/yinghuiwang00/Libruary_System_DDD_Practice1/actions/workflows/ci.yml/badge.svg)](https://github.com/yinghuiwang00/Libruary_System_DDD_Practice1/actions/workflows/ci.yml)

---

## 技术栈

| 层面 | 技术 |
|------|------|
| **语言** | Java 17 |
| **框架** | Spring Boot 3.2.5, Spring Data JPA, Hibernate |
| **数据库** | PostgreSQL (生产), H2 PostgreSQL Mode (测试) |
| **消息队列** | Apache Kafka (spring-kafka), EmbeddedKafka (测试) |
| **API 文档** | SpringDoc OpenAPI 2.5.0 (Swagger UI) |
| **构建** | Maven 多模块 (1 parent + 11 child modules) |
| **测试** | JUnit 5, Mockito, AssertJ, Awaitility, Cucumber 7.15.0 |
| **CI/CD** | GitHub Actions (build + staging 双 job) |
| **监控** | Prometheus + Grafana + Jaeger |

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 启动步骤

```bash
# 1. 克隆项目
git clone https://github.com/yinghuiwang00/Libruary_System_DDD_Practice1.git
cd Libruary_System_DDD_Practice1

# 2. 启动基础设施 (PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger)
docker compose up -d

# 3. 编译所有模块
mvn clean install

# 4. 启动各模块 (每个模块一个终端)
cd library-catalog && mvn spring-boot:run      # 端口 8081
cd library-inventory && mvn spring-boot:run     # 端口 8082
cd library-circulation && mvn spring-boot:run   # 端口 8083
cd library-patron && mvn spring-boot:run        # 端口 8084
cd library-payment && mvn spring-boot:run       # 端口 8085
cd library-analytics && mvn spring-boot:run     # 端口 8086
cd library-notification && mvn spring-boot:run  # 端口 8087
```

---

## 系统架构

### 模块总览

| 模块 | 端口 | 职责 | API 数 | 事件 | Kafka 消费 |
|------|------|------|:------:|:----:|:----------:|
| library-shared | — | 共享 ID、事件基类、值对象 | 0 | 1 (base) | 0 |
| library-catalog | 8081 | 书籍/作者/出版商/分类管理 | ~15 | 4 | 0 |
| library-inventory | 8082 | 库存/副本/图书馆管理 | ~13 | 8 | 2 |
| library-circulation | 8083 | 借阅/归还/预约/续借/逾期 | ~12 | 14 | 1 |
| library-patron | 8084 | 会员注册/管理/罚款 | ~13 | 6 | 2 |
| library-payment | 8085 | 支付/退款 | ~12 | 6 | 1 |
| library-analytics | 8086 | 统计报表 | ~7 | 4 | 2 |
| library-notification | 8087 | 通知管理（多渠道） | ~10 | 4 | 4 |

### DDD 分层架构

每个 bounded context 遵循统一的四层架构：

```
domain/
  ├── model/          # 聚合根、实体、值对象、枚举
  ├── service/        # 领域服务
  ├── repository/     # 仓库接口 (JPA)
  ├── event/          # 领域事件
  └── exception/      # 领域异常 (带错误码)

application/
  ├── service/        # 应用服务 (编排)
  ├── handler/        # 跨上下文事件处理器
  ├── command/        # 命令对象
  ├── query/          # 查询/条件对象
  └── dto/            # DTO + ApiResponse<T>

infrastructure/
  ├── persistence/    # 自定义仓库实现 (Criteria API)
  ├── messaging/      # Kafka 发布器 + 消费者
  └── config/         # JPA、模块配置

interfaces/
  └── rest/           # REST 控制器 + 全局异常处理
```

### 跨模块通信

模块间通过 Kafka 事件通信，不直接调用 API：

```
Catalog ──BookPublishedEvent──> Inventory
Circulation ──BookBorrowedEvent──> Inventory, Patron, Notification
Circulation ──FineIncurredEvent──> Payment
Payment ──PaymentCompletedEvent──> Patron, Notification
Patron ──PatronSuspendedEvent──> Circulation, Notification
```

---

## 功能总览

### 📚 图书管理 (Catalog)
> 端口 8081 | [详细用法 → docs/usage/01-catalog.md](docs/usage/01-catalog.md)

- **书籍管理**: 创建、更新、发布/下架、删除、分页搜索
- **作者管理**: CRUD，关联到书籍（支持多种角色：作者、合著、编辑、译者）
- **出版商管理**: CRUD，按名称搜索
- **分类管理**: 树形结构（父子分类），根分类查询

### 📦 库存管理 (Inventory)
> 端口 8082 | [详细用法 → docs/usage/02-inventory.md](docs/usage/02-inventory.md)

- **图书馆管理**: 创建分馆、激活/停用
- **库存管理**: 初始化库存、查看各馆库存概况
- **副本管理**: 单个添加、批量添加
- **副本状态**: 借出、归还、损坏报告、遗失报告

### 🔄 流通管理 (Circulation)
> 端口 8083 | [详细用法 → docs/usage/03-circulation.md](docs/usage/03-circulation.md)

- **借阅**: 借书、还书、续借、召回
- **预约**: 创建预约、取消预约、排队机制
- **逾期处理**: 批量处理逾期、罚金计算

### 👤 读者管理 (Patron)
> 端口 8084 | [详细用法 → docs/usage/04-patron.md](docs/usage/04-patron.md)

- **会员管理**: 注册、信息修改、会籍续期
- **类型变更**: 学生 / 教师 / 职工 / 公众
- **状态管理**: 暂停、恢复、终止
- **罚款管理**: 添加罚款、缴纳、减免

### 💰 支付管理 (Payment)
> 端口 8085 | [详细用法 → docs/usage/05-payment.md](docs/usage/05-payment.md)

- **支付**: 创建 → 处理 → 完成 / 失败 / 取消
- **退款**: 请求 → 处理 → 完成
- **多方式**: 现金、信用卡、借记卡、在线转账

### 📊 数据分析 (Analytics)
> 端口 8086 | [详细用法 → docs/usage/06-analytics.md](docs/usage/06-analytics.md)

- **报表**: 创建、查询（按类型/状态过滤）、完成、失败、取消
- **重新生成**: 已完成或失败的报表可重新生成

### 🔔 通知管理 (Notification)
> 端口 8087 | [详细用法 → docs/usage/07-notification.md](docs/usage/07-notification.md)

- **通知**: 创建、发送、调度、送达、已读
- **失败处理**: 失败标记、重试
- **多渠道**: Email、SMS、Push、站内
- **自动触发**: 基于 Kafka 事件自动生成（到期提醒、逾期通知等）

### 🔗 端到端场景
> [详细用法 → docs/usage/08-e2e-scenarios.md](docs/usage/08-e2e-scenarios.md)

| # | 场景 | 涉及模块 |
|---|------|---------|
| 1 | 新书入库到可借阅 | Catalog → Inventory |
| 2 | 读者注册到借书 | Patron → Circulation → Inventory |
| 3 | 借书→逾期→罚款→缴费 | Circulation → Patron → Payment |
| 4 | 预约→到书通知→取书 | Circulation → Inventory → Notification |
| 5 | 损坏报告→罚款→退款 | Inventory → Patron → Payment |
| 6 | 完整读者生命周期 | Patron → Circulation → Payment |
| 7 | 库存不足预警 | Inventory → Notification |
| 8 | 数据分析报表生成 | Analytics |

---

## 基础设施

| 服务 | 端口 | 用途 |
|------|------|------|
| PostgreSQL | 5432 | 主数据库（每个模块独立库） |
| Redis | 6379 | 缓存 |
| Kafka | 29092 | 消息队列（跨模块通信） |
| Kafka UI | http://localhost:9000 | Kafka 管理界面 |
| Prometheus | http://localhost:9090 | 监控指标采集 |
| Grafana | http://localhost:3000 | 监控面板（admin） |
| Jaeger | http://localhost:16686 | 分布式链路追踪 |

---

## API 文档 (Swagger UI)

每个模块都有独立的 Swagger UI，启动后访问：

| 模块 | 地址 |
|------|------|
| Catalog | http://localhost:8081/swagger-ui.html |
| Inventory | http://localhost:8082/swagger-ui.html |
| Circulation | http://localhost:8083/swagger-ui.html |
| Patron | http://localhost:8084/swagger-ui.html |
| Payment | http://localhost:8085/swagger-ui.html |
| Analytics | http://localhost:8086/swagger-ui.html |
| Notification | http://localhost:8087/swagger-ui.html |

---

## 构建 & 测试

```bash
# 构建所有模块
mvn clean install

# 构建不跑测试
mvn clean install -DskipTests

# 构建特定模块
cd library-catalog && mvn clean install

# 运行所有测试
mvn test

# 运行特定模块测试
cd library-circulation && mvn test

# 运行特定测试类
mvn test -Dtest=LoanTest

# 运行特定测试方法
mvn test -Dtest=LoanTest#testCheckout

# E2E 测试 (H2 + EmbeddedKafka)
cd library-e2e-test && mvn test
cd library-integration-test && mvn test

# Staging 测试 (PostgreSQL + Kafka，需要 Docker)
mvn test -Pstaging -pl library-staging-test
```

---

## 项目结构

```
Libruary_System_DDD_Practice1/
├── library-shared/              # 共享模块 (ID 类型, 值对象, 事件基类)
├── library-catalog/             # 图书管理 (8081)
├── library-inventory/           # 库存管理 (8082)
├── library-circulation/         # 流通管理 (8083)
├── library-patron/              # 读者管理 (8084)
├── library-payment/             # 支付管理 (8085)
├── library-analytics/           # 数据分析 (8086)
├── library-notification/        # 通知管理 (8087)
├── library-e2e-test/            # JUnit 5 E2E 测试
├── library-integration-test/    # Cucumber BDD E2E 测试
├── library-staging-test/        # Staging 测试 (真实基础设施)
├── Architecture_Design/         # 架构设计文档 (02-18)
├── DDD_Explanation/             # DDD 实践指南 (01-13)
├── IMPLEMENTATION-PLAN/         # 实施计划
├── docs/                        # 文档
│   ├── usage/                   # 功能使用指南 + 端到端场景
│   ├── reference/               # 开发参考文档
│   └── guides/                  # 开发指南
├── CLAUDE.md                    # 项目开发指引
├── DEVELOPMENT_PLAN.md          # 开发计划
└── pom.xml                      # Maven 父 POM
```

---

## CI/CD

### Pipeline 结构

```
push / PR → main / develop
  │
  ├── build (H2 + EmbeddedKafka)         ~4 min
  │   ├── Build all modules (skip tests)
  │   ├── Test library-shared
  │   ├── Test library-catalog
  │   ├── Test library-inventory
  │   ├── Test library-circulation
  │   ├── Test library-patron
  │   ├── Test library-payment
  │   ├── Test library-analytics
  │   ├── Test library-notification
  │   ├── Test library-e2e-test
  │   ├── Test library-integration-test
  │   └── Upload test-reports artifact
  │
  └── staging (PostgreSQL + Kafka)       ~2 min
      ├── needs: build ✅
      ├── Build all modules (skip tests)
      ├── Create database (PostgreSQL service container)
      ├── Test library-staging-test (9 scenarios)
      └── Upload staging-test-reports artifact
```

### 跳过 CI

| Commit Message | 效果 |
|----------------|------|
| `feat: xxx [skip ci]` | 跳过整个 CI |
| `feat: xxx [skip build]` | 跳过 build job（staging 也会因依赖被跳过） |

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [功能使用指南](docs/usage/) | 7 个模块的 API 详细用法 + 8 个端到端场景 |
| [架构设计](Architecture_Design/) | 02-08 各上下文设计, 09 总体架构, 10 Spring 指南, 15 测试策略, 17 Staging 策略 |
| [DDD 实践](DDD_Explanation/) | 01 分层, 02 聚合, 03 事件, 04 测试, 05-11 各上下文实现 |
| [开发参考](docs/reference/) | 向后兼容规则, 检查清单, 测试指南 |
| [开发指南](docs/guides/) | 新建 bounded context 步骤 |
| [实施计划](IMPLEMENTATION-PLAN/) | 各阶段实施计划 |
| [开发计划](DEVELOPMENT_PLAN.md) | 总体开发计划 |
| [CLAUDE.md](CLAUDE.md) | AI 开发助手指引 |

---

## License

This project is for educational purposes.
