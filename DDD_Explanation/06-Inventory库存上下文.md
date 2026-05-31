# Library Inventory 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-inventory` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

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

`library-inventory` 是图书馆管理系统的**库存上下文（Inventory Context）**，负责管理图书副本的物理库存：

| 职责 | 说明 |
|------|------|
| 图书馆管理 | 创建、更新、启用、停用图书馆 |
| 库存管理 | 为某本书在某图书馆创建库存记录，管理副本数量 |
| 副本管理 | 添加、移除、批量添加副本，跟踪副本状态和位置 |
| 借还流转 | 副本借出、归还的状态变更与计数同步 |
| 损坏/丢失报告 | 报告副本损坏或丢失，更新状态和库存计数 |
| 库存预警 | 低库存自动预警事件 |
| 领域事件 | 库存创建、副本增减、借还、损坏、丢失等事件通知 |

运行端口：`8082`，基础路径：`/api/inventory/*`

---

## 2. DDD 分层架构总览

```
library-inventory/src/main/java/com/library/inventory/
│
├── InventoryApplication.java                ← Spring Boot 启动类
├── config/                                  ← 基础设施配置
│   └── JpaConfig.java
│
├── domain/                                  ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                               ← 聚合根、实体、值对象
│   │   ├── CopyInventory.java               ← 聚合根：图书库存
│   │   ├── BookCopy.java                    ← 聚合内实体：图书副本
│   │   ├── Library.java                     ← 聚合根：图书馆
│   │   ├── Location.java                    ← 值对象：物理位置
│   │   └── enums/                           ← 枚举
│   │       ├── CopyStatus.java
│   │       └── CopyCondition.java
│   ├── event/                               ← 领域事件
│   │   ├── InventoryCreatedEvent.java
│   │   ├── CopyAddedEvent.java
│   │   ├── CopiesBatchAddedEvent.java
│   │   ├── CopyBorrowedEvent.java
│   │   ├── CopyReturnedEvent.java
│   │   ├── CopyDamagedEvent.java
│   │   ├── CopyLostEvent.java
│   │   └── LowStockAlertEvent.java
│   ├── exception/                           ← 领域异常（含错误码）
│   │   ├── DomainException.java             ← 异常基类
│   │   ├── InventoryNotFoundException.java
│   │   ├── CopyNotFoundException.java
│   │   ├── LibraryNotFoundException.java
│   │   ├── DuplicateInventoryException.java
│   │   ├── NoAvailableCopyException.java
│   │   └── InvalidOperationException.java
│   ├── repository/                          ← 仓储接口（由 Spring Data JPA 自动实现）
│   │   ├── CopyInventoryRepository.java
│   │   ├── BookCopyRepository.java
│   │   └── LibraryRepository.java
│   └── service/                             ← 领域服务
│       └── InventoryManagementService.java
│
├── application/                             ← 应用层（编排、协调）
│   ├── service/
│   │   ├── InventoryApplicationService.java ← 应用服务：编排库存用例
│   │   └── LibraryApplicationService.java   ← 应用服务：编排图书馆用例
│   ├── command/                             ← 写操作入参
│   │   ├── CreateInventoryCommand.java
│   │   ├── AddCopyCommand.java
│   │   ├── BatchAddCopiesCommand.java
│   │   └── CreateLibraryCommand.java
│   └── dto/                                 ← 输出传输对象
│       ├── ApiResponse.java
│       ├── CopyInventoryDTO.java
│       ├── BookCopyDTO.java
│       └── LibraryDTO.java
│
└── interfaces/                              ← 接口层（REST API）
    └── rest/
        ├── InventoryController.java
        ├── LibraryController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces → application → domain
                         （仓储接口由 Spring Data JPA 自动实现）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务和仓储接口）
- **interfaces 层**依赖 application 层（调用应用服务）
- 本模块没有独立的 infrastructure 子包，仓储实现由 Spring Data JPA 自动代理完成

---

## 3. 引导与配置层

### `InventoryApplication.java`

```java
@SpringBootApplication(scanBasePackages = {"com.library.inventory", "com.library.shared"})
@EnableJpaRepositories
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类，启动整个库存上下文的 Spring 容器。

**设计要点**：

- `scanBasePackages` 同时扫描 `com.library.inventory` 和 `com.library.shared`，确保共享模块的领域事件基类、强类型 ID 等被正确加载
- `@EnableJpaRepositories` 启用 Spring Data JPA 仓储自动发现

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：基础设施配置。`@EnableJpaAuditing` 启用 JPA 审计功能，让实体上的 `@CreatedDate`、`@LastModifiedDate`、`@CreatedBy`、`@LastModifiedBy` 注解自动填充创建/修改时间和操作人。

---

## 4. 领域层（Domain Layer）

> 领域层是 DDD 的核心。这里包含纯业务逻辑，不依赖任何外部框架（除了 JPA 注解作为持久化映射）。

### 4.1 领域模型（domain/model）

#### 4.1.1 `CopyInventory.java` —— 聚合根：图书库存

**DDD 角色**：**聚合根（Aggregate Root）**

