# Library Patron 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-patron` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

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

`library-patron` 是图书馆管理系统的**读者上下文（Patron Context）**，负责管理图书馆读者的完整生命周期：

| 职责 | 说明 |
|------|------|
| 读者注册 | 创建新的读者账户，按类型（学生/教师/职工/校友/社区）初始化借阅权限 |
| 个人信息管理 | 姓名、邮箱、电话、地址等联系信息的更新 |
| 会员状态管理 | 激活（ACTIVE）、停用（SUSPENDED）、终止（TERMINATED）三态转换 |
| 借阅权限控制 | 基于读者类型的借阅上限、借期、续借次数、罚款费率等配置 |
| 罚款管理 | 罚款产生、缴纳、豁免，以及罚款超限自动停用 |
| 会员续期 | 延长会员有效期，批量处理过期会员 |
| 读者类型变更 | 类型变更时自动重新计算借阅权限参数 |
| 领域事件 | 状态变更时发布事件通知其他上下文（如流通上下文） |

运行端口：`8084`，基础路径：`/api/patrons/*`

---

## 2. DDD 分层架构总览

```
library-patron/src/main/java/com/library/patron/
│
├── PatronApplication.java                ← Spring Boot 启动类
├── config/                               ← 基础设施配置
│   └── JpaConfig.java
│
├── domain/                               ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                            ← 聚合根、值对象、枚举
│   │   ├── Patron.java                   ← 聚合根：读者
│   │   ├── BorrowingPrivilege.java       ← 值对象：借阅权限
│   │   └── enums/                        ← 枚举
│   │       ├── MembershipStatus.java
│   │       └── PatronType.java
│   ├── event/                            ← 领域事件
│   │   ├── PatronRegisteredEvent.java
│   │   ├── PatronUpdatedEvent.java
│   │   ├── PatronSuspendedEvent.java
│   │   ├── PatronReactivatedEvent.java
│   │   ├── PatronTerminatedEvent.java
│   │   └── PatronTypeChangedEvent.java
│   ├── exception/                        ← 领域异常（含错误码）
│   │   ├── DomainException.java          ← 异常基类
│   │   ├── PatronNotFoundException.java
│   │   ├── DuplicateEmailException.java
│   │   ├── InvalidOperationException.java
│   │   └── PatronCannotBorrowException.java
│   ├── repository/                       ← 仓储接口（由 Spring Data JPA 自动实现）
│   │   └── PatronRepository.java
│   └── service/                          ← 领域服务
│       └── PatronManagementService.java
│
├── application/                          ← 应用层（编排、协调）
│   ├── service/
│   │   └── PatronApplicationService.java ← 应用服务：编排 Patron 用例
│   ├── command/                          ← 写操作入参
│   │   ├── RegisterPatronCommand.java
│   │   ├── UpdatePatronCommand.java
│   │   ├── SuspendPatronCommand.java
│   │   ├── ReactivatePatronCommand.java
│   │   ├── TerminatePatronCommand.java
│   │   ├── ExtendMembershipCommand.java
│   │   ├── AddFineCommand.java
│   │   ├── PayFineCommand.java
│   │   ├── WaiveFineCommand.java
│   │   └── ChangePatronTypeCommand.java
│   └── dto/                              ← 输出传输对象
│       ├── ApiResponse.java
│       └── PatronDTO.java
│
└── interfaces/                           ← 接口层（REST API）
    └── rest/
        ├── PatronController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces → application → domain
                           （仓储接口由 Spring Data JPA 自动实现，无独立 infrastructure 层）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务和仓储接口）
- **interfaces 层**依赖 application 层（调用应用服务）
- 本模块目前没有独立的 infrastructure 子包，仓储由 Spring Data JPA 的接口代理机制自动实现

---

## 3. 引导与配置层

### `PatronApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.patron", "com.library.shared"})
@EnableJpaRepositories
public class PatronApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatronApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类，启动整个读者上下文的 Spring 容器。

**设计要点：**

- `scanBasePackages` 包含 `com.library.shared`：确保共享模块中的 `PatronId`、`DomainEvent`、`DomainEventPublisher` 等组件能被扫描到
- `@EnableJpaRepositories`：显式启用 JPA 仓储的自动发现机制

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：基础设施配置。`@EnableJpaAuditing` 启用 JPA 审计功能，让实体上的 `@CreatedDate`、`@LastModifiedDate` 注解自动填充创建/修改时间。

**为什么放在 config 包而不是 infrastructure？** 这是一个全局性的 JPA 配置，不属于某个具体的持久化实现，所以独立放在 config 包。

---

## 4. 领域层（Domain Layer）

> 领域层是 DDD 的核心。这里包含纯业务逻辑，不依赖任何外部框架（除了 JPA 注解作为持久化映射）。

### 4.1 领域模型（domain/model）

#### 4.1.1 `Patron.java` —— 聚合根：读者

**DDD 角色**：**聚合根（Aggregate Root）**

