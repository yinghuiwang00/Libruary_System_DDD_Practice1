# Library Notification 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-notification` 模块中每一个源文件的设计意图、DDD 定位和实现细节。

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
- [6. 接口层（Interfaces Layer）](#6-接口层interfaces-layer)
- [7. 层间调用流程](#7-层间调用流程)
- [8. 文件清单速查表](#8-文件清单速查表)

---

## 1. 模块概览

`library-notification` 是图书馆管理系统的**通知上下文（Notification Context）**，负责向读者发送各类通知消息，支持多渠道投递和全生命周期管理。

| 职责 | 说明 |
|------|------|
| 通知创建 | 创建各类通知（到期提醒、逾期通知、预约可取、罚款通知等） |
| 多渠道投递 | 支持 EMAIL、SMS、PUSH、IN_APP 四种通知渠道 |
| 生命周期管理 | 通知的调度、发送、确认送达、已读、失败、重试、取消 |
| 优先级管理 | LOW / NORMAL / HIGH / URGENT 四级优先级 |
| 失败重试 | 通知发送失败后支持自动/手动重试（最多 3 次） |
| 领域事件 | 通知状态变更时发布事件通知其他上下文 |

运行端口：`8087`，基础路径：`/api/notifications/*`

---

## 2. DDD 分层架构总览

```
library-notification/src/main/java/com/library/notification/
│
├── NotificationApplication.java          ← Spring Boot 启动类
├── config/                               ← 基础设施配置
│   └── JpaConfig.java
│
├── domain/                               ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/
│   │   ├── Notification.java             ← 聚合根：通知
│   │   └── enums/                        ← 枚举
│   │       ├── NotificationType.java      ← 通知类型（9种业务场景）
│   │       ├── NotificationChannel.java   ← 通知渠道（4种）
│   │       ├── NotificationPriority.java  ← 通知优先级（4级）
│   │       └── NotificationStatus.java    ← 通知状态（7种）
│   ├── event/                            ← 领域事件
│   │   ├── NotificationCreatedEvent.java
│   │   ├── NotificationDeliveredEvent.java
│   │   ├── NotificationFailedEvent.java
│   │   └── NotificationReadEvent.java
│   ├── exception/                        ← 领域异常
│   │   ├── DomainException.java          ← 异常基类
│   │   ├── NotificationNotFoundException.java
│   │   └── InvalidOperationException.java
│   ├── repository/                       ← 仓储接口
│   │   └── NotificationRepository.java
│   └── service/                          ← 领域服务
│       └── NotificationService.java
│
├── application/                          ← 应用层（编排、协调）
│   ├── service/
│   │   └── NotificationApplicationService.java
│   ├── command/                          ← 命令对象（8个）
│   │   ├── CreateNotificationCommand.java
│   │   ├── ScheduleNotificationCommand.java
│   │   ├── SendNotificationCommand.java
│   │   ├── MarkDeliveredCommand.java
│   │   ├── MarkReadCommand.java
│   │   ├── FailNotificationCommand.java
│   │   ├── RetryNotificationCommand.java
│   │   └── CancelNotificationCommand.java
│   └── dto/                              ← 数据传输对象
│       ├── ApiResponse.java
│       └── NotificationDTO.java
│
└── interfaces/                           ← 接口层（REST API）
    └── rest/
        ├── NotificationController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces → application → domain ← infrastructure
                                  （实现 domain 的 repository 接口）
```

---

## 3. 引导与配置层

### `NotificationApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.notification", "com.library.shared"})
@EnableJpaRepositories
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类。`scanBasePackages` 同时扫描通知上下文和共享内核包，使得 `NotificationId`、`DomainEvent`、`DomainEventPublisher` 等共享组件可用。

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：启用 JPA 审计功能，让 `@CreatedDate`、`@LastModifiedDate` 注解自动填充时间戳。

---

## 4. 领域层（Domain Layer）

### 4.1 领域模型（domain/model）

#### 4.1.1 `Notification.java` —— 聚合根：通知

**DDD 角色**：**聚合根（Aggregate Root）**

```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_type", columnList = "notification_type")
})
public class Notification {

    @EmbeddedId
    private NotificationId id;           // 共享模块的强类型 ID

    private NotificationType notificationType;  // 通知类型
    private NotificationPriority priority;      // 优先级
    private NotificationChannel channel;        // 投递渠道

    private String recipientId;          // 收件人 ID
    private String recipientEmail;       // 收件人邮箱
    private String recipientPhone;       // 收件人电话

    private String subject;              // 通知标题
    private String content;              // 通知内容

    private NotificationStatus status;   // 当前状态

    private LocalDateTime scheduledAt;   // 计划发送时间
    private LocalDateTime sentAt;        // 实际发送时间
    private LocalDateTime deliveredAt;   // 送达时间
    private LocalDateTime readAt;        // 已读时间
    private LocalDateTime failedAt;      // 失败时间

    private String failureReason;        // 失败原因
    private Integer retryCount;          // 已重试次数
    private Integer maxRetries;          // 最大重试次数（默认3）

    @Version
    private Long version;                // 乐观锁

    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId NotificationId id` | 使用共享模块的强类型 ID |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突 |
| 构造控制 | `protected` + `private` 构造器 | JPA 要求无参构造器，但防止外部直接实例化 |
| 工厂方法 | `Notification.create(...)` | 静态工厂方法，确保创建时总是生成新 ID 和正确的初始状态 |
| 状态守卫 | 每个状态变更方法校验当前状态 | 业务规则编码在聚合根内，保证聚合不变量 |
| 数据库索引 | 三个 `@Index` | 针对 recipientId、status、type 建索引，优化查询性能 |

**状态机：通知生命周期**

```
                          ┌──cancel()──→ CANCELLED
                          │
PENDING ──schedule()──→ SCHEDULED ──send()──→ SENDING ──markDelivered()──→ DELIVERED ──markRead()──→ READ
  │                                              │
  └──send()──→ SENDING                            └──fail()──→ FAILED ──retry()──→ PENDING（循环）
```

**业务规则（编码在聚合根中）：**

- `schedule()`：只有 PENDING 状态才能调度
- `send()`：只有 PENDING 或 SCHEDULED 状态才能发送
- `markDelivered()`：只有 SENDING 状态才能标记为已送达
- `markRead()`：只有 DELIVERED 状态才能标记为已读
- `fail()`：只有 SENDING 状态才能标记为失败
- `retry()`：只有 FAILED 状态且未超过最大重试次数才能重试
- `cancel()`：终态（DELIVERED/READ/CANCELLED）不能取消

---

#### 4.1.2 枚举类型

**`NotificationType`** —— 通知类型

```java
public enum NotificationType {
    DUE_DATE_REMINDER,       // 到期提醒
    OVERDUE_NOTICE,          // 逾期通知
    HOLD_AVAILABLE,          // 预约可取
    HOLD_CANCELLED,          // 预约取消
    FINE_NOTIFICATION,       // 罚款通知
    PAYMENT_CONFIRMATION,    // 支付确认
    BOOK_RETURNED,           // 图书归还确认
    MEMBERSHIP_RENEWAL,      // 会员续期
    SYSTEM_ANNOUNCEMENT      // 系统公告
}
```

**`NotificationChannel`** —— 通知渠道

```java
public enum NotificationChannel {
    EMAIL,    // 邮件
    SMS,      // 短信
    PUSH,     // 推送通知
    IN_APP    // 站内消息
}
```

**`NotificationPriority`** —— 通知优先级

```java
public enum NotificationPriority {
    LOW,     // 低优先级
    NORMAL,  // 普通（默认）
    HIGH,    // 高优先级
    URGENT   // 紧急
}
```

**`NotificationStatus`** —— 通知状态

```java
public enum NotificationStatus {
    PENDING,    // 待发送
    SCHEDULED,  // 已调度
    SENDING,    // 发送中
    DELIVERED,  // 已送达
    READ,       // 已读
    FAILED,     // 发送失败
    CANCELLED   // 已取消
}
```

---

### 4.2 领域事件（domain/event）

所有领域事件继承自 `library-shared` 中的 `DomainEvent` 基类。

| 事件类 | 携带数据 | 触发时机 |
|--------|----------|----------|
| `NotificationCreatedEvent` | notificationId, notificationType, recipientId | 通知创建成功后 |
| `NotificationDeliveredEvent` | notificationId, notificationType, channel | 通知送达确认后 |
| `NotificationFailedEvent` | notificationId, reason | 通知发送失败后 |
| `NotificationReadEvent` | notificationId | 通知标记已读后 |

以 `NotificationCreatedEvent` 为例：

```java
public class NotificationCreatedEvent extends DomainEvent {
    private final NotificationId notificationId;
    private final NotificationType notificationType;
    private final String recipientId;
}
```

---

### 4.3 领域异常（domain/exception）

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `DomainException` | （基类） | 所有领域异常的父类 | 400 |
| `NotificationNotFoundException` | `NOTIFICATION_NOT_FOUND` | 通过 ID 查找通知不存在 | 404 |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作（如在终态取消） | 409 |

---

### 4.4 仓储接口（domain/repository）

#### `NotificationRepository.java`

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, NotificationId> {
    List<Notification> findByRecipientId(String recipientId);
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByNotificationType(NotificationType notificationType);
}
```

**DDD 定位**：领域层定义的仓储接口，由 Spring Data JPA 自动实现。提供按收件人、状态、类型查询的能力。

---

### 4.5 领域服务（domain/service）

#### `NotificationService.java`

**DDD 角色**：**领域服务** —— 通知聚合的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final DomainEventPublisher eventPublisher;
}
```

**职责：**

| 方法 | 业务逻辑 |
|------|----------|
| `createNotification()` | 创建通知 → 保存 → 发布 NotificationCreatedEvent |
| `scheduleNotification()` | 查找通知 → 调用 schedule() → 保存 |
| `sendNotification()` | 查找通知 → 调用 send() → 保存 |
| `markDelivered()` | 查找通知 → 调用 markDelivered() → 保存 → 发布 NotificationDeliveredEvent |
| `markRead()` | 查找通知 → 调用 markRead() → 保存 → 发布 NotificationReadEvent |
| `failNotification()` | 查找通知 → 调用 fail() → 保存 → 发布 NotificationFailedEvent |
| `retryNotification()` | 查找通知 → 调用 retry()（含重试次数校验）→ 保存 |
| `cancelNotification()` | 查找通知 → 调用 cancel()（含终态校验）→ 保存 |

**DDD 设计要点：**

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写操作方法覆盖为 `@Transactional`
2. **事件发布**：关键状态变更后发布领域事件，通知其他上下文
3. **保护聚合不变量**：业务规则（状态校验、重试次数限制）编码在 Notification 实体中

---

## 5. 应用层（Application Layer）

### `NotificationApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

应用服务在领域对象和外部表示之间做转换——接收 Command 对象（String 类型的外部参数），调用领域服务（强类型的领域对象），返回 DTO。

```java
@Service
@Transactional
public class NotificationApplicationService {
    private final NotificationService notificationService;
}
```

**类型转换示例：**

```java
public NotificationDTO createNotification(CreateNotificationCommand command) {
    Notification notification = notificationService.createNotification(
        NotificationType.valueOf(command.getNotificationType()),  // String → 枚举
        NotificationChannel.valueOf(command.getChannel()),        // String → 枚举
        command.getRecipientId(),                                 // String 直接传递
        ...
    );
    return NotificationDTO.from(notification);  // Notification → DTO
}
```

### 命令对象（application/command）

8 个命令对象，每个对应一个写操作：

| 命令类 | 用途 | 必填字段 |
|--------|------|----------|
| `CreateNotificationCommand` | 创建通知 | notificationType, channel, recipientId, subject, content |
| `ScheduleNotificationCommand` | 调度通知 | notificationId, scheduledAt |
| `SendNotificationCommand` | 发送通知 | notificationId |
| `MarkDeliveredCommand` | 标记送达 | notificationId |
| `MarkReadCommand` | 标记已读 | notificationId |
| `FailNotificationCommand` | 标记失败 | notificationId, reason |
| `RetryNotificationCommand` | 重试发送 | notificationId |
| `CancelNotificationCommand` | 取消通知 | notificationId, reason |

### DTO（application/dto）

- **`NotificationDTO`**：通知的完整数据传输对象，包含 `from(Notification)` 静态方法将聚合根转换为扁平的 DTO
- **`ApiResponse<T>`**：统一响应信封，包含 success、data、errorCode、errorMessage、timestamp

---

## 6. 接口层（Interfaces Layer）

### `NotificationController.java`

```java
@RestController
@RequestMapping("/api/notifications")
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 |
|-----------|------|------|
| `POST` | `/api/notifications` | 创建通知 |
| `GET` | `/api/notifications` | 查询通知（支持 recipientId/status/type 过滤） |
| `GET` | `/api/notifications/{id}` | 获取单条通知 |
| `POST` | `/api/notifications/{id}/schedule` | 调度通知 |
| `POST` | `/api/notifications/{id}/send` | 发送通知 |
| `POST` | `/api/notifications/{id}/deliver` | 标记已送达 |
| `PUT` | `/api/notifications/{id}/read` | 标记已读 |
| `POST` | `/api/notifications/{id}/fail` | 标记发送失败 |
| `POST` | `/api/notifications/{id}/retry` | 重试发送 |
| `POST` | `/api/notifications/{id}/cancel` | 取消通知 |

### `GlobalExceptionHandler.java`

统一异常处理，将领域异常映射为 HTTP 状态码：
- `NotificationNotFoundException` → 404
- `InvalidOperationException` → 409 Conflict
- `DomainException` → 400
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400（含字段级错误信息）
- 其他异常 → 500

---

## 7. 层间调用流程

以 **"创建并发送通知"** 为例：

```
HTTP POST /api/notifications
  │
  ▼
┌──────────────────────────────────────────────────────────┐
│ NotificationController.createNotification(command)        │  ← interfaces 层
│   @Valid 触发 Bean Validation                             │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ NotificationApplicationService.createNotification(cmd)    │  ← application 层
│   1. NotificationType.valueOf(cmd.getNotificationType())  │
│   2. NotificationChannel.valueOf(cmd.getChannel())        │
│   3. 调用 notificationService.createNotification(...)     │
│   4. NotificationDTO.from(notification)                   │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ NotificationService.createNotification(...)               │  ← domain 层
│   1. Notification.create(type, channel, ...)              │
│   2. notificationRepository.save(notification)            │
│   3. eventPublisher.publish(NotificationCreatedEvent)     │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ DomainEventPublisher → Spring ApplicationEventPublisher   │  ← shared 模块
│ PostgreSQL: INSERT INTO notifications (...)               │  ← 数据库
└──────────────────────────────────────────────────────────┘
```

---

## 8. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `NotificationApplication.java` | Bootstrap | 启动类 | Spring Boot 入口 |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/Notification.java` | Domain | **聚合根** | 通知全生命周期管理 |
| 4 | `domain/model/enums/NotificationType.java` | Domain | 枚举 | 通知业务类型（9种） |
| 5 | `domain/model/enums/NotificationChannel.java` | Domain | 枚举 | 通知投递渠道（4种） |
| 6 | `domain/model/enums/NotificationPriority.java` | Domain | 枚举 | 通知优先级（4级） |
| 7 | `domain/model/enums/NotificationStatus.java` | Domain | 枚举 | 通知状态（7种） |
| 8 | `domain/event/NotificationCreatedEvent.java` | Domain | 领域事件 | 通知已创建 |
| 9 | `domain/event/NotificationDeliveredEvent.java` | Domain | 领域事件 | 通知已送达 |
| 10 | `domain/event/NotificationFailedEvent.java` | Domain | 领域事件 | 通知发送失败 |
| 11 | `domain/event/NotificationReadEvent.java` | Domain | 领域事件 | 通知已读 |
| 12 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 13 | `domain/exception/NotificationNotFoundException.java` | Domain | 异常 | 通知不存在 |
| 14 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作 |
| 15 | `domain/repository/NotificationRepository.java` | Domain | 仓储接口 | 通知数据访问 |
| 16 | `domain/service/NotificationService.java` | Domain | **领域服务** | 通知业务编排 |
| 17 | `application/service/NotificationApplicationService.java` | Application | **应用服务** | 通知用例编排 |
| 18 | `application/command/CreateNotificationCommand.java` | Application | 命令 | 创建通知入参 |
| 19 | `application/command/ScheduleNotificationCommand.java` | Application | 命令 | 调度通知入参 |
| 20 | `application/command/SendNotificationCommand.java` | Application | 命令 | 发送通知入参 |
| 21 | `application/command/MarkDeliveredCommand.java` | Application | 命令 | 标记送达入参 |
| 22 | `application/command/MarkReadCommand.java` | Application | 命令 | 标记已读入参 |
| 23 | `application/command/FailNotificationCommand.java` | Application | 命令 | 标记失败入参 |
| 24 | `application/command/RetryNotificationCommand.java` | Application | 命令 | 重试发送入参 |
| 25 | `application/command/CancelNotificationCommand.java` | Application | 命令 | 取消通知入参 |
| 26 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 27 | `application/dto/NotificationDTO.java` | Application | DTO | 通知数据传输 |
| 28 | `interfaces/rest/NotificationController.java` | Interfaces | REST 控制器 | 通知 API |
| 29 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：29 个源文件**