CopyInventory 是库存上下文中最核心的聚合根，表示"某本书在某图书馆的库存"。它管理副本的添加、移除、借还计数同步等操作，并维护 `totalCopies` 和 `availableCopies` 两个关键业务计数器。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId CopyInventoryId id` | 使用共享模块的强类型 ID |
| 乐观锁 | `@Version Long version` | 防止并发修改导致库存计数不一致 |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` | 自动记录创建/修改时间和操作人 |
| 聚合内实体 | `@OneToMany List<BookCopy> copies` | 副本是库存聚合的内部实体，级联保存 |
| 集合保护 | `Collections.unmodifiableList(copies)` | 返回不可变集合，防止外部绕过领域逻辑 |
| 业务常量 | `MAX_COPIES_PER_INVENTORY = 1000` | 限制单个库存的最大副本数 |
| 预警阈值 | `LOW_STOCK_THRESHOLD = 2` | 可用副本数 <= 2 触发低库存预警 |

**状态机：库存计数同步**

```
addCopy()          → totalCopies++, availableCopies++
removeCopy()       → totalCopies--, availableCopies-- (仅当副本可用时)
onCopyStatusChanged(AVAILABLE → X)  → availableCopies--
onCopyStatusChanged(X → AVAILABLE)  → availableCopies++
```

**关键方法：**

- `addCopy()`：添加单个副本，自动生成条码，更新计数
- `addCopies(count, ...)`：批量添加副本，自动生成序列条码
- `getAvailableCopy()`：获取一本可用副本，无可用则抛出 `NoAvailableCopyException`
- `removeCopy()`：移除副本，同时更新库存计数
- `isLowStock()`：判断是否触发低库存预警
- `getAvailabilityRate()`：计算可用率（availableCopies / totalCopies）

**条码生成规则**：

```java
String.format("%s-%s-%06d", libraryCode, bookId前8位, 序列号)
// 示例: BJ-01-3f9a2b1c-000001
```

---

#### 4.1.2 `BookCopy.java` —— 聚合内实体：图书副本

**DDD 角色**：**聚合内实体（Entity within Aggregate）**

BookCopy 表示图书馆中的一本物理副本，是 CopyInventory 聚合的内部实体。它管理副本的完整状态生命周期。

```java
@Entity
@Table(name = "book_copies")
public class BookCopy {
    @EmbeddedId private CopyId id;
    @Column(nullable = false, unique = true) private String barcode;
    @Embedded private Location location;
    @Enumerated(EnumType.STRING) private CopyStatus status;
    @Enumerated(EnumType.STRING) private CopyCondition condition;
    @Column(name = "borrow_count") private Integer borrowCount;
    // ... 审计和状态追踪字段
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| 唯一标识 | `CopyId`（共享模块强类型 ID），`barcode` 有唯一约束 |
| 值对象嵌入 | `@Embedded Location location`，物理位置作为值对象嵌入 |
| 状态追踪 | 完整追踪损坏日期、损坏描述、丢失日期、丢失原因、移除日期、移除原因 |
| 借阅统计 | `borrowCount` 和 `lastBorrowedDate` 记录借阅次数和最后借出日期 |
| 采购信息 | `acquisitionDate`、`acquisitionMethod`、`acquisitionCost` 记录采购成本 |

**状态机：副本生命周期**

```
AVAILABLE ──markAsBorrowed()──→ BORROWED ──markAsReturned()──→ AVAILABLE
    │                              │
    │──markAsReserved()──→ RESERVED ──releaseReservation()──→ AVAILABLE
    │
    │──markAsDamaged()──→ DAMAGED ──markAsUnderRepair()──→ UNDER_REPAIR ──markAsRepaired()──→ AVAILABLE
    │
    │──markAsLost()──→ LOST
    │
    └──markAsRemoved()──→ REMOVED