Patron 是读者上下文中唯一的聚合根，管理读者的完整生命周期：注册、信息更新、状态变更、借阅记录、罚款管理。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId PatronId id` | 使用共享模块的强类型 ID，而非裸 String |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突 |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate` | 自动记录创建/修改时间 |
| 构造控制 | `protected Patron()` + `public Patron(...)` | JPA 要求无参构造器，但 `protected` 防止外部直接实例化 |
| 工厂方法 | `Patron.create(...)` | 静态工厂方法，确保创建时总是生成新 ID 和正确的初始状态 |
| 值对象嵌入 | `@Embedded BorrowingPrivilege` | 借阅权限作为值对象嵌入，与 Patron 同表存储 |
| 自验证 | `validateName()`, `validateEmail()` | 构造和更新时执行字段验证，保证不变量 |
| 数据库索引 | `@Index` 在 email、status、patron_type 上 | 加速按邮箱查重、按状态/类型过滤的查询 |

**状态机：会员生命周期**

```
                        register()
                           │
                           ▼
  ┌──────── ACTIVE ◄──────┘
  │         │
  │   suspend()      reactivate()
  │         │              │
  │         ▼              │
  │    SUSPENDED ──────────┘
  │         │
  │    terminate()
  │         │
  │         ▼
  │    TERMINATED（终态）
  │
  │  罚款超限(≥50元) 时自动 SUSPENDED
  │  缴纳/豁免罚款降至 <50元 时自动回 ACTIVE
  └── terminate() 前提：无在借图书、无未缴罚款
```

**关键业务规则（编码在领域模型中）：**

| 方法 | 业务规则 |
|------|----------|
| `canBorrow()` | 四重检查：状态为 ACTIVE、当前借阅数 < 上限、罚款 < 50元、会员未过期 |
| `recordLoan()` | 前置条件：`canBorrow()` 为 true；增加当前借阅数和历史借阅数 |
| `recordReturn()` | 前置条件：当前借阅数 > 0；减少当前借阅数 |
| `addFine()` | 金额必须为正；加到未缴罚款；罚款 >= 50元时自动停用 |
| `payFine()` | 金额必须为正且不超过未缴罚款；缴纳后罚款 < 50元时自动恢复 |
| `waiveFine()` | 与 payFine() 逻辑相同，但语义是"豁免"而非"缴纳" |
| `suspend()` | 不能停用已终止的读者；不能重复停用 |
| `reactivate()` | 只能恢复已停用的读者；罚款超限不允许恢复 |
| `terminate()` | 不能重复终止；有在借图书不允许终止；有未缴罚款不允许终止 |
| `updatePersonalInfo()` | 已终止的读者不能更新信息 |
| `extendMembership()` | 已终止的会员不能续期 |
| `updatePatronType()` | 变更类型时自动重建 BorrowingPrivilege 值对象 |

**代码片段 —— canBorrow() 方法：**

```java
public boolean canBorrow() {
    if (this.status != MembershipStatus.ACTIVE) return false;
    if (this.currentLoans >= this.borrowingPrivilege.getMaxLoans()) return false;
    if (this.outstandingFines.compareTo(MAX_ALLOWED_FINE) >= 0) return false;
    if (this.membershipExpiry != null && this.membershipExpiry.isBefore(LocalDate.now())) return false;
    return true;
}
```

**查询方法：**

| 方法 | 返回 | 用途 |
|------|------|------|
| `isActive()` | boolean | 状态是否为 ACTIVE |
| `isSuspended()` | boolean | 状态是否为 SUSPENDED |
| `isTerminated()` | boolean | 状态是否为 TERMINATED |
| `hasOutstandingFines()` | boolean | 是否有未缴罚款 |
| `isMembershipValid()` | boolean | 会员是否未过期 |
| `getRemainingLoanQuota()` | int | 剩余可借数量 |
| `getFullName()` | String | firstName + " " + lastName |

---

#### 4.1.2 `BorrowingPrivilege.java` —— 值对象：借阅权限

**DDD 角色**：**值对象（Value Object）**

BorrowingPrivilege 封装了读者的借阅权限参数，作为值对象嵌入 Patron 聚合根中（`@Embedded`），与 Patron 同表存储。

```java
@Embeddable
public class BorrowingPrivilege {
    @Column(name = "max_loans")        private Integer maxLoans;
    @Column(name = "loan_period_days") private Integer loanPeriodDays;
    @Column(name = "max_renewals")     private Integer maxRenewals;
    @Column(name = "daily_fine_rate")  private BigDecimal dailyFineRate;
    @Column(name = "max_fine_amount")  private BigDecimal maxFineAmount;
    @Column(name = "can_place_holds")  private Boolean canPlaceHolds;
    @Column(name = "max_holds")        private Integer maxHolds;
    @Column(name = "can_recall_books") private Boolean canRecallBooks;
}
```

**值对象特征：**

1. **不可变性**：构造后无修改方法（无 setter），更新权限时整体替换为新实例
2. **自验证**：构造函数中验证所有数值参数为正数或非负数
3. **按 PatronType 初始化**：构造时根据读者类型自动设定默认参数

**不同读者类型的默认借阅权限参数：**

