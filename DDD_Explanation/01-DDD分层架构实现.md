# DDD 分层架构实现说明

> 本文档基于实际代码，说明项目如何实现 DDD 的四层架构

## 分层概览

```
┌─────────────────────────────────────┐
│  interfaces/rest/                    │  ← 接口层：REST Controller + 异常处理
├─────────────────────────────────────┤
│  application/                        │  ← 应用层：服务编排、事件处理器、DTO
│    ├── service/                      │
│    ├── handler/                      │
│    ├── command/query/dto/            │
├─────────────────────────────────────┤
│  domain/                             │  ← 领域层：核心业务逻辑
│    ├── model/    (聚合根、实体、枚举)  │
│    ├── service/  (领域服务)           │
│    ├── repository/ (仓储接口)         │
│    ├── event/    (领域事件)           │
│    └── exception/(领域异常)           │
├─────────────────────────────────────┤
│  infrastructure/                     │  ← 基础设施层：技术实现
│    ├── persistence/ (自定义仓储实现)   │
│    ├── messaging/  (Kafka 消费/发布)  │
│    └── config/     (JPA/模块配置)     │
└─────────────────────────────────────┘
```

## 各层职责与实际示例

### 领域层 (domain/)

**核心原则**: 不依赖任何框架，纯业务逻辑

- **聚合根**: `@Entity` + `@EmbeddedId`（自定义 ID 类型）+ `@Version`（乐观锁）
  - 示例：`Book.java` — 包含 `create()`, `publish()`, `delete()`, `addAuthor()` 等业务方法
  - 示例：`Patron.java` — 包含 `recordLoan()`, `addFine()`, `suspend()`, `reactivate()` 等业务方法
  - 示例：`Loan.java` — 包含 `create()`, `returnBook()`, `renew()`, `recall()`, `markOverdue()` 等业务方法
  - 示例：`Payment.java` — 包含 `create()`, `process()`, `complete()`, `requestRefund()` 等业务方法
- **值对象**: `@Embeddable` 或自定义类（如 `ISBN`, `Money`, `Email`, `PhoneNumber`, `Address`, `Location`, `BorrowingPrivilege`, `TrendAnalysis`, `CirculationPolicy`, `Fine`）
- **仓储接口**: 继承 `JpaRepository` + 自定义接口，实现由 infrastructure 层提供
- **领域事件**: 继承 `DomainEvent` 基类，由聚合根业务方法触发
- **领域异常**: 继承 `DomainException`，携带错误码

### 应用层 (application/)

**核心原则**: 编排领域对象，不包含业务逻辑

- **应用服务**: 调用领域服务 + 发布事件 + 管理事务
  - 示例：`BookApplicationService` — 编排 BookManagementService + CatalogDomainEventPublisher
  - 示例：`CirculationApplicationService` — 编排流通用例
- **事件处理器**: 处理来自其他上下文的 Kafka 消息
  - 示例：`BookBorrowedEventHandler` — 处理 BookBorrowedEvent，更新 Patron 贷款数
  - 示例：`PaymentCompletedEventHandler` — 处理 PaymentCompletedEvent，减少 Patron 罚款
- **DTO/Command/Query**: 数据传输对象，与领域模型分离

### 基础设施层 (infrastructure/)

**核心原则**: 为领域层提供技术实现

- **Kafka 消费者**: `@Component` + `@KafkaListener`，解析 JSON → 调用 handler
- **Kafka 发布者**: `*DomainEventPublisher`，实现双发模式（Spring EventBus + Kafka）
- **持久化**: JPA 仓储接口实现（如 Criteria API 查询）
- **配置**: JPA 配置、模块特定 Bean

### 接口层 (interfaces/)

**核心原则**: HTTP 入口，薄层转换

- **REST Controller**: `@RestController` + `@RequestMapping`
- **全局异常处理**: `@RestControllerAdvice` + `GlobalExceptionHandler`

## 依赖方向

```
interfaces → application → domain ← infrastructure
```

- domain 不依赖任何外层
- application 依赖 domain
- infrastructure 依赖 domain（实现 domain 定义的接口）
- interfaces 依赖 application

## 各模块文件分布

| 模块 | domain/ | application/ | infrastructure/ | interfaces/ | 合计 |
|------|---------|-------------|-----------------|-------------|------|
| library-catalog | 31 | 9 | 2 | 5 | 49 |
| library-inventory | 25 | 13 | 3 | 3 | 46 |
| library-circulation | 29 | 12 | 2 | 4 | 48 |
| library-patron | 17 | 16 | 3 | 2 | 41 |
| library-payment | 18 | 12 | 2 | 2 | 37 |
| library-analytics | 16 | 10 | 3 | 2 | 33 |
| library-notification | 14 | 19 | 4 | 2 | 41 |
| library-shared | 23 | - | - | - | 23 |

**各层文件数统计说明：**

| 模块 | domain/model | domain/event | domain/exception | domain/repository | domain/service | application/service | application/handler | application/command | application/dto/query | infrastructure/messaging | infrastructure/persistence | interfaces/rest |
|------|-------------|-------------|-----------------|------------------|---------------|--------------------|--------------------|--------------------|-----------------------|------------------------|------------------------|----------------|
| catalog | 7 | 4 | 9 | 5 | 5 | 1 | - | 2 | 5 | 1 | 1 | 5 |
| inventory | 5+3enums | 8 | 7 | 3 | 1 | 2 | 3 | 4 | 4 | 3 | - | 3 |
| circulation | 4+2enums | 14 | 6 | 2 | 1 | 1 | 1 | 6 | 3 | 2 | - | 4 |
| patron | 2+2enums | 6 | 5 | 1 | 1 | 1 | 4 | 10 | 2 | 3 | - | 2 |
| payment | 2+4enums | 6 | 3 | 2 | 1 | 1 | 1 | 8 | 3 | 2 | - | 2 |
| analytics | 2+4enums | 4 | 4 | 1 | 1 | 1 | 2 | 5 | 2 | 3 | - | 2 |
| notification | 1+4enums | 4 | 3 | 1 | 1 | 1 | 8 | 8 | 2 | 4 | - | 2 |

> 数据截至 2026-05-31，基于实际源码文件统计