```

**业务规则（编码在实体中）：**

- `markAsBorrowed()`：只有 AVAILABLE 状态才能借出
- `markAsReturned()`：只有 BORROWED 状态才能归还
- `markAsReserved()`：只有 AVAILABLE 状态才能预约
- `markAsDamaged()`：REMOVED 状态不能标记为损坏
- `markAsLost()`：REMOVED 状态不能标记为丢失
- `markAsUnderRepair()`：只有 DAMAGED 状态才能进入维修
- `markAsRepaired()`：只有 UNDER_REPAIR 状态才能标记修复完成，修复后状态变为 AVAILABLE、品相变为 GOOD
- `markAsRemoved()`：BORROWED 状态不能移除（必须先归还）；已移除的不能重复移除
- `canBeRemoved()`：BORROWED 和 REMOVED 状态不可移除
- `canBeTransferred()`：BORROWED 和 REMOVED 状态不可转移

---

#### 4.1.3 `Library.java` —— 聚合根：图书馆

**DDD 角色**：**聚合根（Aggregate Root）**

Library 表示一个图书馆分支机构，管理图书馆的基本信息、联系方式和运营状态。

```java
@Entity
@Table(name = "libraries")
public class Library {
    @EmbeddedId private LibraryId id;
    @Column(unique = true, length = 20) private String code;
    @Column(nullable = false, length = 200) private String name;
    // 联系方式、地址、运营信息...
    @Column(name = "is_active") private Boolean active;
    @Version private Long version;
}
```

**业务规则：**

- `validateCode()`：编号不能为空，不能超过 20 字符，自动转大写并去除空白
- `validateName()`：名称不能为空，不能超过 200 字符
- `activate()` / `deactivate()`：启用/停用图书馆（软状态变更）
- `isActive()`：判断图书馆是否处于活跃状态
- `updateContactInfo()`：更新地址、城市、省份、邮编、电话、邮箱
- `updateOperatingInfo()`：更新营业时间和楼层数（楼层必须 > 0）

**工厂方法**：`Library.create(code, name)` 生成新 ID 并创建实例，默认 `active = true`。

---

#### 4.1.4 `Location.java` —— 值对象：物理位置

**DDD 角色**：**值对象（Value Object）**

Location 表示副本在图书馆内的精确物理位置，是一个经典的 `@Embeddable` 值对象。

```java
@Embeddable
public class Location {
    @Column(name = "library_code") private String libraryCode;
    private Integer floor;
    private String zone;
    private String aisle;
    private String shelf;
    private String position;
    @Column(name = "location_code") private String locationCode;
}
```

**值对象特征：**

1. **不可变性**：构造后字段不提供 setter（除 JPA 需要的 protected 无参构造器）
2. **相等性**：重写了 `equals()`/`hashCode()`，两个 Location 相等当且仅当 `libraryCode` 和 `locationCode` 相同
3. **自验证**：`validateLibraryCode()` 确保馆号非空，自动转大写

**位置编码规则**：

```
格式: {zone}{aisle}-{shelf}-{position}
示例: A01-A-001
默认值: zone=A, aisle=01, shelf=A, position=001
```

**两种构造方式：**

| 方法 | 用途 | 参数 |
|------|------|------|
| `Location.of(...)` | 精确定位 | 6 个详细位置参数 |
| `Location.simple(...)` | 简便定位 | 馆号 + 位置编码字符串（自动解析） |

`parseLocationCode()` 方法能从位置编码字符串反向解析出 zone、aisle、shelf、position 各部分。

---

#### 4.1.5 `enums/CopyStatus.java` —— 枚举：副本状态

```java
public enum CopyStatus {
    AVAILABLE,      // 可用
    BORROWED,       // 已借出
    RESERVED,       // 已预约
    DAMAGED,        // 已损坏
    UNDER_REPAIR,   // 维修中
    LOST,           // 已丢失
    REMOVED         // 已移除
}
```

**DDD 意义**：定义了副本的完整生命周期状态。7 个状态覆盖了副本从入库到退出的全部阶段。`REMOVED` 是终态，表示副本已永久下架。

---

#### 4.1.6 `enums/CopyCondition.java` —— 枚举：副本品相

```java
public enum CopyCondition {
    NEW,        // 全新
    GOOD,       // 良好
    FAIR,       // 一般
    POOR,       // 较差
    DAMAGED     // 已损坏
}
```

**DDD 意义**：品相与状态独立管理。状态描述"副本在做什么"（借出、维修中），品相描述"副本物理状况如何"。副本修复后品相自动设为 `GOOD`。

---

### 4.2 领域事件（domain/event）

> 领域事件表示领域中已经发生的、有业务意义的事情。在 DDD 中，事件用于实现聚合间和上下文间的解耦通信。

所有领域事件都继承自 `library-shared` 模块中的 `DomainEvent` 基类，该基类提供了 `eventId`、`eventType`、`occurredOn` 等通用属性。

#### 4.2.1 `InventoryCreatedEvent.java`

```java
public class InventoryCreatedEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final String libraryId;
    private final int initialCopyCount;
    private final LocalDateTime createdAt;
}
```

**触发时机**：`InventoryManagementService.createInitialInventory()` 成功创建新库存后发布。

**携带信息**：库存 ID、图书 ID、图书馆 ID 和初始副本数量——其他上下文（如分析上下文统计各馆库存）需要的信息。

---

#### 4.2.2 `CopyAddedEvent.java`

```java
public class CopyAddedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String bookId;
    private final String barcode;
    private final LocalDateTime addedAt;
}
```

**触发时机**：单个副本被添加到库存后发布。

**携带 barcode**：因为条码是副本的唯一物理标识，其他系统（如条码扫描系统）需要此信息。

---

#### 4.2.3 `CopiesBatchAddedEvent.java`

```java
public class CopiesBatchAddedEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final int count;
    private final LocalDateTime addedAt;
}
```

**触发时机**：批量添加副本后发布。

**与 CopyAddedEvent 的区别**：批量添加事件只报告数量，不逐个报告每个副本。这是性能与粒度的权衡——大批量添加时，逐个发布事件会带来不必要的开销。

---

#### 4.2.4 `CopyBorrowedEvent.java`

```java
public class CopyBorrowedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String bookId;
    private final String libraryId;
    private final LocalDateTime borrowedAt;
}
```

**触发时机**：副本被借出（checkout）后发布。

**携带 libraryId**：因为借出事件需要知道"哪本书在哪个图书馆被借了"，流上下文可能需要此信息。

---

#### 4.2.5 `CopyReturnedEvent.java`

```java
public class CopyReturnedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String bookId;
    private final String libraryId;
    private final LocalDateTime returnedAt;
}
```

**触发时机**：副本被归还后发布。

**与 CopyBorrowedEvent 对称设计**：字段结构完全一致，只是语义相反（borrowedAt vs returnedAt）。

---

#### 4.2.6 `CopyDamagedEvent.java`

```java
public class CopyDamagedEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String damageDescription;
    private final LocalDateTime damagedAt;
}
```

**触发时机**：副本被报告损坏后发布。

**不携带 bookId**：损坏事件主要面向库存管理，关注的是"哪个副本"和"损坏描述"，不需要图书维度信息。

---

#### 4.2.7 `CopyLostEvent.java`

```java
public class CopyLostEvent extends DomainEvent {
    private final String copyId;
    private final String inventoryId;
    private final String lostReason;
    private final LocalDateTime lostAt;
}
```

**触发时机**：副本被报告丢失后发布。

**与 CopyDamagedEvent 对称设计**：结构类似，但丢失事件关注的是"丢失原因"而非"损坏描述"。

---

#### 4.2.8 `LowStockAlertEvent.java`

```java
public class LowStockAlertEvent extends DomainEvent {
    private final String inventoryId;
    private final String bookId;
    private final int availableCopies;
    private final int threshold;
    private final LocalDateTime alertedAt;
}
```

**触发时机**：副本添加后检测到库存低于阈值时发布（在 `addCopyToInventory()` 和 `batchAddCopies()` 中检查）。

**DDD 意义**：这是一个**预警型领域事件**——不表示"已完成的业务动作"，而是"需要关注的业务状态"。通知上下文可以监听此事件，向管理员发送补货提醒。

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
- `errorCode`：每个子类定义自己的错误码字符串（如 `"INV-001"`）
- 继承 `RuntimeException`：是非受检异常，不强制调用方处理

---

#### 4.3.2 具体异常类

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `InventoryNotFoundException` | `INV-001` | 通过 ID 查找库存不存在 | 404 |
| `CopyNotFoundException` | `INV-002` | 通过 ID 查找副本不存在 | 404 |
| `LibraryNotFoundException` | `INV-003` | 通过 ID 查找图书馆不存在 | 404 |
| `DuplicateInventoryException` | `INV-004` | 同一本书在同一个图书馆重复创建库存 | 400 |
| `NoAvailableCopyException` | `INV-005` | 请求可用副本但库存中无可用副本 | 400 |
| `InvalidOperationException` | `INV-006` | 状态不允许的操作（如借出已借出的副本） | 400 |

每个异常类的实现都非常简洁，例如：

```java
public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(CopyInventoryId id) {
        super("INV-001", "Inventory not found: " + id);
    }
}
```

**错误码命名规则**：`INV-` 前缀 + 三位数字序号，与其他上下文的错误码命名空间隔离。

---

### 4.4 仓储接口（domain/repository）

> 仓储（Repository）是 DDD 中领域模型与数据存储之间的桥梁。本模块的仓储全部由 Spring Data JPA 自动代理实现，无需手写实现类。

#### 4.4.1 `CopyInventoryRepository.java`

```java
@Repository
public interface CopyInventoryRepository extends JpaRepository<CopyInventory, CopyInventoryId> {
    Optional<CopyInventory> findByBookIdAndLibraryId(String bookId, String libraryId);
    List<CopyInventory> findByBookId(String bookId);
    List<CopyInventory> findByLibraryId(String libraryId);
    List<CopyInventory> findByBookIdAndAvailableCopiesGreaterThan(String bookId, int minAvailable);
    boolean existsByBookIdAndLibraryId(String bookId, String libraryId);
    List<CopyInventory> findByAvailableCopiesLessThanEqual(int threshold);
}
```

**DDD 定位**：库存聚合的数据访问接口。

**方法分析：**

| 方法 | 用途 | 调用场景 |
|------|------|----------|
| `findByBookIdAndLibraryId()` | 定位某书在某馆的库存 | 创建前查重、查找可用副本 |
| `findByBookId()` | 查看某书在所有馆的库存 | 库存总览查询 |
| `findByLibraryId()` | 查看某馆所有库存 | 馆内库存管理 |
| `existsByBookIdAndLibraryId()` | 判断库存是否已存在 | 创建前去重 |
| `findByBookIdAndAvailableCopiesGreaterThan()` | 查找有可用副本的库存 | 跨馆调拨时查找可借阅的馆 |
| `findByAvailableCopiesLessThanEqual()` | 查找低库存记录 | 低库存报表 |

---

#### 4.4.2 `BookCopyRepository.java`

```java
@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, CopyId> {
    Optional<BookCopy> findByBarcode(String barcode);
    List<BookCopy> findByInventoryId(String inventoryId);
    List<BookCopy> findByStatus(CopyStatus status);
    List<BookCopy> findByStatusIn(List<CopyStatus> statuses);
    boolean existsByBarcode(String barcode);
}
```

**DDD 定位**：副本实体的数据访问接口。

**方法分析：**

| 方法 | 用途 |
|------|------|
| `findByBarcode()` | 通过条码查找（条码是物理标识） |
| `findByInventoryId()` | 查看某个库存下的所有副本 |
| `findByStatus()` / `findByStatusIn()` | 按状态筛选副本（如查所有损坏的副本） |
| `existsByBarcode()` | 条码唯一性校验 |

---

#### 4.4.3 `LibraryRepository.java`

```java
@Repository
public interface LibraryRepository extends JpaRepository<Library, LibraryId> {
    Optional<Library> findByCode(String code);
    List<Library> findByActiveTrue();
    boolean existsByCode(String code);
}
```

**DDD 定位**：图书馆聚合的数据访问接口。

**方法分析：**

| 方法 | 用途 |
|------|------|
| `findByCode()` | 通过馆号查找（馆号是业务唯一标识） |
| `findByActiveTrue()` | 只查询活跃的图书馆 |
| `existsByCode()` | 馆号唯一性校验 |

---

### 4.5 领域服务（domain/service）

> 领域服务用于承载**不适合放在单个实体或值对象中的业务逻辑**，例如跨聚合的协调、仓储调用和领域事件发布。

#### `InventoryManagementService.java`

**DDD 角色**：**领域服务** —— 库存上下文的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class InventoryManagementService {
    private final CopyInventoryRepository inventoryRepository;
    private final BookCopyRepository copyRepository;
    private final LibraryRepository libraryRepository;
    private final DomainEventPublisher eventPublisher;
}
```