| 参数 | STUDENT | FACULTY | STAFF | ALUMNI | COMMUNITY |
|------|---------|---------|-------|--------|-----------|
| maxLoans | 5 | 20 | 10 | 3 | 2 |
| loanPeriodDays | 21 | 90 | 30 | 14 | 7 |
| maxRenewals | 1 | 3 | 2 | 0 | 0 |
| dailyFineRate | 0.50 | 0.25 | 0.30 | 0.75 | 1.00 |
| maxFineAmount | 30.00 | 50.00 | 40.00 | 20.00 | 15.00 |
| canPlaceHolds | true | true | true | true | false |
| maxHolds | 3 | 10 | 5 | 2 | 0 |
| canRecallBooks | false | true | false | false | false |

**业务语义**：FACULTY（教师）权限最高——20本、90天、可催还；COMMUNITY（社区）权限最低——2本、7天、不可预约。

**查询方法：**

- `hasRenewalQuota(int currentRenewals)`：当前续借次数是否还在限额内
- `hasHoldQuota(int currentHolds)`：是否允许预约且当前预约数在限额内

---

#### 4.1.3 `enums/MembershipStatus.java` —— 枚举：会员状态

```java
public enum MembershipStatus {
    ACTIVE,       // 激活：正常使用
    SUSPENDED,    // 停用：暂停借阅（罚款超限、管理停用、会员过期）
    TERMINATED    // 终止：永久注销（终态，不可逆）
}
```

**DDD 意义**：定义了读者会员的三态生命周期。注意 TERMINATED 是终态，不存在从 TERMINATED 恢复到其他状态的业务路径。

---

#### 4.1.4 `enums/PatronType.java` —— 枚举：读者类型

```java
public enum PatronType {
    STUDENT,    // 学生
    FACULTY,    // 教职工（教师）
    STAFF,      // 职工
    ALUMNI,     // 校友
    COMMUNITY   // 社区用户
}
```

**DDD 意义**：读者类型直接决定了 BorrowingPrivilege 的默认参数。当 PatronType 变更时，Patron 聚合根的 `updatePatronType()` 方法会重建整个 BorrowingPrivilege 值对象。

---

### 4.2 领域事件（domain/event）

> 领域事件表示领域中已经发生的、有业务意义的事情。在 DDD 中，事件用于实现聚合间和上下文间的解耦通信。

所有领域事件都继承自 `library-shared` 模块中的 `DomainEvent` 基类，该基类提供了 `eventId`、`eventType`、`occurredOn` 等通用属性。

#### 4.2.1 `PatronRegisteredEvent.java`

```java
public class PatronRegisteredEvent extends DomainEvent {
    private final PatronId patronId;
    private final String fullName;
    private final String email;
    private final PatronType patronType;
    private final LocalDate memberSince;
}
```

**触发时机**：`PatronManagementService.registerPatron()` 成功保存新读者后发布。

**携带信息**：新读者的 ID、姓名、邮箱、类型、注册日期——这是其他上下文（如通知上下文发送欢迎邮件）最需要的信息。

---

#### 4.2.2 `PatronUpdatedEvent.java`

```java
public class PatronUpdatedEvent extends DomainEvent {
    private final PatronId patronId;
}
```

**触发时机**：读者个人信息更新后发布。

**设计特点**：只携带 patronId，不包含具体修改内容。这是一种轻量级事件设计——订阅方如果需要详细信息，可以根据 ID 回查。

---

#### 4.2.3 `PatronSuspendedEvent.java`

```java
public class PatronSuspendedEvent extends DomainEvent {
    private final PatronId patronId;
    private final String reason;
}
```

**触发时机**：读者被停用时发布（包括管理员手动停用和会员过期批量停用）。

**携带 reason**：停用原因在审计和通知场景中非常重要。

---

#### 4.2.4 `PatronReactivatedEvent.java`

```java
public class PatronReactivatedEvent extends DomainEvent {
    private final PatronId patronId;
    private final String reason;
}
```

**触发时机**：被停用的读者恢复为激活状态时发布。

---

#### 4.2.5 `PatronTerminatedEvent.java`

```java
public class PatronTerminatedEvent extends DomainEvent {
    private final PatronId patronId;
    private final String reason;
}
```

**触发时机**：读者会员被终止时发布。终止是不可逆操作，其他上下文（如流通上下文）收到此事件后应取消该读者的所有预约。

---

#### 4.2.6 `PatronTypeChangedEvent.java`

```java
public class PatronTypeChangedEvent extends DomainEvent {
    private final PatronId patronId;
    private final PatronType oldType;
    private final PatronType newType;
}
```

**触发时机**：读者类型变更时发布。

**携带新旧类型**：订阅方可以据此调整该读者的相关配置（如流通上下文重新计算借阅上限）。

---

### 4.3 领域异常（domain/exception）

> DDD 中，异常也是领域逻辑的一部分。每个异常都带有**错误码（errorCode）**，方便 API 层统一处理和前端国际化。

#### 4.3.1 `DomainException.java` —— 异常基类

