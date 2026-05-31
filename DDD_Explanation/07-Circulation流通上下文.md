# Library Circulation 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-circulation` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

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
  - [5.3 数据传输对象（application/dto）](#53-数据传输对象applicationdto)
- [6. 基础设施层（Infrastructure Layer）](#6-基础设施层infrastructure-layer)
- [7. 接口层（Interfaces Layer）](#7-接口层interfaces-layer)
- [8. 层间调用流程](#8-层间调用流程)
- [9. 文件清单速查表](#9-文件清单速查表)

---

## 1. 模块概览

`library-circulation` 是图书馆管理系统的**流通上下文（Circulation Context）**，负责管理图书的借阅、归还、续借、预约与召回等核心业务流程：

| 职责 | 说明 |
|------|------|
| 图书借阅 | 创建借阅记录、关联读者与副本、计算应还日期 |
| 图书归还 | 归还处理、逾期检测、罚款计算 |
| 借阅续借 | 延长借阅期限（含次数上限与预约冲突检查） |
| 图书预约 | 排队预约、队列管理、到期通知与自动过期 |
| 图书召回 | 管理员召回已借出图书、缩短应还日期 |
| 罚款管理 | 逾期罚款计算与支付/豁免状态跟踪 |
| 定时任务 | 逾期标记、到期提醒、预约过期处理 |

运行端口：`8083`，基础路径：`/api/circulation/*`

---

## 2. DDD 分层架构总览

```
library-circulation/src/main/java/com/library/circulation/
│
├── CirculationApplication.java          ← Spring Boot 启动类
├── config/                              ← 基础设施配置
│   └── CirculationConfig.java
│
├── domain/                              ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                           ← 聚合根、实体、值对象
│   │   ├── Loan.java                    ← 聚合根：借阅
│   │   ├── Hold.java                    ← 聚合根：预约
│   │   ├── Fine.java                    ← 值对象（@Embeddable）：罚款
│   │   ├── CirculationPolicy.java       ← 值对象（@Embeddable）：流通策略
│   │   └── enums/                       ← 枚举
│   │       ├── LoanStatus.java
│   │       └── HoldStatus.java
│   ├── event/                           ← 领域事件（14 个）
│   │   ├── BookBorrowedEvent.java
│   │   ├── BookReturnedEvent.java
│   │   ├── LoanRenewedEvent.java
│   │   ├── LoanRecalledEvent.java
│   │   ├── LoanCancelledEvent.java
│   │   ├── FineIncurredEvent.java
│   │   ├── DueDateReminderEvent.java
│   │   ├── OverdueNoticeEvent.java
│   │   ├── HoldPlacedEvent.java
│   │   ├── HoldFulfilledEvent.java
│   │   ├── HoldPickedUpEvent.java
│   │   ├── HoldCancelledEvent.java
│   │   ├── HoldExpiredEvent.java
│   │   └── HoldExpiredNotPickedUpEvent.java
│   ├── exception/                       ← 领域异常（含错误码）
│   │   ├── DomainException.java         ← 异常基类
│   │   ├── LoanNotFoundException.java
│   │   ├── HoldNotFoundException.java
│   │   ├── LoanRenewalException.java
│   │   ├── DuplicateHoldException.java
│   │   └── InvalidOperationException.java
│   ├── repository/                      ← 仓储接口（由 Spring Data 自动实现）
│   │   ├── LoanRepository.java
│   │   └── HoldRepository.java
│   └── service/                         ← 领域服务
│       └── CirculationManagementService.java
│
├── application/                         ← 应用层（编排、协调）
│   ├── service/
│   │   └── CirculationApplicationService.java  ← 应用服务
│   ├── command/                         ← 写操作入参
│   │   ├── BorrowBookCommand.java
│   │   ├── ReturnBookCommand.java
│   │   ├── RenewLoanCommand.java
│   │   ├── RecallBookCommand.java
│   │   ├── PlaceHoldCommand.java
│   │   └── CancelHoldCommand.java
│   └── dto/                             ← 输出传输对象
│       ├── ApiResponse.java
│       ├── LoanDTO.java
│       └── HoldDTO.java
│
├── infrastructure/                      ← （未独立目录，Spring Data 自动实现仓储）
│
└── interfaces/                          ← 接口层（REST API）
    └── rest/
        ├── LoanController.java
        ├── HoldController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces -> application -> domain
                            （仓储由 Spring Data JPA 自动实现）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务、使用 Command/DTO 做转换）
- **interfaces 层**依赖 application 层（调用应用服务）
- 仓储接口定义在 domain 层，由 Spring Data JPA 框架在运行时自动生成实现

---

## 3. 引导与配置层

### `CirculationApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.circulation", "com.library.shared"})
public class CirculationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CirculationApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类，启动整个流通上下文的 Spring 容器。`scanBasePackages` 同时扫描 `com.library.circulation` 和 `com.library.shared`，以引入共享模块的领域事件基础设施（`DomainEvent` 基类、`DomainEventPublisher`、强类型 ID 等）。

---

### `config/CirculationConfig.java`

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.library.circulation.domain.repository")
@EnableJpaAuditing
public class CirculationConfig {

    @Bean
    public CirculationPolicy circulationPolicy() {
        return CirculationPolicy.standard();
    }
}
```

**DDD 定位**：基础设施配置类，承担三重职责：

| 注解/Bean | 作用 | DDD 意义 |
|-----------|------|----------|
| `@EnableJpaRepositories` | 指定 JPA 仓储扫描包 | 让 Spring Data 自动实现 LoanRepository 和 HoldRepository |
| `@EnableJpaAuditing` | 启用 JPA 审计 | 自动填充 `@CreatedDate`、`@LastModifiedDate` |
| `circulationPolicy()` Bean | 注册标准流通策略 | 将领域策略对象注入 Spring 容器，实现策略模式的依赖注入 |

**`CirculationPolicy.standard()`** 作为默认 Bean 注册，应用服务通过构造器注入获取该策略。若未来需要按读者类型（如教职工策略）切换，只需替换 Bean 定义即可。

---

## 4. 领域层（Domain Layer）

> 领域层是 DDD 的核心。这里包含纯业务逻辑，不依赖任何外部框架（除了 JPA 注解作为持久化映射）。

### 4.1 领域模型（domain/model）

#### 4.1.1 `Loan.java` —— 聚合根：借阅

**DDD 角色**：**聚合根（Aggregate Root）**

Loan 是流通上下文中最重要的聚合根，管理图书借阅的完整生命周期，包括续借、召回、归还和罚款。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId LoanId id` | 使用共享模块的强类型 ID |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突（如同时续借和召回） |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate` | 自动记录时间戳 |
| 构造控制 | `protected Loan()` + `private Loan(...)` | JPA 要求无参构造器，`protected` 防止外部直接实例化 |
| 工厂方法 | `Loan.create(...)` | 静态工厂方法，自动生成 ID 并根据策略计算应还日期 |
| 嵌入值对象 | `@Embedded Fine fine` | 罚款作为值对象嵌入 Loan 聚合内 |
| 瞬态策略 | `@Transient CirculationPolicy` | 流通策略不持久化，运行时注入用于罚款计算 |
| 数据库索引 | `@Index` 在 patron_id、copy_id、status、due_date | 优化高频查询性能 |

**状态机：借阅生命周期**

```
ACTIVE ──renew()──> RENEWED ──renew()──> RENEWED (maxRenewalsAllowed次)
  │                     │
  │──markOverdue()──> OVERDUE ──returnBook()──> RETURNED
  │                     │
  │──recall()──> ACTIVE (dueDate缩短) ──returnBook()──> RETURNED
  │
  └──cancel()──> CANCELLED
  └──returnBook()──> RETURNED
```

**业务规则（编码在领域模型中）：**

- `returnBook()`：只有 ACTIVE、OVERDUE、RENEWED 状态才能归还；归还时若超过 dueDate 则自动计算罚款
- `renew()`：不能续借逾期、被召回、已达最大续借次数的借阅；若有排队预约也不能续借（由领域服务检查）
- `recall()`：只能召回 ACTIVE 状态的借阅；新应还日期若早于原 dueDate 则缩短
- `markOverdue()`：只有 ACTIVE 或 RENEWED 状态才能标记为逾期
- `cancel()`：已归还的借阅不能取消

**罚款计算逻辑：**

```java
private void calculateFine() {
    long overdueDays = ChronoUnit.DAYS.between(this.dueDate, this.returnDate);
    BigDecimal fineAmount = this.circulationPolicy.calculateFine((int) overdueDays);
    this.fine = new Fine(FineId.generate(), fineAmount, (int) overdueDays, LocalDateTime.now());
}
```

罚款在归还时自动计算并嵌入 Loan 聚合，计算逻辑委托给 `CirculationPolicy` 值对象。

---

#### 4.1.2 `Hold.java` —— 聚合根：预约

**DDD 角色**：**聚合根（Aggregate Root）**

Hold 管理图书预约的排队、通知、取书与过期流程。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId HoldId id` | 使用共享模块的强类型 ID |
| 队列位置 | `Integer queuePosition` | 支持排队预约，先到先得 |
| 多阶段状态 | `HoldStatus` 枚举 | 预约经历 WAITING -> READY_FOR_PICKUP -> FULFILLED 多个阶段 |
| 取书期限 | `availableUntilDate` | 进入可取书状态后有有限时间窗口 |
| 数据库索引 | `@Index` 在 book_id、patron_id、status、expiration_date | 优化排队查询 |

**状态机：预约生命周期**

```
WAITING ──fulfill()──> READY_FOR_PICKUP ──markAsPickedUp()──> FULFILLED
  │                          │
  │──cancel()──> CANCELLED   │──markAsExpiredNotPickedUp()──> EXPIRED_NOT_PICKED_UP
  │──markAsExpired()──> EXPIRED
```

**业务规则（编码在领域模型中）：**

- `fulfill()`：只有 WAITING 状态才能被满足；自动计算取书截止日期
- `cancel()`：已满足（FULFILLED）或已取消的预约不能再取消
- `markAsPickedUp()`：必须在 READY_FOR_PICKUP 状态且在取书期限内
- `markAsExpiredNotPickedUp()`：进入可取书状态但超时未取
- `markAsExpired()`：等待中的预约过期
- `updateQueuePosition()`：队列位置必须为正整数
- `extendExpiration()`：只能延长等待中的预约有效期

**队列管理**：预约取消或过期后，由领域服务 `CirculationManagementService.updateQueuePositions()` 重新排列剩余等待中的预约的队列位置，确保连续性。

---

#### 4.1.3 `Fine.java` —— 值对象：罚款

**DDD 角色**：**值对象（Value Object），使用 `@Embeddable` 嵌入 Loan 聚合**

Fine 封装了罚款金额、逾期天数、支付状态和豁免信息。

```java
@Embeddable
public class Fine {
    @Embedded private FineId id;
    private BigDecimal amount;
    private Integer overdueDays;
    private LocalDateTime calculatedAt;
    private LocalDateTime paidDate;
    private LocalDateTime waivedDate;
    private String waivedReason;
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| `@Embeddable` | 不创建独立的数据库表，罚款字段嵌入 `loans` 表 |
| 金额验证 | `validateAmount()` 确保金额非负并统一精度为两位小数 |
| 支付方法 | `pay()` 要求支付金额 >= 罚款金额，已支付或已豁免不可再支付 |
| 豁免方法 | `waive()` 需提供理由，已支付不可豁免 |
| 状态判断 | `isPaid()`、`isWaived()`、`isOutstanding()` 提供罚款状态查询 |
| 不可变标识 | 基于 `FineId` 实现 `equals()`/`hashCode()` |

**`@Embeddable` vs 独立实体**：罚款没有独立的生命周期——它随 Loan 创建而创建，随 Loan 删除而删除。将其设计为值对象嵌入 Loan 是正确的 DDD 选择。

---

#### 4.1.4 `CirculationPolicy.java` —— 值对象：流通策略

**DDD 角色**：**值对象（Value Object），使用 `@Embeddable`**

CirculationPolicy 封装了所有流通相关的业务规则参数，实现了**策略模式（Strategy Pattern）**。

```java
@Embeddable
public class CirculationPolicy {
    private Integer loanPeriodDays;           // 借阅天数
    private Integer maxRenewalsAllowed;       // 最大续借次数
    private BigDecimal dailyFineRate;         // 每日罚款费率
    private BigDecimal maxFineAmount;         // 罚款上限
    private Integer gracePeriodDays;          // 宽限期
    private Integer holdExpirationDays;       // 预约等待过期天数
    private Integer holdPickupDays;           // 取书天数
    private Integer recallNoticeDays;         // 召回通知天数
    private Integer reminderDaysBeforeDue;    // 到期前提醒天数
}
```

**工厂方法：**

```java
public static CirculationPolicy standard() {
    return new CirculationPolicy(30, 2, new BigDecimal("0.50"), new BigDecimal("50.00"),
                                  3, 7, 5, 7, 3);
}

public static CirculationPolicy faculty() {
    return new CirculationPolicy(90, 3, new BigDecimal("0.25"), new BigDecimal("30.00"),
                                  7, 14, 7, 14, 7);
}
```

| 策略 | 借阅天数 | 最大续借 | 每日罚款 | 罚款上限 | 宽限期 | 预约过期 | 取书天数 | 召回通知 | 到期提醒 |
|------|----------|----------|----------|----------|--------|----------|----------|----------|----------|
| 标准 | 30 | 2 | 0.50 | 50.00 | 3 | 7 | 5 | 7 | 3 |
| 教职工 | 90 | 3 | 0.25 | 30.00 | 7 | 14 | 7 | 14 | 7 |

**核心业务方法：**

- `calculateFine(int overdueDays)`：每日费率 * 逾期天数，不超过上限（`calculated.min(maxFineAmount)`）
- `isInGracePeriod(int overdueDays)`：宽限期内不算正式逾期

**`@Embeddable` 注解说明**：虽然当前实现中策略是通过 `@Bean` 注入而非嵌入实体的，标记 `@Embeddable` 为未来可能的按读者类型持久化策略预留了灵活性。

---

#### 4.1.5 `enums/LoanStatus.java` —— 枚举：借阅状态

```java
public enum LoanStatus {
    ACTIVE,       // 活跃（正常借阅中）
    RETURNED,     // 已归还
    OVERDUE,      // 已逾期
    RECALLED,     // 已召回（状态值保留，当前逻辑中 recall 不改变状态）
    RENEWED,      // 已续借
    CANCELLED,    // 已取消
    LOST          // 已丢失（预留状态）
}
```

**DDD 意义**：定义了借阅的完整生命周期状态。`LOST` 是预留状态，用于标记图书丢失的场景。

---

#### 4.1.6 `enums/HoldStatus.java` —— 枚举：预约状态

```java
public enum HoldStatus {
    WAITING,              // 排队等待中
    READY_FOR_PICKUP,     // 已到书，等待取书
    FULFILLED,            // 已取书完成
    CANCELLED,            // 已取消
    EXPIRED,              // 等待过期（未到书即过期）
    EXPIRED_NOT_PICKED_UP // 到书后未取而过期
}
```

**DDD 意义**：预约有两条过期路径——等待中过期（`EXPIRED`）和到书后未取过期（`EXPIRED_NOT_PICKED_UP`），区分这两种场景对业务统计和读者信用评估有重要意义。

---

### 4.2 领域事件（domain/event）

> 领域事件表示领域中已经发生的、有业务意义的事情。在 DDD 中，事件用于实现聚合间和上下文间的解耦通信。

所有领域事件都继承自 `library-shared` 模块中的 `DomainEvent` 基类，该基类提供了 `eventId`、`eventType`、`occurredOn` 等通用属性。

#### 4.2.1 借阅相关事件

##### `BookBorrowedEvent.java`

```java
public class BookBorrowedEvent extends DomainEvent {
    private final LoanId loanId;
    private final CopyId copyId;
    private final PatronId patronId;
    private final BookId bookId;
    private final LocalDateTime loanDate;
    private final LocalDateTime dueDate;
}
```

**触发时机**：`CirculationManagementService.borrowBook()` 成功保存借阅后发布。
**下游消费方**：库存上下文（更新副本状态为借出）、读者上下文（更新借阅计数）、通知上下文（发送借阅确认）。

---

##### `BookReturnedEvent.java`

```java
public class BookReturnedEvent extends DomainEvent {
    private final LoanId loanId;
    private final CopyId copyId;
    private final PatronId patronId;
    private final BookId bookId;
    private final LocalDateTime returnDate;
    private final BigDecimal fineAmount;   // 可能为 null（未逾期时）
}
```

**触发时机**：图书归还后发布。若产生罚款，`fineAmount` 非空。
**下游消费方**：库存上下文（更新副本状态为可用）、支付上下文（如有罚款则创建待支付记录）。

---

##### `LoanRenewedEvent.java`

```java
public class LoanRenewedEvent extends DomainEvent {
    private final LoanId loanId;
    private final LocalDateTime oldDueDate;
    private final LocalDateTime newDueDate;
    private final int renewalCount;
    private final LocalDateTime renewedAt;
}
```

**触发时机**：借阅续借成功后发布。携带新旧应还日期，方便下游系统更新记录。

---

##### `LoanRecalledEvent.java`

```java
public class LoanRecalledEvent extends DomainEvent {
    private final LoanId loanId;
    private final LocalDateTime newDueDate;
    private final String reason;
    private final LocalDateTime recalledAt;
}
```

**触发时机**：管理员召回图书后发布。
**下游消费方**：通知上下文（发送召回通知给读者）、库存上下文。

---

##### `LoanCancelledEvent.java`

```java
public class LoanCancelledEvent extends DomainEvent {
    private final LoanId loanId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final String reason;
}
```

**触发时机**：借阅被取消后发布。

---

##### `FineIncurredEvent.java`

```java
public class FineIncurredEvent extends DomainEvent {
    private final FineId fineId;
    private final LoanId loanId;
    private final PatronId patronId;
    private final BigDecimal amount;
    private final int overdueDays;
    private final LocalDateTime incurredAt;
}
```

**触发时机**：归还时产生罚款后，紧随 `BookReturnedEvent` 之后发布。
**下游消费方**：支付上下文（创建待支付账单）、读者上下文（更新信用记录）。

---

##### `DueDateReminderEvent.java`

```java
public class DueDateReminderEvent extends DomainEvent {
    private final LoanId loanId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final LocalDateTime dueDate;
}
```

**触发时机**：定时任务 `sendDueDateReminders()` 检测到借阅即将到期时发布。
**下游消费方**：通知上下文（发送到期提醒邮件/短信）。

---

##### `OverdueNoticeEvent.java`

```java
public class OverdueNoticeEvent extends DomainEvent {
    private final LoanId loanId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final long daysOverdue;
}
```

**触发时机**：定时任务 `processOverdueLoans()` 将借阅标记为逾期后发布。
**下游消费方**：通知上下文（发送逾期通知）。

---

#### 4.2.2 预约相关事件

##### `HoldPlacedEvent.java`

```java
public class HoldPlacedEvent extends DomainEvent {
    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final int queuePosition;
    private final LocalDateTime placedAt;
}
```

**触发时机**：读者成功预约图书后发布。携带队列位置信息。

---

##### `HoldFulfilledEvent.java`

```java
public class HoldFulfilledEvent extends DomainEvent {
    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final CopyId copyId;
    private final LocalDateTime availableUntil;
    private final LocalDateTime fulfilledAt;
}
```

**触发时机**：预约被满足（有副本可分配）后发布。
**下游消费方**：通知上下文（通知读者来取书）。

---

##### `HoldPickedUpEvent.java`

```java
public class HoldPickedUpEvent extends DomainEvent {
    private final HoldId holdId;
    private final PatronId patronId;
    private final BookId bookId;
    private final CopyId copyId;
}
```

**触发时机**：读者取走预约图书后发布。预约流程完成。

---

##### `HoldCancelledEvent.java`

```java
public class HoldCancelledEvent extends DomainEvent {
    private final HoldId holdId;
    private final BookId bookId;
    private final PatronId patronId;
    private final String reason;
    private final LocalDateTime cancelledAt;
}
```

**触发时机**：预约被取消后发布。触发队列位置重排。

---

##### `HoldExpiredEvent.java`

```java
public class HoldExpiredEvent extends DomainEvent {
    private final HoldId holdId;
    private final PatronId patronId;
    private final BookId bookId;
}
```

**触发时机**：等待中的预约到期未到书而过期。由定时任务 `processExpiredHolds()` 触发。

---

##### `HoldExpiredNotPickedUpEvent.java`

```java
public class HoldExpiredNotPickedUpEvent extends DomainEvent {
    private final HoldId holdId;
    private final PatronId patronId;
    private final BookId bookId;
}
```

**触发时机**：预约到书后读者未在规定时间内取书而过期。会触发该副本的下一个预约处理。

---

### 4.3 领域异常（domain/exception）

> DDD 中，异常也是领域逻辑的一部分。每个异常都带有**错误码（errorCode）**，方便 API 层统一处理和前端国际化。

#### 4.3.1 `DomainException.java` —— 异常基类

```java
public abstract class DomainException extends RuntimeException {
    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

**设计要点：**

- `abstract`：不能直接实例化，所有领域异常必须继承此类
- `errorCode`：每个子类定义自己的错误码字符串
- 继承 `RuntimeException`：是非受检异常，不强制调用方处理

---

#### 4.3.2 具体异常类

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `LoanNotFoundException` | `LOAN_NOT_FOUND` | 通过 ID 查找借阅不存在 | 404 |
| `HoldNotFoundException` | `HOLD_NOT_FOUND` | 通过 ID 查找预约不存在 | 404 |
| `LoanRenewalException` | `LOAN_RENEWAL_FAILED` | 续借条件不满足（逾期/已召回/达上限/有预约排队） | 409 Conflict |
| `DuplicateHoldException` | `DUPLICATE_HOLD` | 同一读者对同一书重复预约 | 409 Conflict |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作（如归还已归还的借阅） | 409 Conflict |

每个异常类的实现都非常简洁，例如：

```java
public class LoanNotFoundException extends DomainException {
    public LoanNotFoundException(Object loanId) {
        super("LOAN_NOT_FOUND", "Loan not found: " + loanId);
    }
}
```

**错误码到 HTTP 状态的映射**：在 `GlobalExceptionHandler` 中根据 errorCode 字符串映射。

---

### 4.4 仓储接口（domain/repository）

> 仓储（Repository）是 DDD 中领域模型与数据存储之间的桥梁。本模块的仓储直接继承 Spring Data JPA 的 `JpaRepository`，由框架在运行时自动生成实现。

#### 4.4.1 `LoanRepository.java`

```java
@Repository
public interface LoanRepository extends JpaRepository<Loan, LoanId> {

    List<Loan> findByPatronId(PatronId patronId);
    List<Loan> findByPatronIdAndStatus(PatronId patronId, LoanStatus status);
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByCopyId(CopyId copyId);
    List<Loan> findByBookIdAndStatus(BookId bookId, LoanStatus status);
    List<Loan> findByDueDateBeforeAndStatus(LocalDateTime date, LoanStatus status);
    List<Loan> findByDueDateBeforeAndStatusIn(LocalDateTime date, List<LoanStatus> statuses);
    boolean existsByPatronIdAndStatus(PatronId patronId, LoanStatus status);
    boolean existsByCopyIdAndStatus(CopyId copyId, LoanStatus status);
    long countByPatronIdAndStatus(PatronId patronId, LoanStatus status);
}
```

**方法类型分析：**

| 方法 | 用途 | 调用方 |
|------|------|--------|
| `findByPatronId()` | 查询读者全部借阅历史 | `getLoanHistory()` |
| `findByPatronIdAndStatus()` | 查询读者活跃借阅 | `getActiveLoans()` |
| `findByDueDateBeforeAndStatusIn()` | 查询即将到期/已逾期的借阅 | `processOverdueLoans()`, `sendDueDateReminders()` |
| `existsByCopyIdAndStatus()` | 检查副本是否有活跃借阅 | 跨上下文查询 |
| `countByPatronIdAndStatus()` | 统计读者活跃借阅数 | 借阅限额检查 |

所有方法都是 Spring Data 派生查询，由框架根据方法名自动生成 SQL。

---

#### 4.4.2 `HoldRepository.java`

```java
@Repository
public interface HoldRepository extends JpaRepository<Hold, HoldId> {

    List<Hold> findByPatronId(PatronId patronId);
    List<Hold> findByBookId(BookId bookId);
    List<Hold> findByBookIdAndStatus(BookId bookId, HoldStatus status);
    Optional<Hold> findFirstByBookIdAndStatusOrderByQueuePositionAsc(BookId bookId, HoldStatus status);
    boolean existsByBookIdAndPatronIdAndStatusIn(BookId bookId, PatronId patronId, List<HoldStatus> statuses);
    List<Hold> findByStatusAndExpirationDateBefore(HoldStatus status, LocalDateTime date);
    List<Hold> findByStatusAndAvailableUntilDateBefore(HoldStatus status, LocalDateTime date);
    long countByBookIdAndStatus(BookId bookId, HoldStatus status);
}
```

**关键方法分析：**

| 方法 | 用途 | DDD 意义 |
|------|------|----------|
| `findFirstByBookIdAndStatusOrderByQueuePositionAsc()` | 获取排队中的第一个预约 | 支持先到先得的队列调度 |
| `existsByBookIdAndPatronIdAndStatusIn()` | 检查重复预约 | 领域服务用于防止重复预约 |
| `findByStatusAndExpirationDateBefore()` | 查询过期的等待中预约 | 定时任务处理预约过期 |
| `findByStatusAndAvailableUntilDateBefore()` | 查询超时未取的预约 | 定时任务处理未取过期 |
| `countByBookIdAndStatus()` | 计算排队人数 | 用于计算新预约的队列位置和续借冲突检查 |

---

### 4.5 领域服务（domain/service）

> 领域服务用于承载**不适合放在单个实体或值对象中的业务逻辑**，例如跨聚合的协调、仓储调用和领域事件发布。

#### 4.5.1 `CirculationManagementService.java`

**DDD 角色**：**领域服务** —— 流通上下文的核心业务编排

```java
@Service
public class CirculationManagementService {
    private final LoanRepository loanRepository;
    private final HoldRepository holdRepository;
    private final DomainEventPublisher eventPublisher;
}
```

**职责一览：**

| 方法 | 业务逻辑 | 事件发布 |
|------|----------|----------|
| `borrowBook()` | 创建 Loan → 保存 | `BookBorrowedEvent` |
| `returnBook()` | 查找 Loan → 归还 → 计算罚款 → 保存 → 处理下一个预约 | `BookReturnedEvent` + `FineIncurredEvent`（如有罚款） |
| `renewLoan()` | 查找 Loan → 检查预约冲突 → 续借 → 保存 | `LoanRenewedEvent` |
| `recallBook()` | 查找 Loan → 召回（缩短 dueDate）→ 保存 | `LoanRecalledEvent` |
| `cancelLoan()` | 查找 Loan → 取消 → 保存 | `LoanCancelledEvent` |
| `placeHold()` | 检查重复 → 计算队列位置 → 创建 Hold → 保存 | `HoldPlacedEvent` |
| `cancelHold()` | 查找 Hold → 取消 → 重排队列 → 保存 | `HoldCancelledEvent` |
| `fulfillHold()` | 查找 Hold → 满足（分配副本）→ 保存 | `HoldFulfilledEvent` |
| `pickupHold()` | 查找 Hold → 标记已取 → 保存 | `HoldPickedUpEvent` |
| `processExpiredHolds()` | 批量处理过期等待 + 过期未取 | `HoldExpiredEvent` / `HoldExpiredNotPickedUpEvent` |
| `processOverdueLoans()` | 批量标记逾期 | `OverdueNoticeEvent` |
| `sendDueDateReminders()` | 批量发送到期提醒 | `DueDateReminderEvent` |

**DDD 设计要点：**

1. **事务管理**：写操作方法使用 `@Transactional`，读操作使用 `@Transactional(readOnly = true)`
2. **事件发布**：每个状态变更操作完成后发布对应的领域事件，实现与其他上下文的解耦
3. **跨聚合协调**：`returnBook()` 中归还借阅后自动处理下一个预约；`cancelHold()` 后重排队列
4. **批量操作**：`processExpiredHolds()`、`processOverdueLoans()`、`sendDueDateReminders()` 是定时任务调用的批量处理方法

**关键业务流程——还书后的预约处理：**

```java
private void processNextHold(BookId bookId) {
    Optional<Hold> nextHold = holdRepository
        .findFirstByBookIdAndStatusOrderByQueuePositionAsc(bookId, HoldStatus.WAITING);
    // 实际副本分配由调用方（拥有库存访问权限的组件）完成
}
```

`processNextHold()` 查找下一个等待中的预约，为后续的副本分配做准备。实际的副本分配和预约满足需要跨上下文协调。

**预约队列重排：**

```java
private void updateQueuePositions(BookId bookId) {
    List<Hold> waitingHolds = holdRepository.findByBookIdAndStatus(bookId, HoldStatus.WAITING);
    for (int i = 0; i < waitingHolds.size(); i++) {
        Hold hold = waitingHolds.get(i);
        if (hold.getQueuePosition() != (i + 1)) {
            hold.updateQueuePosition(i + 1);
            holdRepository.save(hold);
        }
    }
}
```

预约取消或过期后，该方法重新排列剩余等待中预约的 `queuePosition`，确保连续性。

---

## 5. 应用层（Application Layer）

> 应用层是 DDD 中的**"编排层"**，它不包含业务逻辑，而是协调领域对象完成用例。它的职责是：接收外部请求 -> 转换参数 -> 调用领域服务 -> 转换输出 -> 返回结果。

### 5.1 应用服务（application/service）

#### `CirculationApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional
public class CirculationApplicationService {
    private final CirculationManagementService circulationService;
    private final CirculationPolicy policy;
}
```

**CirculationManagementService vs CirculationApplicationService 的区别：**

| 维度 | CirculationManagementService（领域服务） | CirculationApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（LoanId, CopyId, PatronId, CirculationPolicy） | 外部格式（String, Command 对象） |
| 输出 | 领域对象（Loan, Hold） | DTO（LoanDTO, HoldDTO） |
| 职责 | 业务规则校验和领域事件发布 | 参数转换和结果转换 |
| 调用者 | 应用服务 | 控制器 |

**典型方法流程**（以 `borrowBook` 为例）：

```
Controller 调用 -> CirculationApplicationService.borrowBook(BorrowBookCommand)
  1. CopyId.of(command.getCopyId())       // String -> 强类型 ID
  2. PatronId.of(command.getPatronId())   // String -> 强类型 ID
  3. BookId.of(command.getBookId())       // String -> 强类型 ID
  4. circulationService.borrowBook(...)   // 调用领域服务
  5. LoanDTO.fromDomain(loan)             // Loan -> DTO
  6. 返回 LoanDTO
```

**设计特点**：

- 类级别 `@Transactional`，所有方法默认在事务中执行
- 读操作方法覆盖为 `@Transactional(readOnly = true)`
- 注入 `CirculationPolicy` Bean 作为默认策略，所有操作共用同一策略实例

---

### 5.2 命令对象（application/command）

> 命令（Command）是 CQRS 模式中"写操作"的输入对象，封装了执行某个操作所需的全部参数。

#### `BorrowBookCommand.java`

```java
public class BorrowBookCommand {
    @NotBlank(message = "Copy ID must not be blank")
    private String copyId;
    @NotBlank(message = "Patron ID must not be blank")
    private String patronId;
    @NotBlank(message = "Book ID must not be blank")
    private String bookId;
}
```

**设计要点**：

- 使用 `@NotBlank` Bean Validation 注解，在 Controller 层由框架自动验证
- 所有 ID 字段使用 String 类型（而非强类型 ID），由应用服务负责转换为领域对象
- 提供无参构造器（JSON 反序列化）和全参构造器

---

#### `ReturnBookCommand.java`

```java
public class ReturnBookCommand {
    @NotBlank(message = "Loan ID must not be blank")
    private String loanId;
}
```

只需借阅 ID 即可完成归还操作（归还日期由系统自动确定）。

---

#### `RenewLoanCommand.java`

```java
public class RenewLoanCommand {
    @NotBlank(message = "Loan ID must not be blank")
    private String loanId;
}
```

---

#### `RecallBookCommand.java`

```java
public class RecallBookCommand {
    @NotBlank(message = "Loan ID must not be blank")
    private String loanId;
    private String reason;   // 可选
}
```

召回原因 `reason` 是可选字段，不使用 `@NotBlank`。

---

#### `PlaceHoldCommand.java`

```java
public class PlaceHoldCommand {
    @NotBlank(message = "Book ID must not be blank")
    private String bookId;
    @NotBlank(message = "Patron ID must not be blank")
    private String patronId;
    private String pickupLibraryId;   // 可选
}
```

预约图书需要指定书和读者，取书图书馆可选。

---

#### `CancelHoldCommand.java`

```java
public class CancelHoldCommand {
    @NotBlank(message = "Hold ID must not be blank")
    private String holdId;
    private String reason;   // 可选
}
```

---

### 5.3 数据传输对象（application/dto）

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

**设计要点：**

- 泛型 `<T>` 支持任意数据类型
- 成功时：`success=true, data=实际数据, errorMessage=null`
- 失败时：`success=false, data=null, errorCode=错误码, errorMessage=错误信息`
- `timestamp` 自动记录响应时间
- 使用 `class` 而非 `record`（与 catalog 模块不同），因为需要无参构造器支持 JSON 反序列化

---

#### `LoanDTO.java`

```java
public class LoanDTO {
    private String id;
    private String copyId;
    private String patronId;
    private String bookId;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;
    private Integer renewalCount;
    private BigDecimal fineAmount;
    private Boolean isRecalled;
    private LocalDateTime createdAt;

    public static LoanDTO fromDomain(Loan loan) { ... }
}
```

**关键设计**：`fromDomain(Loan)` 是**组装器方法（Assembler）**，将 Loan 聚合根展平为扁平 DTO。注意类型转换：

- `LoanId` -> `String`（`loan.getId().getValue()`）
- `LoanStatus` 枚举 -> `String`（`loan.getStatus().name()`）
- `Fine` 嵌入值对象 -> `BigDecimal`（仅取金额）
- `recallDate != null` -> `Boolean isRecalled`

---

#### `HoldDTO.java`

```java
public class HoldDTO {
    private String id;
    private String bookId;
    private String patronId;
    private LocalDateTime requestDate;
    private LocalDateTime expirationDate;
    private String status;
    private Integer queuePosition;
    private String fulfilledCopyId;
    private LocalDateTime availableUntilDate;
    private String pickupLibraryId;
    private LocalDateTime createdAt;

    public static HoldDTO fromDomain(Hold hold) { ... }
}
```

与 LoanDTO 类似，`fromDomain(Hold)` 将 Hold 聚合根转换为扁平 DTO，`fulfilledCopyId` 在未满足时为 null。

---

## 6. 基础设施层（Infrastructure Layer）

> 本模块的仓储接口（`LoanRepository`、`HoldRepository`）由 Spring Data JPA 自动生成实现，无需手写基础设施层代码。这是流通上下文与编目上下文的一个架构差异——编目上下文有 `BookRepositoryImpl`（Criteria API）和 `CatalogDomainEventPublisher`，而流通上下文的查询足够简单，Spring Data 派生查询即可满足。

**隐式基础设施组件：**

| 组件 | 实现方式 | 说明 |
|------|----------|------|
| `LoanRepository` 实现 | Spring Data JPA 自动生成 | 所有方法都是派生查询 |
| `HoldRepository` 实现 | Spring Data JPA 自动生成 | 所有方法都是派生查询 |
| 领域事件发布 | `library-shared` 模块的 `DomainEventPublisher` | 通过 Spring ApplicationEventPublisher 发布本地事件 |
| 数据库连接 | `application.yml` 配置 | PostgreSQL，端口 5432，数据库 `library_circulation` |

---

## 7. 接口层（Interfaces Layer）

> 接口层是系统与外部世界的边界。它接收 HTTP 请求，调用应用服务，返回 HTTP 响应。**不包含任何业务逻辑。**

### 7.1 `LoanController.java`

```java
@RestController
@RequestMapping("/api/circulation")
@Tag(name = "Loan Management", description = "APIs for managing book loans")
public class LoanController {
    private final CirculationApplicationService circulationService;
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/circulation/loans` | 借阅图书 | 命令 |
| `POST` | `/api/circulation/loans/{loanId}/return` | 归还图书 | 命令（状态变更） |
| `POST` | `/api/circulation/loans/{loanId}/renew` | 续借 | 命令（状态变更） |
| `POST` | `/api/circulation/loans/{loanId}/recall` | 召回 | 命令（状态变更） |
| `GET` | `/api/circulation/loans/{loanId}` | 查询借阅详情 | 查询 |
| `GET` | `/api/circulation/patrons/{patronId}/loans` | 查询读者活跃借阅 | 查询 |
| `GET` | `/api/circulation/patrons/{patronId}/loans/history` | 查询读者借阅历史 | 查询 |

**设计要点：**

- **CQRS 体现**：POST 是命令（写），GET 是查询（读）；状态变更操作（return/renew/recall）使用 POST 而非 PUT/PATCH
- **统一响应**：所有端点返回 `ApiResponse<T>` 信封
- **Swagger 注解**：`@Operation(summary = ...)` 和 `@Tag` 生成 API 文档
- **参数验证**：`@Valid @RequestBody BorrowBookCommand` 触发 Bean Validation
- **RESTful 设计**：资源嵌套在 `/loans` 下，操作作为子路径（`/return`、`/renew`、`/recall`）

---

### 7.2 `HoldController.java`

```java
@RestController
@RequestMapping("/api/circulation")
@Tag(name = "Hold Management", description = "APIs for managing book holds")
public class HoldController {
    private final CirculationApplicationService circulationService;
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/circulation/holds` | 预约图书 | 命令 |
| `DELETE` | `/api/circulation/holds/{holdId}` | 取消预约 | 命令 |
| `GET` | `/api/circulation/holds/{holdId}` | 查询预约详情 | 查询 |
| `GET` | `/api/circulation/patrons/{patronId}/holds` | 查询读者预约 | 查询 |
| `GET` | `/api/circulation/books/{bookId}/holds` | 查询图书预约队列 | 查询 |

**设计要点：**

- 两个控制器共享 `/api/circulation` 前缀，通过 Swagger Tag 分组文档
- `cancelHold` 使用 `DELETE` 方法，`reason` 通过 `@RequestParam` 传入
- `getBookHoldQueue` 按 `bookId` 查询，返回该书的所有等待中预约（按队列位置排序）

---

### 7.3 `GlobalExceptionHandler.java` —— 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ExceptionHandler(IllegalArgumentException.class)
    @ExceptionHandler(IllegalStateException.class)
}
```

**DDD 定位**：**跨切面（Cross-Cutting Concern）**，在接口层统一处理所有异常。

**错误码 -> HTTP 状态映射逻辑：**

```java
private HttpStatus mapToHttpStatus(String errorCode) {
    return switch (errorCode) {
        case "LOAN_NOT_FOUND", "HOLD_NOT_FOUND" -> HttpStatus.NOT_FOUND;         // 404
        case "INVALID_OPERATION", "LOAN_RENEWAL_FAILED", "DUPLICATE_HOLD" -> HttpStatus.CONFLICT;  // 409
        default -> HttpStatus.BAD_REQUEST;                                        // 400
    };
}
```

| 异常类型 | HTTP 状态 | 说明 |
|----------|-----------|------|
| `DomainException`（含 NOT_FOUND） | 404 | 资源不存在 |
| `DomainException`（含 INVALID_OPERATION / LOAN_RENEWAL_FAILED / DUPLICATE_HOLD） | 409 | 业务冲突 |
| `DomainException`（其他） | 400 | 一般业务错误 |
| `MethodArgumentNotValidException` | 400 | 参数验证失败 |
| `IllegalArgumentException` | 400 | 非法参数 |
| `IllegalStateException` | 409 | 非法状态（如重复支付罚款） |

**返回格式统一**：所有异常都包装成 `ApiResponse.error(errorCode, message)`，前端只需处理一种响应格式。

---

## 8. 层间调用流程

以 **"借阅图书"** 为例，展示一次完整请求的层间调用链：

```
HTTP POST /api/circulation/loans
  {"copyId": "CP001", "patronId": "PT001", "bookId": "BK001"}
  |
  v
+-----------------------------------------------------------+
| LoanController.borrowBook(BorrowBookCommand)               |  <- interfaces 层
|   @Valid 触发 Bean Validation (@NotBlank 校验)             |
+---------------------------+-------------------------------+
                            |
                            v
+-----------------------------------------------------------+
| CirculationApplicationService.borrowBook(BorrowBookCommand)|  <- application 层
|   1. CopyId.of("CP001")     // String -> 强类型 ID         |
|   2. PatronId.of("PT001")   // String -> 强类型 ID         |
|   3. BookId.of("BK001")     // String -> 强类型 ID         |
|   4. 调用 circulationService.borrowBook(...)               |
|   5. LoanDTO.fromDomain(loan)  // Loan -> DTO              |
+---------------------------+-------------------------------+
                            |
                            v
+-----------------------------------------------------------+
| CirculationManagementService.borrowBook(copyId, patronId,  |  <- domain 层（服务）
|                                          bookId, policy)   |
|   1. LocalDateTime now = LocalDateTime.now()               |
|   2. Loan.create(copyId, patronId, bookId, now, policy)    |
|      -> 生成 LoanId                                        |
|      -> dueDate = now + policy.loanPeriodDays              |
|   3. loanRepository.save(loan)                             |
|   4. eventPublisher.publish(BookBorrowedEvent)             |
+-----+-----------------------------+------------------------+
      |                             |
      v                             v
+----------------+  +-----------------------------------------+
| LoanRepository |  | DomainEventPublisher                     |  <- infrastructure 层
| (Spring Data   |  |   -> ApplicationEventPublisher.publish()  |    Spring 本地事件
|  JPA 自动实现) |  |   -> Kafka (如已配置)                     |    Kafka 远程事件
+-------+--------+  +-----------------------------------------+
        |
        v
+-----------------------------------------------------------+
| PostgreSQL (library_circulation)                           |  <- 数据库
| INSERT INTO loans (id, copy_id, patron_id, book_id,        |
|   loan_date, due_date, status, ...)                        |
+-----------------------------------------------------------+
```

以 **"归还图书（含罚款和预约处理）"** 为例，展示更复杂的调用链：

```
HTTP POST /api/circulation/loans/{loanId}/return
  |
  v
+-----------------------------------------------------------+
| LoanController.returnBook(loanId)                          |  <- interfaces 层
+---------------------------+-------------------------------+
                            |
                            v
+-----------------------------------------------------------+
| CirculationApplicationService.returnBook(ReturnBookCommand)|  <- application 层
|   1. LoanId.of(loanId)     // String -> 强类型 ID          |
|   2. circulationService.returnBook(loanId)                 |
|   3. LoanDTO.fromDomain(loan)                              |
+---------------------------+-------------------------------+
                            |
                            v
+-----------------------------------------------------------+
| CirculationManagementService.returnBook(loanId)            |  <- domain 层（服务）
|   1. loanRepository.findById(loanId)                       |
|      -> 未找到则抛出 LoanNotFoundException                   |
|   2. loan.returnBook(now)                                  |
|      -> 状态检查（ACTIVE/OVERDUE/RENEWED）                   |
|      -> 若逾期则调用 calculateFine()                        |
|         -> CirculationPolicy.calculateFine()               |
|         -> 创建 Fine 值对象嵌入 Loan                        |
|   3. loanRepository.save(loan)                             |
|   4. eventPublisher.publish(BookReturnedEvent)             |
|   5. 若有罚款: eventPublisher.publish(FineIncurredEvent)    |
|   6. processNextHold(bookId)                               |
|      -> 查找该书下一个等待中的预约                           |
+-----------------------------------------------------------+
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `CirculationApplication.java` | Bootstrap | 启动类 | Spring Boot 入口 |
| 2 | `config/CirculationConfig.java` | Config | 配置 | JPA 审计 + 仓储扫描 + 流通策略 Bean |
| 3 | `domain/model/Loan.java` | Domain | **聚合根** | 借阅生命周期管理（借出/归还/续借/召回/逾期/罚款） |
| 4 | `domain/model/Hold.java` | Domain | **聚合根** | 预约排队管理（预约/满足/取书/取消/过期） |
| 5 | `domain/model/Fine.java` | Domain | **值对象（@Embeddable）** | 罚款金额、支付与豁免状态 |
| 6 | `domain/model/CirculationPolicy.java` | Domain | **值对象（@Embeddable）** | 流通策略参数（借阅天数/罚款费率/续借上限等） |
| 7 | `domain/model/enums/LoanStatus.java` | Domain | 枚举 | 借阅状态（7 种） |
| 8 | `domain/model/enums/HoldStatus.java` | Domain | 枚举 | 预约状态（6 种） |
| 9 | `domain/event/BookBorrowedEvent.java` | Domain | 领域事件 | 图书已借出 |
| 10 | `domain/event/BookReturnedEvent.java` | Domain | 领域事件 | 图书已归还 |
| 11 | `domain/event/LoanRenewedEvent.java` | Domain | 领域事件 | 借阅已续借 |
| 12 | `domain/event/LoanRecalledEvent.java` | Domain | 领域事件 | 借阅已召回 |
| 13 | `domain/event/LoanCancelledEvent.java` | Domain | 领域事件 | 借阅已取消 |
| 14 | `domain/event/FineIncurredEvent.java` | Domain | 领域事件 | 罚款已产生 |
| 15 | `domain/event/DueDateReminderEvent.java` | Domain | 领域事件 | 到期提醒 |
| 16 | `domain/event/OverdueNoticeEvent.java` | Domain | 领域事件 | 逾期通知 |
| 17 | `domain/event/HoldPlacedEvent.java` | Domain | 领域事件 | 预约已创建 |
| 18 | `domain/event/HoldFulfilledEvent.java` | Domain | 领域事件 | 预约已满足（副本已分配） |
| 19 | `domain/event/HoldPickedUpEvent.java` | Domain | 领域事件 | 预约图书已取 |
| 20 | `domain/event/HoldCancelledEvent.java` | Domain | 领域事件 | 预约已取消 |
| 21 | `domain/event/HoldExpiredEvent.java` | Domain | 领域事件 | 预约等待过期 |
| 22 | `domain/event/HoldExpiredNotPickedUpEvent.java` | Domain | 领域事件 | 预约到书未取过期 |
| 23 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 24 | `domain/exception/LoanNotFoundException.java` | Domain | 异常 | 借阅不存在 |
| 25 | `domain/exception/HoldNotFoundException.java` | Domain | 异常 | 预约不存在 |
| 26 | `domain/exception/LoanRenewalException.java` | Domain | 异常 | 续借失败 |
| 27 | `domain/exception/DuplicateHoldException.java` | Domain | 异常 | 重复预约 |
| 28 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作 |
| 29 | `domain/repository/LoanRepository.java` | Domain | 仓储接口 | 借阅数据访问 |
| 30 | `domain/repository/HoldRepository.java` | Domain | 仓储接口 | 预约数据访问 |
| 31 | `domain/service/CirculationManagementService.java` | Domain | **领域服务** | 流通业务编排（借阅/归还/续借/预约/召回/定时任务） |
| 32 | `application/service/CirculationApplicationService.java` | Application | **应用服务** | 流通用例编排（参数转换 + DTO 转换） |
| 33 | `application/command/BorrowBookCommand.java` | Application | 命令 | 借阅入参 |
| 34 | `application/command/ReturnBookCommand.java` | Application | 命令 | 归还入参 |
| 35 | `application/command/RenewLoanCommand.java` | Application | 命令 | 续借入参 |
| 36 | `application/command/RecallBookCommand.java` | Application | 命令 | 召回入参 |
| 37 | `application/command/PlaceHoldCommand.java` | Application | 命令 | 预约入参 |
| 38 | `application/command/CancelHoldCommand.java` | Application | 命令 | 取消预约入参 |
| 39 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 40 | `application/dto/LoanDTO.java` | Application | DTO | 借阅数据传输 |
| 41 | `application/dto/HoldDTO.java` | Application | DTO | 预约数据传输 |
| 42 | `interfaces/rest/LoanController.java` | Interfaces | REST 控制器 | 借阅 API（7 个端点） |
| 43 | `interfaces/rest/HoldController.java` | Interfaces | REST 控制器 | 预约 API（5 个端点） |
| 44 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：44 个源文件**
