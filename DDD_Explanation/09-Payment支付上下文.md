# Library Payment 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-payment` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

---

## 目录

- [1. 模块概览](#1-模块概览)
- [2. DDD 分层架构总览](#2-ddd-分层架构总览)
- [3. 引导与配置层](#3-引导与配置层)
- [4. 领域层（Domain Layer）](#4-领域层domain-layer)
  - [4.1 领域模型（domain/model）](#41-领域模型domainmodel)
  - [4.2 领域事件（domain/event）](#42-领域事件domainevent)
  - [4.3 领域异常（domain/exception）](#43-领域异常domainexception)
  - [4.4 仓储接口（domain/repository）](#44-仓储接口domainrepository)
  - [4.5 领域服务（domain/service）](#45-领域服务domainservice)
- [5. 应用层（Application Layer）](#5-应用层application-layer)
  - [5.1 应用服务（application/service）](#51-应用服务applicationservice)
  - [5.2 命令对象（application/command）](#52-命令对象applicationcommand)
  - [5.3 查询对象（application/query）](#53-查询对象applicationquery)
  - [5.4 数据传输对象（application/dto）](#54-数据传输对象applicationdto)
- [6. 基础设施层（Infrastructure Layer）](#6-基础设施层infrastructure-layer)
- [7. 接口层（Interfaces Layer）](#7-接口层interfaces-layer)
- [8. 层间调用流程](#8-层间调用流程)
- [9. 文件清单速查表](#9-文件清单速查表)

---

## 1. 模块概览

`library-payment` 是图书馆管理系统的**支付上下文（Payment Context）**，负责管理罚金缴纳、会费支付、押金等支付流程及其退款处理：

| 职责 | 说明 |
|------|------|
| 支付管理 | 创建、处理、完成、失败、取消支付 |
| 退款管理 | 退款申请、处理、完成退款 |
| 支付类型 | 罚金支付（FINE_PAYMENT）、会费（MEMBERSHIP_FEE）、押金（DEPOSIT）、其他（OTHER） |
| 支付方式 | 现金、信用卡、借记卡、支付宝、微信支付、银行转账 |
| 参考号生成 | 自动生成唯一支付参考号（PAY + 时间戳 + 随机数） |
| 退款校验 | 验证退款金额不超过可退金额，支持部分退款与全额退款 |
| 领域事件 | 支付/退款状态变更时发布事件通知其他上下文 |

运行端口：`8085`，基础路径：`/api/payments/*`

---

## 2. DDD 分层架构总览

```
library-payment/src/main/java/com/library/payment/
│
├── PaymentApplication.java               <- Spring Boot 启动类
├── config/                               <- 基础设施配置
│   └── JpaConfig.java
│
├── domain/                               <- 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                            <- 聚合根、实体、值对象
│   │   ├── Payment.java                  <- 聚合根：支付
│   │   ├── Refund.java                   <- 实体：退款
│   │   └── enums/                        <- 枚举
│   │       ├── PaymentMethod.java
│   │       ├── PaymentStatus.java
│   │       ├── PaymentType.java
│   │       └── RefundStatus.java
│   ├── event/                            <- 领域事件
│   │   ├── PaymentCreatedEvent.java
│   │   ├── PaymentCompletedEvent.java
│   │   ├── PaymentFailedEvent.java
│   │   ├── PaymentCancelledEvent.java
│   │   ├── RefundRequestedEvent.java
│   │   └── RefundCompletedEvent.java
│   ├── exception/                        <- 领域异常（含错误码）
│   │   ├── DomainException.java          <- 异常基类
│   │   ├── InvalidOperationException.java
│   │   └── PaymentNotFoundException.java
│   ├── repository/                       <- 仓储接口
│   │   ├── PaymentRepository.java
│   │   └── RefundRepository.java
│   └── service/                          <- 领域服务
│       └── PaymentService.java
│
├── application/                          <- 应用层（编排、协调）
│   ├── service/
│   │   └── PaymentApplicationService.java <- 应用服务
│   ├── command/                          <- 写操作入参（8个Command类）
│   │   ├── CreatePaymentCommand.java
│   │   ├── ProcessPaymentCommand.java
│   │   ├── CompletePaymentCommand.java
│   │   ├── FailPaymentCommand.java
│   │   ├── CancelPaymentCommand.java
│   │   ├── RequestRefundCommand.java
│   │   ├── ProcessRefundCommand.java
│   │   └── CompleteRefundCommand.java
│   └── dto/                              <- 输出传输对象
│       ├── ApiResponse.java
│       ├── PaymentDTO.java
│       └── RefundDTO.java
│
└── interfaces/                           <- 接口层（REST API）
    └── rest/
        ├── PaymentController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces -> application -> domain <- infrastructure
                                   （实现 domain 的 repository 接口）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务和仓储接口）
- **interfaces 层**依赖 application 层（调用应用服务）
- **infrastructure 层**暂未创建独立实现，仓储接口由 Spring Data JPA 自动实现

---

## 3. 引导与配置层

### `PaymentApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.payment", "com.library.shared"})
@EnableJpaRepositories
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类。`scanBasePackages` 同时扫描 `com.library.payment` 和 `com.library.shared`，确保共享模块中的 `DomainEventPublisher`、值对象 ID（`PaymentId`、`PatronId`、`RefundId`）能被正确注入。`@EnableJpaRepositories` 启用仓储自动发现。

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：启用 JPA 审计功能，让实体上的 `@CreatedDate`、`@LastModifiedDate` 自动填充。放在 config 包而非 infrastructure，因为这是全局性配置。

---

## 4. 领域层（Domain Layer）

### 4.1 领域模型（domain/model）

#### 4.1.1 `Payment.java` —— 聚合根：支付

**DDD 角色**：**聚合根（Aggregate Root）**

Payment 是支付上下文中最重要的聚合根，管理支付的完整生命周期以及与退款的关联。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId PaymentId id` | 共享模块强类型 ID |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突 |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate` | 自动记录创建/修改时间 |
| 构造控制 | `protected Payment()` + `private Payment(...)` | JPA 无参构造器 + 防止外部直接实例化 |
| 工厂方法 | `Payment.create(...)` | 确保创建时总是生成新 ID、初始状态 PENDING、生成参考号 |
| 金额精度 | `precision=10, scale=2` + `setScale(2, HALF_UP)` | 精确到分，避免浮点误差 |
| 参考号唯一 | `PAY` + 时间戳 + 4位随机数，`unique=true` | 业务可追溯 |
| 退款关联 | `@Transient List<Refund> refunds` | 不在此表持久化，通过 Refund 仓储独立管理 |
| 索引策略 | 4 个 `@Index`（patron_id, status, payment_date, reference_number） | 优化查询性能 |

**状态机：支付生命周期**

```
PENDING --process()--> PROCESSING --complete()--> COMPLETED --requestRefund()--> REFUNDED
   |                       |
   |--cancel()--> CANCELLED |--fail()--> FAILED
```

**业务规则（编码在领域模型中）：**

- `process()`：只有 PENDING 状态才能开始处理；记录外部交易 ID
- `complete()`：只有 PROCESSING 状态才能完成；设置 paymentDate 和 processedDate
- `fail(reason)`：只有 PROCESSING 状态才能失败；记录失败原因
- `cancel(reason)`：只有 PENDING 状态才能取消；记录取消原因
- `requestRefund()`：只有 COMPLETED 状态才能申请退款；退款金额不能超过可退金额；全额退款自动变为 REFUNDED 状态
- `validateAmount()`：金额必须非空且为正数，统一精度到两位小数

---

#### 4.1.2 `Refund.java` —— 实体：退款

**DDD 角色**：**聚合内实体（Entity within Aggregate）**

Refund 通过 `String paymentId` 关联到 Payment 聚合根（DDD 中跨聚合通过 ID 而非对象引用关联），有独立的数据库表但生命周期受 Payment 控制。

| 设计点 | 说明 |
|--------|------|
| 关联方式 | `String paymentId` 而非 `@ManyToOne`，DDD 跨聚合推荐做法 |
| 独立持久化 | 有自己的表 `refunds`，索引覆盖 payment_id 和 status |
| 状态校验 | 所有状态变更方法都校验当前状态 |

**退款状态机**：`PENDING -> PROCESSING -> COMPLETED/FAILED`，`PENDING -> CANCELLED`

---

#### 4.1.3 枚举类

| 枚举 | 值 | DDD 意义 |
|------|----|----------|
| `PaymentMethod` | CASH, CREDIT_CARD, DEBIT_CARD, ALIPAY, WECHAT_PAY, BANK_TRANSFER | 覆盖线上线下常见支付渠道 |
| `PaymentStatus` | PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED | 支付完整生命周期状态 |
| `PaymentType` | FINE_PAYMENT, MEMBERSHIP_FEE, DEPOSIT, OTHER | 明确支付业务来源，为跨上下文集成提供依据 |
| `RefundStatus` | PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED | 与 PaymentStatus 类似但无 REFUNDED |

---

### 4.2 领域事件（domain/event）

所有领域事件继承自 `library-shared` 的 `DomainEvent` 基类。支付上下文定义了 6 个事件：

| 事件 | 触发时机 | 关键携带信息 |
|------|----------|-------------|
| `PaymentCreatedEvent` | `createPayment()` 保存后 | paymentId, patronId, amount, paymentMethod |
| `PaymentCompletedEvent` | 支付变为 COMPLETED | paymentId, patronId, amount, referenceNumber, paymentDate |
| `PaymentFailedEvent` | 支付处理失败 | paymentId, patronId, amount, failureReason |
| `PaymentCancelledEvent` | 待支付被取消 | paymentId, patronId, amount, reason |
| `RefundRequestedEvent` | 申请退款 | refundId, paymentId, amount, reason |
| `RefundCompletedEvent` | 退款完成 | refundId, paymentId, amount, refundMethod |

**事件设计要点**：每个事件只携带下游上下文真正需要的字段。`PaymentCompletedEvent` 携带 `referenceNumber` 和 `paymentDate` 用于对账；`RefundCompletedEvent` 携带 `refundMethod` 用于统计退款渠道分布。

---

### 4.3 领域异常（domain/exception）

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `DomainException` | 由子类指定 | 异常基类，非抽象，携带 errorCode | 根据错误码映射 |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作 / 退款超额 | 409 Conflict |
| `PaymentNotFoundException` | `PAYMENT_NOT_FOUND` | 通过 ID 查找支付不存在 | 404 Not Found |

---

### 4.4 仓储接口（domain/repository）

#### `PaymentRepository.java`

```java
public interface PaymentRepository extends JpaRepository<Payment, PaymentId> {
    List<Payment> findByPatronId(PatronId patronId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByReferenceNumber(String referenceNumber);

    @Query("SELECT p FROM Payment p WHERE p.patronId = :patronId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsByPatron(PatronId patronId);
}
```

Spring Data 派生查询 + JPQL 自定义查询，覆盖按读者、状态、参考号查询以及已完成支付统计。

#### `RefundRepository.java`

```java
public interface RefundRepository extends JpaRepository<Refund, RefundId> {
    List<Refund> findByPaymentId(String paymentId);
    List<Refund> findByStatus(RefundStatus status);
}
```

通过支付 ID 查找关联退款（用于计算可退金额）和按状态筛选。

---

### 4.5 领域服务（domain/service）

#### `PaymentService.java` —— 领域服务

**DDD 角色**：**领域服务** —— 支付与退款的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final DomainEventPublisher eventPublisher;
}
```

**写操作方法**（全部标注 `@Transactional`）：

| 方法 | 业务逻辑 |
|------|----------|
| `createPayment()` | 创建 Payment -> 保存 -> 发布 PaymentCreatedEvent |
| `processPayment()` | 查找 Payment -> process() -> 保存 |
| `completePayment()` | 查找 Payment -> complete() -> 保存 -> 发布 PaymentCompletedEvent |
| `failPayment()` | 查找 Payment -> fail() -> 保存 -> 发布 PaymentFailedEvent |
| `cancelPayment()` | 查找 Payment -> cancel() -> 保存 -> 发布 PaymentCancelledEvent |
| `requestRefund()` | 查找 Payment -> 加载已有退款 -> 二次校验可退金额 -> 保存 Refund -> 发布 RefundRequestedEvent |
| `processRefund()` | 查找 Refund -> process() -> 保存 |
| `completeRefund()` | 查找 Refund -> complete() -> 保存 -> 发布 RefundCompletedEvent |

**DDD 设计要点**：

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写方法覆盖
2. **事件发布**：关键状态变更后发布事件
3. **退款金额二次校验**：`requestRefund()` 从持久化层加载已有退款，合并后重新计算可退金额，确保并发场景下的数据一致性
4. **跨聚合协调**：同时操作 Payment 和 Refund，是典型的"需要领域服务"的场景
5. **保护不变量**：业务规则编码在实体中，服务层不重复判断

---

## 5. 应用层（Application Layer）

### 5.1 应用服务（application/service）

#### `PaymentApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional(readOnly = true)
public class PaymentApplicationService {
    private final PaymentService paymentService;
}
```

**PaymentService vs PaymentApplicationService：**

| 维度 | PaymentService（领域服务） | PaymentApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（PaymentId, BigDecimal） | Command 对象 |
| 输出 | 领域对象（Payment, Refund） | DTO（PaymentDTO, RefundDTO） |
| 职责 | 业务规则 + 事件发布 | 参数转换 + 结果转换 |

应用服务将每个 Command 解包为领域对象参数，调用领域服务，再将领域对象转换为 DTO。这是应用层的核心价值——**在领域对象和外部表示之间做转换**。

---

### 5.2 命令对象（application/command）

8 个 Command 类，覆盖支付和退款的完整生命周期：

| 命令 | 必填字段 | 用途 |
|------|----------|------|
| `CreatePaymentCommand` | patronId, paymentType, amount, paymentMethod | 创建支付 |
| `ProcessPaymentCommand` | paymentId, externalTransactionId | 开始处理支付 |
| `CompletePaymentCommand` | paymentId | 完成支付 |
| `FailPaymentCommand` | paymentId, reason | 标记失败 |
| `CancelPaymentCommand` | paymentId, reason | 取消支付 |
| `RequestRefundCommand` | paymentId, amount | 申请退款（支持部分退款） |
| `ProcessRefundCommand` | refundId, externalRefundId | 开始处理退款 |
| `CompleteRefundCommand` | refundId, refundMethod | 完成退款 |

所有 Command 使用 Bean Validation 注解（`@NotNull`、`@NotBlank`、`@Positive`）进行参数校验。

---

### 5.3 查询对象（application/query）

支付上下文当前未定义独立的查询对象。查询操作直接通过 String 参数传递，由应用服务内部转换为领域 ID。当查询条件简单时，无需引入独立的 Query 类。

---

### 5.4 数据传输对象（application/dto）

#### `ApiResponse.java` —— 统一响应信封

```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) { ... }
}
```

泛型信封，成功时携带 data，失败时携带 errorCode + errorMessage，自动填充 timestamp。

#### `PaymentDTO.java`

通过 `fromDomain(Payment)` 将 Payment 聚合根展平为扁平 DTO。所有 ID 和枚举转为 String，不暴露内部字段（feeAmount, netAmount, failureReason, externalTransactionId）。

#### `RefundDTO.java`

通过 `fromDomain(Refund)` 转换，不暴露 externalRefundId 和 refundMethod 等内部字段。

---

## 6. 基础设施层（Infrastructure Layer）

支付上下文当前未创建独立的 `infrastructure` 包。原因：

1. **仓储实现**：Spring Data JPA 自动实现 `JpaRepository` 接口
2. **事件发布**：使用 `library-shared` 的 `DomainEventPublisher` 统一实现
3. **无复杂查询**：当前通过派生方法和简单 JPQL 即可满足

**预期扩展**：`PaymentDomainEventPublisher`（支付事件路由）、`ExternalPaymentGateway`（第三方支付集成）、`PaymentRepositoryImpl`（统计查询）。

---

## 7. 接口层（Interfaces Layer）

### 7.1 `PaymentController.java`

```java
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "APIs for managing payments and refunds")
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/payments` | 创建支付 | 命令 |
| `POST` | `/api/payments/{id}/process` | 处理支付 | 命令 |
| `POST` | `/api/payments/{id}/complete` | 完成支付 | 命令 |
| `POST` | `/api/payments/{id}/fail` | 标记失败 | 命令 |
| `POST` | `/api/payments/{id}/cancel` | 取消支付 | 命令 |
| `GET` | `/api/payments/{id}` | 获取支付 | 查询 |
| `GET` | `/api/payments?patronId=xxx` | 按读者查询 | 查询 |
| `POST` | `/api/payments/{id}/refunds` | 申请退款 | 命令 |
| `GET` | `/api/payments/{id}/refunds` | 查询退款列表 | 查询 |
| `POST` | `/api/payments/refunds/{id}/process` | 处理退款 | 命令 |
| `POST` | `/api/payments/refunds/{id}/complete` | 完成退款 | 命令 |

**设计要点**：CQRS（POST=命令，GET=查询）、统一 `ApiResponse<T>` 信封、Swagger 注解、状态变更用 POST、退款 API 嵌套在支付路径下体现聚合语义。请求体使用 `Map<String, Object>` 接收，Controller 内部构建 Command。

---

### 7.2 `GlobalExceptionHandler.java` —— 全局异常处理

| 异常类型 | HTTP 状态 | 说明 |
|----------|-----------|------|
| `PaymentNotFoundException` | 404 | 支付不存在 |
| `InvalidOperationException` | 409 | 状态不允许的操作 |
| `DomainException`（其他） | 按错误码映射 | 通用领域异常 |
| `MethodArgumentNotValidException` | 400 | 参数验证失败 |
| `IllegalArgumentException` | 400 | 非法参数 |
| `Exception`（兜底） | 500 | 返回通用消息，不泄露内部细节 |

---

## 8. 层间调用流程

以 **"创建支付"** 为例：

```
HTTP POST /api/payments
  |
  v
+-----------------------------------------------------------+
| PaymentController.createPayment(Map body)                   |  <- interfaces 层
|   构建 CreatePaymentCommand                                 |
+----------------------------+------------------------------+
                             |
                             v
+-----------------------------------------------------------+
| PaymentApplicationService.createPayment(CreatePaymentCommand)| <- application 层
|   1. 从 command 提取领域对象参数                              |
|   2. 调用 paymentService.createPayment(...)                  |
|   3. PaymentDTO.fromDomain(payment)                         |
+----------------------------+------------------------------+
                             |
                             v
+-----------------------------------------------------------+
| PaymentService.createPayment(...)                            | <- domain 层
|   1. Payment.create(...)  // 生成 ID, 参考号, PENDING 状态    |
|   2. paymentRepository.save(payment)                        |
|   3. eventPublisher.publish(PaymentCreatedEvent)             |
+----------+------------------+-------------------------------+
           |                  |
           v                  v
+--------------------+ +------------------------------------+
| PaymentRepository   | | DomainEventPublisher (shared)      |
| (Spring Data JPA    | |   Spring 本地事件 + Kafka 远程事件   |
|  自动实现)          | |                                    |
+--------------------+ +------------------------------------+
           |
           v
+-----------------------------------------------------------+
| PostgreSQL (library_payment)                                |
| INSERT INTO payments (id, patron_id, status, reference_number, ...) |
+-----------------------------------------------------------+
```

以 **"申请退款"** 为例（涉及两个聚合）：

```
HTTP POST /api/payments/{id}/refunds
  |
  v
+-----------------------------------------------------------+
| PaymentController -> PaymentApplicationService               |
+----------------------------+------------------------------+
                             |
                             v
+-----------------------------------------------------------+
| PaymentService.requestRefund(paymentId, amount, reason)      | <- domain 层
|   1. 查找 Payment 聚合根                                    |
|   2. payment.requestRefund() -- 内部校验                     |
|   3. 从 refundRepository 加载已有退款 -> 二次金额校验         |
|   4. refundRepository.save(refund)                          |
|   5. paymentRepository.save(payment) -- 可能更新为 REFUNDED  |
|   6. eventPublisher.publish(RefundRequestedEvent)            |
+-----------------------------------------------------------+
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `PaymentApplication.java` | Bootstrap | 启动类 | Spring Boot 入口，扫描 payment + shared |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/Payment.java` | Domain | **聚合根** | 支付生命周期管理、退款关联、金额校验 |
| 4 | `domain/model/Refund.java` | Domain | 聚合内实体 | 退款生命周期管理 |
| 5 | `domain/model/enums/PaymentMethod.java` | Domain | 枚举 | 支付方式（6种渠道） |
| 6 | `domain/model/enums/PaymentStatus.java` | Domain | 枚举 | 支付状态（6种状态） |
| 7 | `domain/model/enums/PaymentType.java` | Domain | 枚举 | 支付类型（4种类型） |
| 8 | `domain/model/enums/RefundStatus.java` | Domain | 枚举 | 退款状态（5种状态） |
| 9 | `domain/event/PaymentCreatedEvent.java` | Domain | 领域事件 | 支付已创建 |
| 10 | `domain/event/PaymentCompletedEvent.java` | Domain | 领域事件 | 支付已完成 |
| 11 | `domain/event/PaymentFailedEvent.java` | Domain | 领域事件 | 支付处理失败 |
| 12 | `domain/event/PaymentCancelledEvent.java` | Domain | 领域事件 | 支付已取消 |
| 13 | `domain/event/RefundRequestedEvent.java` | Domain | 领域事件 | 退款已申请 |
| 14 | `domain/event/RefundCompletedEvent.java` | Domain | 领域事件 | 退款已完成 |
| 15 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 16 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作（409） |
| 17 | `domain/exception/PaymentNotFoundException.java` | Domain | 异常 | 支付不存在（404） |
| 18 | `domain/repository/PaymentRepository.java` | Domain | 仓储接口 | 支付数据访问 |
| 19 | `domain/repository/RefundRepository.java` | Domain | 仓储接口 | 退款数据访问 |
| 20 | `domain/service/PaymentService.java` | Domain | **领域服务** | 支付/退款业务编排、事件发布 |
| 21 | `application/service/PaymentApplicationService.java` | Application | **应用服务** | Command -> 领域服务 -> DTO |
| 22 | `application/command/CreatePaymentCommand.java` | Application | 命令 | 创建支付入参 |
| 23 | `application/command/ProcessPaymentCommand.java` | Application | 命令 | 处理支付入参 |
| 24 | `application/command/CompletePaymentCommand.java` | Application | 命令 | 完成支付入参 |
| 25 | `application/command/FailPaymentCommand.java` | Application | 命令 | 标记失败入参 |
| 26 | `application/command/CancelPaymentCommand.java` | Application | 命令 | 取消支付入参 |
| 27 | `application/command/RequestRefundCommand.java` | Application | 命令 | 申请退款入参 |
| 28 | `application/command/ProcessRefundCommand.java` | Application | 命令 | 处理退款入参 |
| 29 | `application/command/CompleteRefundCommand.java` | Application | 命令 | 完成退款入参 |
| 30 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 31 | `application/dto/PaymentDTO.java` | Application | DTO | 支付数据传输 |
| 32 | `application/dto/RefundDTO.java` | Application | DTO | 退款数据传输 |
| 33 | `interfaces/rest/PaymentController.java` | Interfaces | REST 控制器 | 支付/退款 API（11个端点） |
| 34 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：34 个源文件**