```java
public class DomainException extends RuntimeException {
    private final String errorCode;

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

**设计要点：**

- 非 abstract（与 catalog 模块不同），所有领域异常直接继承此类
- `errorCode`：每个子类定义自己的错误码字符串（如 `"PATRON_NOT_FOUND"`）
- 继承 `RuntimeException`：是非受检异常，不强制调用方处理

---

#### 4.3.2 具体异常类

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `PatronNotFoundException` | `PATRON_NOT_FOUND` | 通过 ID 查找读者不存在 | 404 |
| `DuplicateEmailException` | `DUPLICATE_EMAIL` | 注册或更新时邮箱已被使用 | 409 Conflict |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作（如停用已终止读者） | 409 Conflict |
| `PatronCannotBorrowException` | `PATRON_CANNOT_BORROW` | 读者不满足借阅条件（状态/配额/罚款/过期） | 403 Forbidden |

每个异常类的实现示例：

```java
// PatronCannotBorrowException —— 携带诊断信息
public class PatronCannotBorrowException extends DomainException {
    public PatronCannotBorrowException(MembershipStatus status, int currentLoans, BigDecimal outstandingFines) {
        super("PATRON_CANNOT_BORROW",
            String.format("Patron cannot borrow: status=%s, currentLoans=%d, outstandingFines=%s",
                status, currentLoans, outstandingFines));
    }
}
```

**PatronCannotBorrowException 的设计亮点**：错误信息中包含了当前状态、在借数量和未缴罚款——这三个值正好是 `canBorrow()` 方法检查的四个条件中的三个（第四个是会员过期），有助于快速定位借阅失败的原因。

---

### 4.4 仓储接口（domain/repository）

> 仓储（Repository）是 DDD 中领域模型与数据存储之间的桥梁。**领域层只定义接口**，由 Spring Data JPA 自动生成实现。

#### 4.4.1 `PatronRepository.java`

```java
@Repository
public interface PatronRepository extends JpaRepository<Patron, PatronId> {

    Optional<Patron> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Patron> findByStatus(MembershipStatus status);

    List<Patron> findByPatronType(PatronType patronType);

    List<Patron> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);

    @Query("SELECT p FROM Patron p WHERE p.status = 'ACTIVE' AND p.membershipExpiry < :date")
    List<Patron> findExpiredActiveMembers(LocalDate date);

    @Query("SELECT p FROM Patron p WHERE p.status = 'ACTIVE' AND p.outstandingFines > 0")
    List<Patron> findActivePatronsWithOutstandingFines();

    long countByStatus(MembershipStatus status);

    long countByPatronType(PatronType patronType);
}
```

**DDD 定位**：继承 `JpaRepository` 获得 CRUD 能力，无需独立的实现类。

**方法类型分析：**

| 方法类型 | 示例 | 说明 |
|----------|------|------|
| Spring Data 派生查询 | `findByEmail()`, `existsByEmail()`, `findByStatus()` | 由 Spring Data 自动实现，根据方法名生成 SQL |
| JPQL 自定义查询 | `findExpiredActiveMembers()`, `findActivePatronsWithOutstandingFines()` | 使用 `@Query` + JPQL，处理状态+条件的组合查询 |
| 模糊搜索 | `findByFirstNameContainingOrLastNameContaining()` | 支持按姓名模糊搜索（OR 语义） |
| 统计查询 | `countByStatus()`, `countByPatronType()` | 按维度统计读者数量 |

**关键查询的用途：**

- `existsByEmail()`：注册和更新时检查邮箱唯一性（被 PatronManagementService 调用）
- `findExpiredActiveMembers()`：批量查找过期会员，供 `suspendExpiredMemberships()` 定时任务使用
- `findActivePatronsWithOutstandingFines()`：查找有未缴罚款的活跃读者，可用于催缴通知

---

### 4.5 领域服务（domain/service）

> 领域服务用于承载**不适合放在单个实体或值对象中的业务逻辑**，例如跨聚合的协调、外部仓储的调用、领域事件的发布等。

#### 4.5.1 `PatronManagementService.java`

**DDD 角色**：**领域服务** —— 读者聚合的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class PatronManagementService {
    private final PatronRepository patronRepository;
    private final DomainEventPublisher eventPublisher;
}
```

**依赖：**

- `PatronRepository`：读者数据持久化
- `DomainEventPublisher`（共享模块）：领域事件发布

**方法一览：**

| 方法 | 类型 | 业务逻辑 |
|------|------|----------|
| `registerPatron()` | 写 | 检查邮箱唯一性 → 创建 Patron → 保存 → 发布 PatronRegisteredEvent |
| `updatePatronInfo()` | 写 | 查找读者 → 检查新邮箱唯一性 → 更新信息 → 保存 → 发布 PatronUpdatedEvent |
| `suspendPatron()` | 写 | 查找读者 → 调用 `suspend()` → 保存 → 发布 PatronSuspendedEvent |
| `reactivatePatron()` | 写 | 查找读者 → 调用 `reactivate()` → 保存 → 发布 PatronReactivatedEvent |
| `terminatePatron()` | 写 | 查找读者 → 调用 `terminate()` → 保存 → 发布 PatronTerminatedEvent |
| `extendMembership()` | 写 | 查找读者 → 调用 `extendMembership()` → 保存 |
| `addFine()` | 写 | 查找读者 → 调用 `addFine()` → 保存 |
| `payFine()` | 写 | 查找读者 → 调用 `payFine()` → 保存 |
| `waiveFine()` | 写 | 查找读者 → 调用 `waiveFine()` → 保存 |
| `changePatronType()` | 写 | 查找读者 → 记录旧类型 → 调用 `updatePatronType()` → 保存 → 发布 PatronTypeChangedEvent |
| `suspendExpiredMemberships()` | 写 | 查询所有过期活跃会员 → 逐个停用 → 发布事件（批量操作） |
| `getPatron()` | 读 | 按ID查找读者 |
| `getAllPatrons()` | 读 | 查询所有读者 |
| `getPatronsByStatus()` | 读 | 按状态过滤读者 |
| `getPatronsByType()` | 读 | 按类型过滤读者 |