**职责：**

| 方法 | 业务逻辑 |
|------|----------|
| `createInitialInventory()` | 验证图书馆存在且活跃 → 检查库存不重复 → 创建 CopyInventory → 添加初始副本 → 发布 InventoryCreatedEvent |
| `addCopyToInventory()` | 查找库存 → 生成唯一条码 → 调用 `inventory.addCopy()` → 保存 → 发布 CopyAddedEvent → 检查低库存预警 |
| `batchAddCopies()` | 查找库存 → 调用 `inventory.addCopies(count)` → 保存 → 发布 CopiesBatchAddedEvent → 检查低库存预警 |
| `checkoutCopy()` | 查找副本 → 记录旧状态 → 调用 `copy.markAsBorrowed()` → 同步库存计数 → 保存 → 发布 CopyBorrowedEvent |
| `returnCopy()` | 查找副本 → 记录旧状态 → 调用 `copy.markAsReturned()` → 同步库存计数 → 保存 → 发布 CopyReturnedEvent |
| `reportCopyDamage()` | 查找副本 → 记录旧状态 → 调用 `copy.markAsDamaged()` → 同步库存计数 → 保存 → 发布 CopyDamagedEvent |
| `reportCopyLoss()` | 查找副本 → 记录旧状态 → 调用 `copy.markAsLost()` → 同步库存计数 → 保存 → 发布 CopyLostEvent |
| `getInventoryOverview()` | 查看某书在所有馆的库存情况 |
| `findAvailableCopy()` | 查找某书在某馆的可用副本 |

