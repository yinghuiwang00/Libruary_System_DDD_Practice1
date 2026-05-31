# Library Shared 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-shared` 模块中每一个源文件的设计意图、DDD 定位和实现细节。

---

## 目录

- [1. 模块概览](#1-模块概览)
- [2. DDD 定位：共享内核（Shared Kernel）](#2-ddd-定位共享内核shared-kernel)
- [3. 领域模型（ID 值对象）](#3-领域模型id-值对象)
  - [3.1 AggregateId —— 抽象基类](#31-aggregateid--抽象基类)
  - [3.2 具体 ID 类型](#32-具体-id-类型)
- [4. 领域模型（通用值对象）](#4-领域模型通用值对象)
  - [4.1 Money —— 金额值对象](#41-money--金额值对象)
  - [4.2 Email —— 邮箱值对象](#42-email--邮箱值对象)
  - [4.3 PhoneNumber —— 电话号码值对象](#43-phonenumber--电话号码值对象)
  - [4.4 Address —— 地址值对象](#44-address--地址值对象)
- [5. 领域事件（基类与发布器）](#5-领域事件基类与发布器)
  - [5.1 DomainEvent —— 领域事件基类](#51-domainevent--领域事件基类)
  - [5.2 DomainEventPublisher —— 领域事件发布器](#52-domaineventpublisher--领域事件发布器)
- [6. 文件清单速查表](#6-文件清单速查表)

---

## 1. 模块概览

`library-shared` 是图书馆管理系统的**共享内核（Shared Kernel）**，为所有其他限界上下文（Bounded Context）提供公共的领域概念和基础设施抽象。

| 职责 | 说明 |
|------|------|
| 强类型 ID | 为每个聚合根定义唯一的标识类型（BookId、AuthorId、LoanId 等） |
| 通用值对象 | Money、Email、PhoneNumber、Address 等跨上下文共享的值对象 |
| 领域事件基类 | 所有上下文的领域事件统一继承 `DomainEvent`，保证事件元数据一致性 |
| 事件发布器 | 封装 Spring `ApplicationEventPublisher`，简化领域事件发布 |
| 跨上下文契约 | ID 类型、值对象和事件基类是各上下文之间通信的"公共语言" |

**不含**：业务逻辑、REST 控制器、数据库访问、Spring Boot 启动类。本模块是纯粹的共享领域概念，不独立运行。

---

## 2. DDD 定位：共享内核（Shared Kernel）

在 DDD 的战略设计中，**共享内核（Shared Kernel）** 是一种限界上下文之间的映射关系。它定义了多个上下文共同依赖的那部分模型——包括领域事件的格式、聚合 ID 的类型等。

```
┌──────────────────┐     ┌──────────────────┐
│  library-catalog │     │ library-circulation│
│  (编目上下文)     │     │  (借阅上下文)       │
│                  │     │                    │
│  使用 BookId     │     │  使用 BookId       │
│  使用 DomainEvent│     │  使用 DomainEvent  │
└────────┬─────────┘     └────────┬──────────┘
         │                        │
         │    ┌───────────────┐   │
         └───→│ library-shared │←──┘
              │  (共享内核)    │
              │               │
              │  BookId       │
              │  AuthorId     │
              │  DomainEvent  │
              │  ...          │
              └───────────────┘
```

**共享内核的设计原则：**

1. **精简**：只放各上下文真正需要共享的内容，避免膨胀为"公共 Utils 模块"
2. **稳定**：共享内核的变更会影响所有依赖方，因此接口必须稳定
3. **无业务逻辑**：不包含任何特定上下文的业务规则
4. **类型安全**：通过强类型 ID 避免"String 到处传"的类型混淆问题

---

## 3. 领域模型（ID 值对象）

> 所有 ID 类型都位于 `com.library.shared.domain.model` 包下，作为 JPA `@Embeddable` 值对象嵌入到各上下文的聚合根中。

### 3.1 AggregateId —— 抽象基类

**DDD 角色**：**值对象（Value Object）基类**

```java
@Embeddable
@MappedSuperclass
public abstract class AggregateId implements Serializable, Comparable<AggregateId> {

    @Column(name = "id", nullable = false)
    private String value;

    protected AggregateId() {}

    protected AggregateId(String value) {
        this.value = Objects.requireNonNull(value, "ID value must not be null");
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateId that = (AggregateId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public int compareTo(AggregateId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() { return value; }
}
```

**设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| `abstract` | 抽象类，不能直接实例化 | 强制每个聚合根定义自己的 ID 类型 |
| `@Embeddable` | JPA 可嵌入对象 | ID 作为值对象嵌入聚合根，不创建独立的数据库表 |
| `@MappedSuperclass` | JPA 映射超类 | 子类继承 `value` 字段的 JPA 映射配置 |
| `protected` 构造器 | 外部不能直接 `new AggregateId(...)` | 只有子类能调用父类构造器 |
| `generateUUID()` | 静态方法生成随机 UUID | 子类通过 `generate()` 工厂方法调用此方法 |
| `equals`/`hashCode` | 基于 `value` 字段 | 值对象相等性由内部值决定，而非对象引用 |
| `Comparable` | 实现 `compareTo` | 支持排序和 TreeSet/TreeMap 等有序集合 |
| `Serializable` | 实现 Java 序列化 | JPA 要求嵌入 ID 类型可序列化 |

**为什么不用 `Long` 或 `String` 作为 ID？**

在 DDD 中，使用裸标量类型（如 `String bookId`）作为聚合标识存在以下问题：

- **类型混淆**：`String bookId` 和 `String authorId` 是同一类型，编译器无法防止参数传反
- **缺乏行为**：裸 String 没有工厂方法、没有 equals 封装、没有验证逻辑
- **语义不清**：看到 `String id` 不知道是哪种实体的 ID

通过 `BookId`、`AuthorId` 等强类型，以上问题全部解决：

```java
// 错误：类型混淆，编译通过但运行时出错
void findBook(String authorId) { ... }  // 本该传 bookId

// 正确：类型安全，编译时即可发现错误
void findBook(BookId bookId) { ... }
findBook(AuthorId.generate());  // 编译错误！
```

---

### 3.2 具体 ID 类型

所有具体 ID 类型都遵循相同的模板，以 `BookId` 为例：

```java
@Embeddable
public class BookId extends AggregateId {

    protected BookId() {}          // JPA 无参构造器

    private BookId(String value) { // 私有构造器
        super(value);
    }

    public static BookId generate() {  // 生成新 ID
        return new BookId(generateUUID());
    }

    public static BookId of(String value) {  // 从已有值重建
        return new BookId(value);
    }
}
```

**两种工厂方法的用途：**

| 方法 | 用途 | 场景 |
|------|------|------|
| `generate()` | 生成全新的 UUID | 创建新聚合根时调用 |
| `of(String)` | 从已有字符串重建 ID | 从数据库加载、从 API 参数解析、从事件中恢复 |

**全部 ID 类型一览：**

| ID 类型 | 对应聚合根 | 所属上下文 | 说明 |
|---------|-----------|-----------|------|
| `BookId` | Book | library-catalog | 图书标识 |
| `AuthorId` | Author | library-catalog | 作者标识 |
| `PublisherId` | Publisher | library-catalog | 出版社标识 |
| `CategoryId` | Category | library-catalog | 分类标识 |
| `CopyId` | BookCopy | library-circulation | 图书副本标识 |
| `CopyInventoryId` | CopyInventory | library-inventory | 库存副本标识 |
| `LibraryId` | Library/Branch | library-inventory | 图书馆/分馆标识 |
| `LoanId` | Loan | library-circulation | 借阅记录标识 |
| `HoldId` | Hold/Reservation | library-circulation | 预约记录标识 |
| `PatronId` | Patron | library-patron | 读者标识 |
| `FineId` | Fine | library-payment | 罚款标识 |
| `PaymentId` | Payment | library-payment | 支付记录标识 |
| `RefundId` | Refund | library-payment | 退款标识 |
| `NotificationId` | Notification | library-notification | 通知标识 |
| `DashboardId` | Dashboard | library-analytics | 仪表盘标识 |
| `ReportId` | Report | library-analytics | 报表标识 |

**DDD 意义**：每个聚合根拥有独立的 ID 类型，体现了 DDD 中"同一性（Identity）"的概念——不同聚合根的标识是不可互换的，即使它们的内部表示（UUID 字符串）格式相同。

---

## 4. 领域模型（通用值对象）

> 通用值对象位于 `com.library.shared.domain.model` 包下，与 ID 值对象同包。它们封装了多个限界上下文共享的业务概念，确保验证逻辑和业务规则的一致性。

### 4.1 Money —— 金额值对象

**DDD 角色**：**值对象（Value Object）**

```java
@Embeddable
public class Money implements Comparable<Money> {
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "currency", length = 3)
    private String currency;

    public Money(BigDecimal amount, String currency) { ... }
    public Money(BigDecimal amount) { this(amount, "CNY"); }  // 默认人民币

    // 运算（返回新实例，不可变）
    public Money add(Money other) { ... }
    public Money subtract(Money other) { ... }
    public Money multiply(int factor) { ... }
    public Money negate() { ... }

    // 比较
    public boolean isPositive() { ... }
    public boolean isNegative() { ... }
    public boolean isZero() { ... }
    public boolean isGreaterThan(Money other) { ... }
}
```

**设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| `@Embeddable` | JPA 可嵌入对象 | 金额作为值对象嵌入聚合根（如 Payment 的 amount 字段） |
| 不可变 | 所有运算返回新 `Money` 实例 | 值对象的核心特性——相等性由属性决定，不可修改 |
| 货币类型 | `currency` 字段（3 字母 ISO 代码） | 防止不同货币的金额混淆运算 |
| `BigDecimal` | 精确小数运算 | 避免浮点数精度丢失，金额计算必须精确 |
| 默认 CNY | 人民币为默认货币 | 项目场景是中国图书馆系统 |
| 跨货币保护 | `add()`/`subtract()` 检查货币一致性 | 防止 CNY + USD 这样的非法运算 |

**使用场景**：Payment 上下文的支付金额、Patron 上下文的罚金、Analytics 上下文的财务报表。

---

### 4.2 Email —— 邮箱值对象

**DDD 角色**：**值对象（Value Object）**

```java
@Embeddable
public class Email {
    @Column(name = "email", length = 200)
    private String value;

    public Email(String value) {
        String normalized = value.trim().toLowerCase();  // 自动规范化
        // 正则验证格式...
        this.value = normalized;
    }

    public String getDomain() { ... }      // 提取域名
    public String getLocalPart() { ... }   // 提取本地部分
}
```

**设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 自动规范化 | `trim()` + `toLowerCase()` | 两个语义相同的邮箱（`User@Example.COM` 和 `user@example.com`）被视为相等 |
| 格式验证 | 正则表达式 `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | 确保进入系统的邮箱都是合法的 |
| `equals` 基于值 | `value` 字段 | 值对象相等性——`Email("A@B.COM") == Email("a@b.com")` |
| 零依赖 | 不依赖外部邮箱验证 API | 共享内核必须精简，不做外部调用 |

**使用场景**：Patron 上下文的会员邮箱、Notification 上下文的收件人邮箱。

---

### 4.3 PhoneNumber —— 电话号码值对象

**DDD 角色**：**值对象（Value Object）**

```java
@Embeddable
public class PhoneNumber {
    @Column(name = "phone", length = 20)
    private String value;

    public PhoneNumber(String value) {
        String digits = value.replaceAll("[\\s\\-()]", "");  // 去除格式符号
        // 验证 7-15 位数字，可选 + 前缀...
        this.value = digits;
    }

    public boolean isInternational() { return value.startsWith("+"); }
}
```

**设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 格式剥离 | 去除空格、横线、括号 | `138-0013-8000` 和 `13800138000` 被视为相同号码 |
| 国际号码支持 | `+` 前缀，7-15 位数字 | 兼容 `+8613800138000` 等国际格式 |
| 国内号码 | 纯数字 7-15 位 | 兼容 `13800138000`、`01012345678` 等 |

**使用场景**：Patron 上下文的会员电话、Notification 上下文的 SMS 收件人。

---

### 4.4 Address —— 地址值对象

**DDD 角色**：**值对象（Value Object）**

```java
@Embeddable
public class Address {
    @Column(name = "street", length = 300)   private String street;
    @Column(name = "city", length = 100)     private String city;        // 必填
    @Column(name = "postal_code", length = 20) private String postalCode;
    @Column(name = "state", length = 100)    private String state;
    @Column(name = "country", length = 100)  private String country;    // 默认 "China"

    public Address(String street, String city, String postalCode, String state, String country) { ... }
    public String getFullAddress() { ... }  // 拼接完整地址字符串
}
```

**设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| city 必填 | `Objects.requireNonNull` | 地址至少需要城市信息 |
| 其他字段可选 | street/postalCode/state 可为 null | 地址的详细程度因场景而异 |
| 默认 country | "China" | 项目场景是中国图书馆系统 |
| `getFullAddress()` | 智能拼接，跳过 null 字段 | 方便显示和日志 |
| 自动 trim | 所有字符串字段去除首尾空格 | 数据规范化 |

**使用场景**：Patron 上下文的会员地址、Inventory 上下文的图书馆/分馆地址。

---

### 通用值对象的 DDD 意义

将 Money、Email、PhoneNumber、Address 定义在共享模块而非各上下文内部，遵循以下 DDD 原则：

1. **消除重复**：Patron、Notification、Payment 等多个上下文都需要邮箱概念，共享一处定义避免验证逻辑不一致
2. **类型安全**：`Email email` 比 `String email` 更安全——编译器阻止传入任意字符串
3. **不变性（Immutability）**：所有值对象都是不可变的，符合 DDD 值对象的定义
4. **自验证（Self-validating）**：构造器中验证格式，确保系统中的值对象始终处于合法状态
5. **`@Embeddable`**：作为 JPA 嵌入对象，不创建独立的数据库表，直接嵌入聚合根的表中

```
library-patron                          library-notification
  │                                        │
  │  Email (shared)                        │  Email (shared)
  │  ───→ "user@example.com"               │  ───→ "user@example.com"
  │  patron.email                          │  notification.recipientEmail
  │                                        │
  │  验证逻辑一致 ✓                         │  验证逻辑一致 ✓
```

---

## 5. 领域事件（基类与发布器）

> 领域事件相关的代码位于 `com.library.shared.domain.event` 包下。

### 5.1 DomainEvent —— 领域事件基类

**DDD 角色**：**领域事件（Domain Event）基类**

```java
public abstract class DomainEvent implements Serializable {

    private final String eventId;          // 事件唯一标识
    private final LocalDateTime occurredAt; // 事件发生时间
    private final String eventType;        // 事件类型（自动取子类类名）
    private final int version;             // 事件版本号

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.version = 1;
    }

    protected DomainEvent(String eventId, LocalDateTime occurredAt, int version) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.eventType = this.getClass().getSimpleName();
        this.version = version;
    }
}
```

**设计要点：**

| 设计点 | 说明 | DDD 意义 |
|--------|------|----------|
| `abstract` | 不能直接实例化 | 所有领域事件必须继承此类并携带业务数据 |
| `eventId` | UUID 自动生成 | 每个事件有全局唯一标识，支持幂等处理和事件溯源 |
| `occurredAt` | 事件发生时间 | 事件是"已经发生的事"，记录发生时刻 |
| `eventType` | `getClass().getSimpleName()` | 自动获取子类类名（如 `BookCreatedEvent`），方便日志和路由 |
| `version` | 默认为 1 | 支持事件版本演进——当事件结构变更时递增版本号 |
| 无参构造器 | 自动生成所有元数据 | 子类只需 `super()` 即可，简化事件创建 |
| 全参构造器 | 手动指定所有字段 | 用于事件溯源重建（从存储中恢复事件时使用） |
| `equals`/`hashCode` | 基于 `eventId` | 事件的唯一性由 eventId 决定 |
| `Serializable` | Java 序列化 | 支持通过 Kafka 等消息中间件传输 |

**事件在上下文间的作用：**

```
library-catalog                        library-inventory
  │                                      │
  │  BookCreatedEvent {bookId, isbn}     │
  │─────────────────────────────────────→│  收到事件，为新书创建库存副本
  │                                      │
```

领域事件是 DDD 中限界上下文之间通信的核心机制。共享模块定义事件基类，确保所有上下文的事件格式一致。

---

### 5.2 DomainEventPublisher —— 领域事件发布器

**DDD 角色**：**领域事件发布服务（Domain Event Publisher）**

```java
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| `@Component` | Spring 管理的 Bean，被各上下文的领域服务注入使用 |
| 封装 Spring | 将 `ApplicationEventPublisher` 封装为领域层的发布器接口，避免领域层直接依赖 Spring |
| `publish()` | 发布单个事件 |
| `publishAll()` | 批量发布事件（聚合根可能产生多个事件） |

**为什么定义在 shared 模块？**

`DomainEventPublisher` 是所有上下文都需要的公共基础设施。如果每个上下文各定义一套，会导致：
- 重复代码
- 发布机制不一致

将它放在共享内核中，确保所有上下文使用统一的事件发布方式。各上下文的领域服务只需注入 `DomainEventPublisher` 即可发布事件：

```java
@Service
public class BookManagementService {
    private final DomainEventPublisher eventPublisher;  // 来自 shared 模块

    public Book createBook(...) {
        // ... 创建图书 ...
        eventPublisher.publish(new BookCreatedEvent(...));
    }
}
```

**DDD 层次关系**：虽然 `DomainEventPublisher` 使用了 Spring 框架（依赖倒置），但它的抽象层面属于领域层——发布领域事件是领域逻辑的一部分。这是 DDD 中"领域层依赖基础设施接口"的合理做法。

---

## 6. 文件清单速查表

| # | 文件路径 | DDD 角色 | 核心职责 |
|---|---------|----------|----------|
| 1 | `domain/model/AggregateId.java` | **值对象基类** | 抽象 ID 基类，提供 UUID 生成、相等性、比较 |
| 2 | `domain/model/BookId.java` | 值对象 | 图书聚合根标识 |
| 3 | `domain/model/AuthorId.java` | 值对象 | 作者聚合根标识 |
| 4 | `domain/model/PublisherId.java` | 值对象 | 出版社聚合根标识 |
| 5 | `domain/model/CategoryId.java` | 值对象 | 分类聚合根标识 |
| 6 | `domain/model/CopyId.java` | 值对象 | 图书副本标识 |
| 7 | `domain/model/CopyInventoryId.java` | 值对象 | 库存副本标识 |
| 8 | `domain/model/LibraryId.java` | 值对象 | 图书馆/分馆标识 |
| 9 | `domain/model/LoanId.java` | 值对象 | 借阅记录标识 |
| 10 | `domain/model/HoldId.java` | 值对象 | 预约记录标识 |
| 11 | `domain/model/PatronId.java` | 值对象 | 读者标识 |
| 12 | `domain/model/FineId.java` | 值对象 | 罚款标识 |
| 13 | `domain/model/PaymentId.java` | 值对象 | 支付记录标识 |
| 14 | `domain/model/RefundId.java` | 值对象 | 退款标识 |
| 15 | `domain/model/NotificationId.java` | 值对象 | 通知标识 |
| 16 | `domain/model/DashboardId.java` | 值对象 | 仪表盘标识 |
| 17 | `domain/model/ReportId.java` | 值对象 | 报表标识 |
| 18 | `domain/event/DomainEvent.java` | **领域事件基类** | 事件元数据（eventId、occurredAt、eventType、version） |
| 19 | `domain/event/DomainEventPublisher.java` | **事件发布服务** | 封装 Spring 事件发布，统一发布机制 |
| 20 | `domain/model/Money.java` | **值对象** | 金额（amount + currency），不可变，含算术和比较运算 |
| 21 | `domain/model/Email.java` | **值对象** | 邮箱（验证 + normalize + toLowerCase） |
| 22 | `domain/model/PhoneNumber.java` | **值对象** | 电话号码（格式剥离 + 国际号码支持） |
| 23 | `domain/model/Address.java` | **值对象** | 地址（street/city/postalCode/state/country） |

**总计：23 个源文件**

**包结构总览：**

```
library-shared/src/main/java/com/library/shared/
└── domain/
    ├── model/                              ← 领域模型（ID 值对象 + 通用值对象）
    │   ├── AggregateId.java                ← ID 抽象基类
    │   ├── BookId.java                     ← 以下 16 个具体 ID 类型
    │   ├── AuthorId.java
    │   ├── PublisherId.java
    │   ├── CategoryId.java
    │   ├── CopyId.java
    │   ├── CopyInventoryId.java
    │   ├── LibraryId.java
    │   ├── LoanId.java
    │   ├── HoldId.java
    │   ├── PatronId.java
    │   ├── FineId.java
    │   ├── PaymentId.java
    │   ├── RefundId.java
    │   ├── NotificationId.java
    │   ├── DashboardId.java
    │   └── ReportId.java
    │   ├── Money.java                      ← 金额值对象（BigDecimal + currency）
    │   ├── Email.java                      ← 邮箱值对象（验证 + 规范化）
    │   ├── PhoneNumber.java                ← 电话号码值对象（格式验证）
    │   └── Address.java                    ← 地址值对象（street/city/postalCode）
    └── event/                              ← 领域事件
        ├── DomainEvent.java                ← 事件基类
        └── DomainEventPublisher.java       ← 事件发布器
```