**DDD 设计要点：**

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写操作方法覆盖为 `@Transactional`
2. **事件发布**：关键状态变更操作完成后发布领域事件，实现与其他上下文的解耦
3. **私有辅助方法 `findOrThrow()`**：统一处理"按ID查找，不存在则抛异常"的模式
4. **保护聚合不变量**：业务规则（如"不能终止有在借图书的读者"）编码在 Patron 实体中，领域服务只负责编排
5. **批量操作的容错设计**：`suspendExpiredMemberships()` 中单个读者的停用失败不会影响其他读者的处理（catch 忽略异常）

**代码片段 —— registerPatron() 方法：**

```java
@Transactional
public Patron registerPatron(String firstName, String lastName, String email,
                              String phone, String address, String city, String postalCode,
                              PatronType patronType) {
    if (patronRepository.existsByEmail(email)) {
        throw new DuplicateEmailException(email);
    }
    Patron patron = Patron.create(firstName, lastName, email, patronType);
    patron.updatePersonalInfo(firstName, lastName, email, phone, address, city, postalCode);
    Patron saved = patronRepository.save(patron);
    eventPublisher.publish(new PatronRegisteredEvent(
        saved.getId(), saved.getFullName(), saved.getEmail(), saved.getPatronType(), saved.getMemberSince()
    ));
    return saved;
}
```

**代码片段 —— changePatronType() 方法：**

```java
@Transactional
public void changePatronType(PatronId patronId, PatronType newType) {
    Patron patron = findOrThrow(patronId);
    PatronType oldType = patron.getPatronType();     // 记录旧类型
    patron.updatePatronType(newType);                 // 变更类型（自动重建 BorrowingPrivilege）
    patronRepository.save(patron);
    eventPublisher.publish(new PatronTypeChangedEvent(patronId, oldType, newType));
}
```

---

## 5. 应用层（Application Layer）

> 应用层是 DDD 中的**"编排层"**，它不包含业务逻辑，而是协调领域对象完成用例。它的职责是：接收外部请求 → 转换参数 → 调用领域服务 → 转换输出 → 返回结果。

### 5.1 应用服务（application/service）

#### `PatronApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional(readOnly = true)
public class PatronApplicationService {
    private final PatronManagementService patronManagementService;  // 领域服务
    private final PatronRepository patronRepository;                 // 仓储（直接用于查询）
}
```

**PatronManagementService vs PatronApplicationService 的区别：**

| 维度 | PatronManagementService（领域服务） | PatronApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（PatronId, PatronType, BigDecimal） | 外部格式（Command 对象） |
| 输出 | 领域对象（Patron） | DTO（PatronDTO） |
| 职责 | 业务规则校验和领域事件发布 | 参数转换和结果转换 |
| 调用者 | 应用服务 | 控制器 |

**方法一览：**

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| `registerPatron()` | RegisterPatronCommand | PatronDTO | 注册新读者 |
| `updatePatron()` | UpdatePatronCommand | PatronDTO | 更新读者信息 |
| `getPatron()` | PatronId | PatronDTO | 按ID查询读者 |
| `getAllPatrons()` | - | List\<PatronDTO\> | 查询所有读者 |
| `suspendPatron()` | SuspendPatronCommand | void | 停用读者 |
| `reactivatePatron()` | ReactivatePatronCommand | void | 恢复读者 |
| `terminatePatron()` | TerminatePatronCommand | void | 终止读者 |
| `extendMembership()` | ExtendMembershipCommand | void | 续期会员 |
| `addFine()` | AddFineCommand | void | 添加罚款 |
| `payFine()` | PayFineCommand | void | 缴纳罚款 |
| `waiveFine()` | WaiveFineCommand | void | 豁免罚款 |
| `changePatronType()` | ChangePatronTypeCommand | void | 变更读者类型 |

**典型方法流程**（以 `registerPatron` 为例）：

```
Controller 调用 → PatronApplicationService.registerPatron(RegisterPatronCommand)
  1. 从 Command 中提取参数（getFirstName(), getEmail(), getPatronType()）
  2. 调用 patronManagementService.registerPatron(...)
  3. 将返回的 Patron 实体转换为 PatronDTO（PatronDTO.fromDomain(patron)）
  4. 返回 PatronDTO
```

---

### 5.2 命令对象（application/command）

> 命令（Command）是 CQRS 模式中"写操作"的输入对象，封装了执行某个操作所需的全部参数。本模块使用传统 class（含 getter/setter），而非 record，以便 Spring MVC 的请求绑定。

#### 5.2.1 `RegisterPatronCommand.java`

```java
public class RegisterPatronCommand {
    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    private String lastName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String postalCode;