**DDD 设计要点：**

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写操作方法覆盖为 `@Transactional`
2. **事件发布**：状态变更后发布领域事件，实现与其他上下文的解耦
3. **跨聚合协调**：`checkoutCopy()` 需要同时操作 BookCopy（状态变更）和 CopyInventory（计数同步），这是典型的"需要领域服务"的场景——两个实体不能直接互相访问
4. **保护聚合不变量**：状态转换规则（如"只有 AVAILABLE 才能借出"）编码在 BookCopy 实体中，领域服务只负责协调
5. **低库存预警**：在添加副本后检查 `inventory.isLowStock()`，如触发则发布 `LowStockAlertEvent`

**状态变更 + 计数同步模式**：

```java
// 借出副本的标准流程
CopyStatus oldStatus = copy.getStatus();       // 1. 记录旧状态
copy.markAsBorrowed();                          // 2. 变更副本状态（实体内部校验）
inventory.onCopyStatusChanged(oldStatus, copy.getStatus()); // 3. 同步库存计数
inventoryRepository.save(inventory);            // 4. 持久化库存
copyRepository.save(copy);                      // 5. 持久化副本
eventPublisher.publish(new CopyBorrowedEvent(...)); // 6. 发布领域事件
```

这个六步流程在借出、归还、损坏、丢失四个操作中完全一致，体现了**业务操作的统一模式**。

---

## 5. 应用层（Application Layer）

> 应用层是 DDD 中的**"编排层"**，它不包含业务逻辑，而是协调领域对象完成用例。它的职责是：接收外部请求 → 转换参数 → 调用领域服务 → 转换输出 → 返回结果。

### 5.1 应用服务（application/service）

#### `InventoryApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional(readOnly = true)
public class InventoryApplicationService {
    private final InventoryManagementService inventoryManagementService;
    private final CopyInventoryRepository inventoryRepository;
}
```

**InventoryManagementService vs InventoryApplicationService 的区别：**

| 维度 | InventoryManagementService（领域服务） | InventoryApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（Location, BookId） | 外部格式（String, Command 对象） |
| 输出 | 领域对象（CopyInventory, BookCopy） | DTO（CopyInventoryDTO, BookCopyDTO） |
| 职责 | 业务规则校验和领域事件发布 | 参数转换和结果转换 |
| 调用者 | 应用服务 | 控制器 |

**典型方法流程**（以 `createInventory` 为例）：

```
Controller 调用 → InventoryApplicationService.createInventory(CreateInventoryCommand)
  1. buildLocation()           // 将 6 个 String/Integer 参数组装为 Location 值对象
  2. 调用 inventoryManagementService.createInitialInventory(...)
  3. CopyInventoryDTO.fromDomain(inventory)  // 领域对象 → DTO
  4. 返回 CopyInventoryDTO