    @NotNull(message = "Patron type must not be null")
    private PatronType patronType;
}
```

**设计要点：**

- `@NotBlank`、`@Email`、`@NotNull` 是 Bean Validation 注解，在 Controller 层由 `@Valid` 触发自动验证
- `PatronType` 是领域枚举，直接出现在命令对象中——Spring MVC 会自动将请求中的字符串转换为枚举值
- phone/address/city/postalCode 为可选字段（无验证注解）

---

#### 5.2.2 `UpdatePatronCommand.java`

```java
public class UpdatePatronCommand {
    private PatronId patronId;    // 路径参数传入，不由请求体绑定
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
}
```

**与 RegisterPatronCommand 的区别**：

- 包含 `PatronId`（从 URL 路径中提取，不从请求体绑定）
- 没有 Bean Validation 注解（更新操作的部分字段可以为 null）

---

#### 5.2.3 状态变更命令

以下四个命令结构类似，都包含 `PatronId` + `reason`：

| 命令类 | 附加字段 | 用途 |
|--------|----------|------|
| `SuspendPatronCommand` | reason | 停用读者 |
| `ReactivatePatronCommand` | reason | 恢复读者 |
| `TerminatePatronCommand` | reason | 终止读者 |

```java
// 以 SuspendPatronCommand 为例
public class SuspendPatronCommand {
    private PatronId patronId;
    private String reason;
}
```

---

#### 5.2.4 `ExtendMembershipCommand.java`

```java
public class ExtendMembershipCommand {
    private PatronId patronId;
    private int months;      // 续期月数
    private String reason;
}
```

**独特之处**：`int months`（基本类型），调用时由 Controller 从请求体 Map 中提取并转换。

---

#### 5.2.5 罚款相关命令

| 命令类 | 附加字段 | 用途 |
|--------|----------|------|
| `AddFineCommand` | amount + reason | 添加罚款 |
| `PayFineCommand` | amount | 缴纳罚款（无 reason） |
| `WaiveFineCommand` | amount + reason | 豁免罚款 |

```java
// 以 AddFineCommand 为例
public class AddFineCommand {
    private PatronId patronId;
    private BigDecimal amount;
    private String reason;
}
```

**PayFineCommand 无 reason**：缴纳是客观行为，不需要理由；而 Add 和 Waive 是管理行为，需要记录原因。

---

#### 5.2.6 `ChangePatronTypeCommand.java`

```java
public class ChangePatronTypeCommand {
    private PatronId patronId;
    private PatronType newPatronType;
}
```

最简洁的命令——只有 ID 和新的类型。

---

### 5.3 数据传输对象（application/dto）

#### 5.3.1 `ApiResponse.java` —— 统一响应信封

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
- 静态工厂方法 `success()` / `error()` 提供便捷构造
- `timestamp` 自动设置为当前时间

---

#### 5.3.2 `PatronDTO.java`

```java
public class PatronDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String patronType;        // 枚举 → String
    private String status;            // 枚举 → String
    private LocalDate memberSince;
    private LocalDate membershipExpiry;
    private Integer currentLoans;
    private BigDecimal outstandingFines;
    private Integer totalBorrowed;
    private Integer maxLoans;         // 来自 BorrowingPrivilege
    private Integer loanPeriodDays;   // 来自 BorrowingPrivilege
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PatronDTO fromDomain(Patron patron) { ... }
}
```

**关键设计：`fromDomain(Patron)` 组装器方法**

将 Patron 聚合根（包含嵌入的 BorrowingPrivilege）展平为一个扁平的 DTO：

- `id`：从 `PatronId` → `String`（`patron.getId().getValue()`）
- `patronType`：从 `PatronType` 枚举 → `String`（`patron.getPatronType().name()`）
- `status`：从 `MembershipStatus` 枚举 → `String`
- `maxLoans`/`loanPeriodDays`：从嵌入的 `BorrowingPrivilege` 值对象中提取

DTO 总是使用基本类型和字符串，方便 JSON 序列化，不暴露领域模型的内部结构。

---

## 6. 基础设施层（Infrastructure Layer）

> 本模块目前**没有独立的 infrastructure 子包**。仓储接口 `PatronRepository` 由 Spring Data JPA 的接口代理机制自动实现，无需手写实现类。

**与 Catalog 模块的对比：**

| 维度 | library-catalog | library-patron |
|------|-----------------|----------------|
| 仓储实现 | `BookRepositoryImpl`（Criteria API 动态查询） | 无，全部由 Spring Data 派生 |
| 事件发布 | `CatalogDomainEventPublisher`（Spring + Kafka 双通道） | 共享模块的 `DomainEventPublisher` |
| 复杂查询 | 需要 `CustomBookRepository` + `BookRepositoryImpl` | 无，JPQL 和方法名派生足够 |

**原因**：Patron 模块的查询场景相对简单（按ID、按邮箱、按状态、按类型），不需要 Criteria API 的动态组合查询能力。如果未来需要更复杂的查询（如多维度组合搜索），可以像 catalog 模块一样引入 CustomRepository + Impl 的模式。

---

## 7. 接口层（Interfaces Layer）

> 接口层是系统与外部世界的边界。它接收 HTTP 请求，调用应用服务，返回 HTTP 响应。**不包含任何业务逻辑。**

### 7.1 `PatronController.java`

```java
@RestController
@RequestMapping("/api/patrons")
@Tag(name = "Patron Management", description = "APIs for managing library patrons")
public class PatronController {
    private final PatronApplicationService patronService;
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/patrons` | 注册新读者 | 命令 |
| `GET` | `/api/patrons/{id}` | 获取读者详情 | 查询 |
| `GET` | `/api/patrons` | 获取所有读者 | 查询 |
| `PUT` | `/api/patrons/{id}` | 更新读者信息 | 命令 |
| `POST` | `/api/patrons/{id}/suspend` | 停用读者 | 命令（状态变更） |
| `POST` | `/api/patrons/{id}/reactivate` | 恢复读者 | 命令（状态变更） |
| `POST` | `/api/patrons/{id}/terminate` | 终止读者 | 命令（状态变更） |
| `POST` | `/api/patrons/{id}/extend-membership` | 续期会员 | 命令 |
| `POST` | `/api/patrons/{id}/fines` | 添加罚款 | 命令 |
| `POST` | `/api/patrons/{id}/fines/pay` | 缴纳罚款 | 命令 |
| `POST` | `/api/patrons/{id}/fines/waive` | 豁免罚款 | 命令 |
| `PUT` | `/api/patrons/{id}/type` | 变更读者类型 | 命令 |

**设计要点：**

- **CQRS 体现**：POST/PUT 是命令（写），GET 是查询（读）；状态变更操作使用 POST 而非 PUT
- **统一响应**：所有端点返回 `ApiResponse<T>` 信封
- **Swagger 注解**：`@Operation(summary = ...)` 生成 API 文档
- **参数验证**：`@Valid @RequestBody RegisterPatronCommand` 触发 Bean Validation
- **请求体处理**：部分端点使用 `Map<String, String>` 或 `Map<String, Object>` 接收请求体，在 Controller 内部组装 Command 对象
- **ID 转换**：`@PathVariable String id` → `PatronId.of(id)` 在 Controller 中完成

**代码片段 —— suspendPatron() 端点：**

```java
@PostMapping("/{id}/suspend")
@Operation(summary = "Suspend a patron")
public ResponseEntity<ApiResponse<Void>> suspendPatron(
        @PathVariable String id,
        @RequestBody(required = false) Map<String, String> body) {
    String reason = body != null ? body.get("reason") : null;
    SuspendPatronCommand command = new SuspendPatronCommand(PatronId.of(id), reason);
    patronService.suspendPatron(command);
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

---

### 7.2 `GlobalExceptionHandler.java` —— 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PatronNotFoundException.class)          → 404
    @ExceptionHandler(DuplicateEmailException.class)          → 409 Conflict
    @ExceptionHandler(PatronCannotBorrowException.class)      → 403 Forbidden
    @ExceptionHandler(InvalidOperationException.class)        → 409 Conflict
    @ExceptionHandler(DomainException.class)                  → 根据 errorCode 映射
    @ExceptionHandler(MethodArgumentNotValidException.class)  → 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)          → 400 Bad Request
    @ExceptionHandler(Exception.class)                        → 500 Internal Server Error
}
```

**DDD 定位**：**跨切面（Cross-Cutting Concern）**，在接口层统一处理所有异常。

**错误码 → HTTP 状态映射逻辑：**

```java
private HttpStatus mapToHttpStatus(String errorCode) {
    return switch (errorCode) {
        case "PATRON_NOT_FOUND"      -> HttpStatus.NOT_FOUND;       // 404
        case "DUPLICATE_EMAIL"       -> HttpStatus.CONFLICT;        // 409
        case "PATRON_CANNOT_BORROW"  -> HttpStatus.FORBIDDEN;       // 403
        case "INVALID_OPERATION"     -> HttpStatus.CONFLICT;        // 409
        default                      -> HttpStatus.BAD_REQUEST;     // 400
    };
}
```

**异常处理层级**：

- 特定异常优先匹配（如 PatronNotFoundException → 404）
- 通用的 `DomainException` 兜底匹配，使用 `mapToHttpStatus()` 映射
- `IllegalArgumentException` 处理领域模型中的参数验证错误
- `Exception` 兜底所有未预期的异常，返回 500 并隐藏内部细节（"An unexpected error occurred"）

**返回格式统一**：所有异常都包装成 `ApiResponse.error(errorCode, message)`，前端只需处理一种响应格式。

---

## 8. 层间调用流程

以 **"注册新读者"** 为例，展示一次完整请求的层间调用链：

```
HTTP POST /api/patrons
  │
  ▼
┌──────────────────────────────────────────────────────────────┐
│ PatronController.registerPatron(RegisterPatronCommand)       │  ← interfaces 层
│   @Valid 触发 Bean Validation (@NotBlank, @Email, @NotNull)  │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ PatronApplicationService.registerPatron(RegisterPatronCommand)│  ← application 层
│   1. 从 Command 提取参数                                      │
│   2. 调用 patronManagementService.registerPatron(...)         │
│   3. PatronDTO.fromDomain(patron)  // Patron → DTO            │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ PatronManagementService.registerPatron(firstName, ...)        │  ← domain 层（服务）
│   1. patronRepository.existsByEmail(email) → 检查邮箱唯一性   │
│   2. Patron.create(...)              // 调用聚合根工厂方法      │
│   3. patron.updatePersonalInfo(...)  // 设置联系电话/地址等     │
│   4. patronRepository.save(patron)   // 持久化                 │
│   5. eventPublisher.publish(                                  │
│        new PatronRegisteredEvent(...))                        │
└───────┬───────────────────────────────┬──────────────────────┘
        │                               │
        ▼                               ▼
┌──────────────────┐  ┌─────────────────────────────────────────┐
│ PatronRepository  │  │ DomainEventPublisher (共享模块)          │
│ (Spring Data JPA  │  │   → 本地事件发布（Spring Event）         │
│  自动实现)        │  │   → 可扩展为 Kafka 远程事件               │
└──────────────────┘  └─────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ PostgreSQL                                                    │  ← 数据库
│ INSERT INTO patrons (id, first_name, last_name, email,       │
│   patron_type, status, member_since, current_loans, ...)     │
└──────────────────────────────────────────────────────────────┘
```

以 **"缴纳罚款"** 为例，展示罚款驱动的自动状态恢复：

```
HTTP POST /api/patrons/{id}/fines/pay
  │
  ▼