```

**`buildLocation()` 辅助方法**：将 Command 中的离散位置参数（libraryCode, floor, zone, aisle, shelf, position）组装为 `Location` 值对象。这是应用层"类型转换"职责的典型体现。

---

#### `LibraryApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional(readOnly = true)
public class LibraryApplicationService {
    private final LibraryRepository libraryRepository;
}
```

**特点**：直接操作仓储，不需要经过领域服务。因为图书馆的 CRUD 操作没有复杂的跨聚合协调需求，应用服务自身足以完成编排。

**方法：**

| 方法 | 说明 |
|------|------|
| `createLibrary()` | 创建图书馆 → 设置联系信息 → 设置运营信息 → 保存 → 转 DTO |
| `getLibrary()` | 按 ID 查询 → 转 DTO |
| `getAllLibraries()` | 查询全部 → 转 DTO 列表 |
| `getActiveLibraries()` | 只查询活跃图书馆 → 转 DTO 列表 |
| `updateLibrary()` | 查找 → 更新联系信息和运营信息 → 保存 → 转 DTO |
| `deactivateLibrary()` | 查找 → 调用 `library.deactivate()` → 保存 |
| `activateLibrary()` | 查找 → 调用 `library.activate()` → 保存 |

---

### 5.2 命令对象（application/command）

> 命令（Command）是写操作的输入对象，封装了执行某个操作所需的全部参数。

#### `CreateInventoryCommand.java`

```java
@Data @Builder
public class CreateInventoryCommand {
    @NotBlank private String bookId;
    @NotBlank private String libraryId;
    @Min(0) private int initialCopyCount;
    private String libraryCode;
    private Integer floor;
    private String zone, aisle, shelf, position;
    private BigDecimal cost;
    private String createdBy;
}
```

**设计要点：**

- 使用 Lombok `@Data` + `@Builder`（而非 Java record），因为 Controller 需要 `setInventoryId()` 来覆盖路径参数
- `@NotBlank` 和 `@Min` 是 Bean Validation 注解，在 Controller 层由框架自动验证
- 位置信息（libraryCode, floor, zone, aisle, shelf, position）作为可选参数，由应用服务的 `buildLocation()` 组装

---

#### `AddCopyCommand.java`

```java
@Data @Builder
public class AddCopyCommand {
    @NotBlank private String inventoryId;
    private String libraryCode;
    private Integer floor;
    private String zone, aisle, shelf, position;
    private String acquisitionMethod;
    private BigDecimal cost;
}
```

与 CreateInventoryCommand 类似，但 `inventoryId` 由 Controller 通过路径参数覆盖设置。

---

#### `BatchAddCopiesCommand.java`

```java
@Data @Builder
public class BatchAddCopiesCommand {
    @NotBlank private String inventoryId;
    @Min(1) private int count;
    private String libraryCode;
    private Integer floor;
    private String zone, aisle, shelf, position;
    private String acquisitionMethod;
    private BigDecimal cost;
}
```

**与 AddCopyCommand 的区别**：增加了 `count` 字段（最小值为 1），用于批量添加。

---

#### `CreateLibraryCommand.java`

```java
@Data @Builder
public class CreateLibraryCommand {
    @NotBlank @Size(max = 20) private String code;
    @NotBlank @Size(max = 200) private String name;
    private String address, city, province, postalCode, phone, email;
    private String openingHours;
    private Integer totalFloors;
}
```

**设计要点**：`code` 和 `name` 是必填项（`@NotBlank`），其他联系方式和运营信息全部可选，支持渐进式完善图书馆资料。

---

### 5.3 数据传输对象（application/dto）

#### `ApiResponse.java` —— 统一响应信封

```java
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String errorCode;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(String errorCode, String error) { ... }
}
```

**设计要点：**

- 泛型 `<T>` 支持任意数据类型
- `@JsonInclude(NON_NULL)`：空字段不序列化，成功时 `error`/`errorCode` 不出现，失败时 `data` 不出现
- 成功时：`success=true, data=实际数据`
- 失败时：`success=false, errorCode=错误码, error=错误信息`

---

#### `CopyInventoryDTO.java`

```java
@Data @Builder
public class CopyInventoryDTO {
    private String id;
    private String bookId;
    private String libraryId;
    private String libraryCode;
    private Integer totalCopies;
    private Integer availableCopies;
    private Double availabilityRate;
    private Boolean lowStock;
    private List<BookCopyDTO> copies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CopyInventoryDTO fromDomain(CopyInventory inventory) { ... }
}
```

**关键设计**：`fromDomain()` 将 CopyInventory 聚合根展平为 DTO，包括：
- 将 `CopyInventoryId` 转为 String
- 将 `getAvailabilityRate()` 计算结果转为 Double
- 将 `isLowStock()` 布尔判断结果包含在 DTO 中
- 将聚合内所有副本转为 `List<BookCopyDTO>`

---

#### `BookCopyDTO.java`

```java
@Data @Builder
public class BookCopyDTO {
    private String id;
    private String inventoryId;
    private String barcode;
    private String locationCode;
    private String locationDescription;
    private String status;
    private String condition;
    // ... 采购信息、借阅统计、损坏/丢失信息
}
```

**组装器设计**：`fromDomain(BookCopy)` 将 BookCopy 实体转为 DTO，其中：
- `Location` 值对象被展平为 `locationCode`（编码）和 `locationDescription`（人类可读描述）
- `CopyStatus` 枚举转为 `.name()` 字符串
- `CopyCondition` 枚举转为 `.name()` 字符串（可为 null）

---

#### `LibraryDTO.java`

```java
@Data @Builder
public class LibraryDTO {
    private String id;
    private String code;
    private String name;
    private String address, city, province, postalCode, phone, email;
    private String openingHours;
    private Integer totalFloors;
    private boolean active;
    private LocalDateTime createdAt, updatedAt;
}
```

简洁的字段映射，`fromDomain(Library)` 将 Library 聚合根转为扁平 DTO。

---

## 6. 基础设施层（Infrastructure Layer）

本模块**没有独立的 infrastructure 子包**。原因如下：

1. **仓储实现**：所有三个仓储接口（`CopyInventoryRepository`、`BookCopyRepository`、`LibraryRepository`）都继承自 `JpaRepository`，Spring Data JPA 在运行时自动生成代理类实现，无需手写实现类
2. **事件发布**：通过共享模块的 `DomainEventPublisher` 接口实现，具体发布机制（Spring ApplicationEventPublisher + 可选 Kafka）在 `library-shared` 模块中统一配置
3. **配置**：`JpaConfig` 放在 `config` 包而非 `infrastructure` 包，因为它是全局性配置

这种设计是**简单优先**原则的体现——当 Spring Data JPA 的自动实现已经满足需求时，不需要为了"遵循 DDD 模板"而引入不必要的抽象层。

---

## 7. 接口层（Interfaces Layer）

> 接口层是系统与外部世界的边界。它接收 HTTP 请求，调用应用服务，返回 HTTP 响应。**不包含任何业务逻辑。**

### 7.1 `InventoryController.java`

```java
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {
    private final InventoryApplicationService inventoryService;
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/inventory/inventories` | 创建库存 | 命令 |
| `POST` | `/api/inventory/inventories/{inventoryId}/copies` | 添加单个副本 | 命令 |
| `POST` | `/api/inventory/inventories/{inventoryId}/copies/batch` | 批量添加副本 | 命令 |
| `POST` | `/api/inventory/copies/{copyId}/checkout` | 借出副本 | 命令（状态变更） |
| `POST` | `/api/inventory/copies/{copyId}/return` | 归还副本 | 命令（状态变更） |
| `POST` | `/api/inventory/copies/{copyId}/damage` | 报告损坏 | 命令（状态变更） |
| `POST` | `/api/inventory/copies/{copyId}/loss` | 报告丢失 | 命令（状态变更） |
| `GET` | `/api/inventory/inventories/{inventoryId}` | 查看库存详情 | 查询 |
| `GET` | `/api/inventory/books/{bookId}/overview` | 查看图书库存总览 | 查询 |

**设计要点：**

- **CQRS 体现**：POST 是命令（写），GET 是查询（读）；状态变更操作（checkout/return/damage/loss）使用 POST 而非 PUT
- **资源嵌套路由**：`/inventories/{id}/copies` 体现了聚合根与聚合内实体的层级关系
- **统一响应**：所有端点返回 `ApiResponse<T>` 信封
- **Swagger 注解**：`@Operation(summary = ...)` 生成 API 文档
- **内部请求类**：`DamageReportRequest` 和 `LossReportRequest` 作为 Controller 的静态内部类定义，因为这些结构只在 Controller 层使用

---

### 7.2 `LibraryController.java`

```java
@RestController
@RequestMapping("/api/inventory/libraries")
@Tag(name = "Library", description = "Library management APIs")
```

| HTTP 方法 | 路径 | 操作 |
|-----------|------|------|
| `POST` | `/api/inventory/libraries` | 创建图书馆 |
| `GET` | `/api/inventory/libraries` | 获取所有图书馆 |
| `GET` | `/api/inventory/libraries/active` | 获取所有活跃图书馆 |
| `GET` | `/api/inventory/libraries/{libraryId}` | 获取图书馆详情 |
| `PUT` | `/api/inventory/libraries/{libraryId}` | 更新图书馆信息 |
| `POST` | `/api/inventory/libraries/{libraryId}/deactivate` | 停用图书馆 |
| `POST` | `/api/inventory/libraries/{libraryId}/activate` | 启用图书馆 |

**注意**：启用/停用使用 POST 而非 PUT，因为这是"执行一个动作"而非"替换资源状态"。

---

### 7.3 `GlobalExceptionHandler.java` —— 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InventoryNotFoundException.class)  → 404
    @ExceptionHandler(CopyNotFoundException.class)       → 404
    @ExceptionHandler(LibraryNotFoundException.class)    → 404
    @ExceptionHandler(DomainException.class)             → 400
    @ExceptionHandler(MethodArgumentNotValidException.class) → 400
    @ExceptionHandler(Exception.class)                   → 500
}
```

**DDD 定位**：**跨切面（Cross-Cutting Concern）**，在接口层统一处理所有异常。

**分层异常处理策略：**

| 异常类型 | HTTP 状态 | 说明 |
|----------|-----------|------|
| `InventoryNotFoundException` | 404 NOT_FOUND | 库存不存在 |
| `CopyNotFoundException` | 404 NOT_FOUND | 副本不存在 |
| `LibraryNotFoundException` | 404 NOT_FOUND | 图书馆不存在 |
| `DomainException`（其他） | 400 BAD_REQUEST | 业务规则违反 |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | 请求参数校验失败 |
| `Exception` | 500 INTERNAL_SERVER_ERROR | 兜底异常，记录日志但不暴露详情 |

**返回格式统一**：所有异常都包装成 `ApiResponse.error(errorCode, message)`，前端只需处理一种响应格式。

---

## 8. 层间调用流程

以 **"创建库存"** 为例，展示一次完整请求的层间调用链：

```
HTTP POST /api/inventory/inventories
  │
  ▼
┌─────────────────────────────────────────────────────────────┐
│ InventoryController.createInventory(CreateInventoryCommand)  │  ← interfaces 层
│   @Valid 触发 Bean Validation                                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ InventoryApplicationService.createInventory(command)         │  ← application 层
│   1. buildLocation(...)          // 6个参数 → Location 值对象 │
│   2. 调用 inventoryManagementService                         │
│   3. CopyInventoryDTO.fromDomain(inventory)  // 领域对象→DTO │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ InventoryManagementService.createInitialInventory(...)       │  ← domain 层（服务）
│   1. libraryRepository.findById()   → 验证图书馆存在且活跃    │
│   2. existsByBookIdAndLibraryId()   → 检查库存不重复          │
│   3. CopyInventory.create(...)      → 调用聚合根工厂方法       │
│   4. inventory.addCopies(count)     → 添加初始副本             │
│   5. inventoryRepository.save()     → 持久化                   │
│   6. eventPublisher.publish(InventoryCreatedEvent)            │
└───────┬───────────────────────────────────┬─────────────────┘
        │                                   │
        ▼                                   ▼
┌──────────────────┐  ┌──────────────────────────────────────┐
│ CopyInventory     │  │ DomainEventPublisher                  │  ← shared 模块
│ Repository        │  │   → Spring ApplicationEventPublisher  │    本地事件
│ (Spring Data JPA  │  │   → KafkaTemplate (可选)              │    远程事件
│  自动实现)        │  └──────────────────────────────────────┘
└──────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ PostgreSQL                                                    │  ← 数据库
│ INSERT INTO copy_inventories (id, book_id, library_id, ...)   │
│ INSERT INTO book_copies (id, inventory_id, barcode, ...)      │
└──────────────────────────────────────────────────────────────┘
```

以 **"借出副本"** 为例，展示跨聚合协调的调用链：

```
HTTP POST /api/inventory/copies/{copyId}/checkout
  │
  ▼
InventoryController → InventoryApplicationService.checkoutCopy(copyId)
  │
  ▼
InventoryManagementService.checkoutCopy(copyId)
  1. copyRepository.findById(copyId)           → 查找副本
  2. CopyStatus oldStatus = copy.getStatus()    → 记录旧状态
  3. copy.markAsBorrowed()                      → 状态变更（实体内部校验规则）
  4. inventoryRepository.findById(...)           → 查找所属库存
  5. inventory.onCopyStatusChanged(old, new)     → 同步 availableCopies 计数
  6. inventoryRepository.save(inventory)         → 持久化库存
  7. copyRepository.save(copy)                   → 持久化副本
  8. eventPublisher.publish(CopyBorrowedEvent)   → 发布领域事件
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `InventoryApplication.java` | Bootstrap | 启动类 | Spring Boot 入口 |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/CopyInventory.java` | Domain | **聚合根** | 图书库存管理与副本协调 |
| 4 | `domain/model/BookCopy.java` | Domain | 聚合内实体 | 副本状态生命周期管理 |
| 5 | `domain/model/Library.java` | Domain | **聚合根** | 图书馆信息与状态管理 |
| 6 | `domain/model/Location.java` | Domain | **值对象** | 物理位置编码与解析 |
| 7 | `domain/model/enums/CopyStatus.java` | Domain | 枚举 | 副本状态枚举（7种） |
| 8 | `domain/model/enums/CopyCondition.java` | Domain | 枚举 | 副本品相枚举（5种） |
| 9 | `domain/event/InventoryCreatedEvent.java` | Domain | 领域事件 | 库存已创建 |
| 10 | `domain/event/CopyAddedEvent.java` | Domain | 领域事件 | 副本已添加 |
| 11 | `domain/event/CopiesBatchAddedEvent.java` | Domain | 领域事件 | 副本批量添加 |
| 12 | `domain/event/CopyBorrowedEvent.java` | Domain | 领域事件 | 副本已借出 |
| 13 | `domain/event/CopyReturnedEvent.java` | Domain | 领域事件 | 副本已归还 |
| 14 | `domain/event/CopyDamagedEvent.java` | Domain | 领域事件 | 副本已损坏 |
| 15 | `domain/event/CopyLostEvent.java` | Domain | 领域事件 | 副本已丢失 |
| 16 | `domain/event/LowStockAlertEvent.java` | Domain | 领域事件 | 低库存预警 |
| 17 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 18 | `domain/exception/InventoryNotFoundException.java` | Domain | 异常 | 库存不存在 |
| 19 | `domain/exception/CopyNotFoundException.java` | Domain | 异常 | 副本不存在 |
| 20 | `domain/exception/LibraryNotFoundException.java` | Domain | 异常 | 图书馆不存在 |
| 21 | `domain/exception/DuplicateInventoryException.java` | Domain | 异常 | 库存重复 |
| 22 | `domain/exception/NoAvailableCopyException.java` | Domain | 异常 | 无可用副本 |
| 23 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作 |
| 24 | `domain/repository/CopyInventoryRepository.java` | Domain | 仓储接口 | 库存数据访问 |
| 25 | `domain/repository/BookCopyRepository.java` | Domain | 仓储接口 | 副本数据访问 |
| 26 | `domain/repository/LibraryRepository.java` | Domain | 仓储接口 | 图书馆数据访问 |
| 27 | `domain/service/InventoryManagementService.java` | Domain | **领域服务** | 库存业务编排与事件发布 |
| 28 | `application/service/InventoryApplicationService.java` | Application | **应用服务** | 库存用例编排 |
| 29 | `application/service/LibraryApplicationService.java` | Application | **应用服务** | 图书馆用例编排 |
| 30 | `application/command/CreateInventoryCommand.java` | Application | 命令 | 创建库存入参 |
| 31 | `application/command/AddCopyCommand.java` | Application | 命令 | 添加副本入参 |
| 32 | `application/command/BatchAddCopiesCommand.java` | Application | 命令 | 批量添加副本入参 |
| 33 | `application/command/CreateLibraryCommand.java` | Application | 命令 | 创建图书馆入参 |
| 34 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 35 | `application/dto/CopyInventoryDTO.java` | Application | DTO | 库存数据传输 |
| 36 | `application/dto/BookCopyDTO.java` | Application | DTO | 副本数据传输 |
| 37 | `application/dto/LibraryDTO.java` | Application | DTO | 图书馆数据传输 |
| 38 | `interfaces/rest/InventoryController.java` | Interfaces | REST 控制器 | 库存 API |
| 39 | `interfaces/rest/LibraryController.java` | Interfaces | REST 控制器 | 图书馆 API |
| 40 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：40 个源文件**