PatronController → PatronApplicationService.payFine(PayFineCommand)
  │
  ▼
PatronManagementService.payFine(patronId, amount)
  │
  ▼
Patron.payFine(amount)
  │  1. 验证金额为正且不超过未缴罚款
  │  2. outstandingFines -= amount
  │  3. if (status == SUSPENDED && outstandingFines < 50.00)
  │        → status = ACTIVE（自动恢复）
  │
  ▼
patronRepository.save(patron)  // 乐观锁版本号递增
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `PatronApplication.java` | Bootstrap | 启动类 | Spring Boot 入口，扫描 patron + shared 包 |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/Patron.java` | Domain | **聚合根** | 读者生命周期管理（注册/更新/状态/借阅/罚款） |
| 4 | `domain/model/BorrowingPrivilege.java` | Domain | **值对象** | 借阅权限参数（按 PatronType 初始化） |
| 5 | `domain/model/enums/MembershipStatus.java` | Domain | 枚举 | 会员状态（ACTIVE/SUSPENDED/TERMINATED） |
| 6 | `domain/model/enums/PatronType.java` | Domain | 枚举 | 读者类型（STUDENT/FACULTY/STAFF/ALUMNI/COMMUNITY） |
| 7 | `domain/event/PatronRegisteredEvent.java` | Domain | 领域事件 | 读者已注册 |
| 8 | `domain/event/PatronUpdatedEvent.java` | Domain | 领域事件 | 读者信息已更新 |
| 9 | `domain/event/PatronSuspendedEvent.java` | Domain | 领域事件 | 读者已停用 |
| 10 | `domain/event/PatronReactivatedEvent.java` | Domain | 领域事件 | 读者已恢复 |
| 11 | `domain/event/PatronTerminatedEvent.java` | Domain | 领域事件 | 读者已终止 |
| 12 | `domain/event/PatronTypeChangedEvent.java` | Domain | 领域事件 | 读者类型已变更 |
| 13 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 14 | `domain/exception/PatronNotFoundException.java` | Domain | 异常 | 读者不存在（404） |
| 15 | `domain/exception/DuplicateEmailException.java` | Domain | 异常 | 邮箱重复（409） |
| 16 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作（409） |
| 17 | `domain/exception/PatronCannotBorrowException.java` | Domain | 异常 | 不满足借阅条件（403） |
| 18 | `domain/repository/PatronRepository.java` | Domain | 仓储接口 | 读者数据访问（Spring Data JPA 自动实现） |
| 19 | `domain/service/PatronManagementService.java` | Domain | **领域服务** | 读者业务编排 + 事件发布 |
| 20 | `application/service/PatronApplicationService.java` | Application | **应用服务** | 读者用例编排 + DTO 转换 |
| 21 | `application/command/RegisterPatronCommand.java` | Application | 命令 | 注册读者入参 |
| 22 | `application/command/UpdatePatronCommand.java` | Application | 命令 | 更新读者入参 |
| 23 | `application/command/SuspendPatronCommand.java` | Application | 命令 | 停用读者入参 |
| 24 | `application/command/ReactivatePatronCommand.java` | Application | 命令 | 恢复读者入参 |
| 25 | `application/command/TerminatePatronCommand.java` | Application | 命令 | 终止读者入参 |
| 26 | `application/command/ExtendMembershipCommand.java` | Application | 命令 | 续期会员入参 |
| 27 | `application/command/AddFineCommand.java` | Application | 命令 | 添加罚款入参 |
| 28 | `application/command/PayFineCommand.java` | Application | 命令 | 缴纳罚款入参 |
| 29 | `application/command/WaiveFineCommand.java` | Application | 命令 | 豁免罚款入参 |
| 30 | `application/command/ChangePatronTypeCommand.java` | Application | 命令 | 变更读者类型入参 |
| 31 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 32 | `application/dto/PatronDTO.java` | Application | DTO | 读者数据传输 |
| 33 | `interfaces/rest/PatronController.java` | Interfaces | REST 控制器 | 读者 API（12 个端点） |
| 34 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：34 个源文件**
