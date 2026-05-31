# Library Catalog 模块 —— DDD 代码结构详解

> 本文档详细讲解 `library-catalog` 模块中每一个源文件（不含测试文件）的设计意图、DDD 定位和实现细节。

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

`library-catalog` 是图书馆管理系统的**编目上下文（Catalog Context）**，负责管理图书的核心元数据：

| 职责 | 说明 |
|------|------|
| 图书管理 | 创建、更新、发布、取消发布、删除图书 |
| 作者管理 | 作者信息的 CRUD |
| 出版社管理 | 出版社信息的 CRUD 与搜索 |
| 分类管理 | 树形分类体系的构建与维护 |
| ISBN 验证 | ISBN-10/ISBN-13 格式验证与转换 |
| 领域事件 | 图书状态变更时发布事件通知其他上下文 |

运行端口：`8081`，基础路径：`/api/catalog/*`

---

## 2. DDD 分层架构总览

```
library-catalog/src/main/java/com/library/catalog/
│
├── CatalogApplication.java              ← Spring Boot 启动类
├── config/                              ← 基础设施配置
│   └── JpaConfig.java
│
├── domain/                              ← 领域层（纯业务逻辑，无外部依赖）
│   ├── model/                           ← 聚合根、实体、值对象
│   │   ├── Book.java                    ← 聚合根：图书
│   │   ├── BookAuthor.java              ← 实体：图书-作者关联
│   │   ├── Author.java                  ← 聚合根：作者
│   │   ├── Category.java                ← 聚合根：分类（树形结构）
│   │   ├── Publisher.java               ← 聚合根：出版社
│   │   ├── ISBN.java                    ← 值对象：国际标准书号
│   │   └── enums/                       ← 枚举
│   │       ├── BookStatus.java
│   │       └── AuthorRole.java
│   ├── event/                           ← 领域事件
│   │   ├── BookCreatedEvent.java
│   │   ├── BookUpdatedEvent.java
│   │   ├── BookPublishedEvent.java
│   │   └── BookDeletedEvent.java
│   ├── exception/                       ← 领域异常（含错误码）
│   │   ├── DomainException.java         ← 异常基类
│   │   ├── BookNotFoundException.java
│   │   ├── AuthorNotFoundException.java
│   │   ├── CategoryNotFoundException.java
│   │   ├── PublisherNotFoundException.java
│   │   ├── DuplicateISBNException.java
│   │   ├── DuplicateAuthorException.java
│   │   ├── InvalidISBNException.java
│   │   └── InvalidOperationException.java
│   ├── repository/                      ← 仓储接口（由基础设施层实现）
│   │   ├── BookRepository.java
│   │   ├── CustomBookRepository.java
│   │   ├── AuthorRepository.java
│   │   ├── CategoryRepository.java
│   │   └── PublisherRepository.java
│   └── service/                         ← 领域服务
│       ├── BookManagementService.java
│       ├── AuthorManagementService.java
│       ├── CategoryManagementService.java
│       ├── PublisherManagementService.java
│       └── ISBNValidationService.java
│
├── application/                         ← 应用层（编排、协调）
│   ├── service/
│   │   └── BookApplicationService.java  ← 应用服务：编排 Book 用例
│   ├── command/                         ← 写操作入参
│   │   ├── CreateBookCommand.java
│   │   └── UpdateBookCommand.java
│   ├── query/                           ← 读操作入参
│   │   └── BookSearchCriteria.java
│   └── dto/                             ← 输出传输对象
│       ├── ApiResponse.java
│       ├── BookDTO.java
│       ├── AuthorDTO.java
│       ├── CategoryDTO.java
│       └── PublisherDTO.java
│
├── infrastructure/                      ← 基础设施层（技术实现）
│   ├── persistence/jpa/
│   │   └── BookRepositoryImpl.java      ← 复杂查询实现（Criteria API）
│   └── messaging/
│       └── CatalogDomainEventPublisher.java  ← 事件发布器（Spring + Kafka）
│
└── interfaces/                          ← 接口层（REST API）
    └── rest/
        ├── BookController.java
        ├── AuthorController.java
        ├── CategoryController.java
        ├── PublisherController.java
        └── GlobalExceptionHandler.java
```

**DDD 分层依赖规则：**

```
interfaces → application → domain ← infrastructure
                                  （实现 domain 的 repository 接口）
```

- **domain 层**不依赖任何其他层，纯 Java + JPA 注解
- **application 层**依赖 domain 层（调用领域服务和仓储接口）
- **infrastructure 层**依赖 domain 层（实现仓储接口）
- **interfaces 层**依赖 application 层（调用应用服务）

---

## 3. 引导与配置层

### `CatalogApplication.java`

```java
@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
```

**DDD 定位**：Spring Boot 入口类，启动整个编目上下文的 Spring 容器。`@SpringBootApplication` 自动扫描 `com.library.catalog` 包下所有组件。

---

### `config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**DDD 定位**：基础设施配置。`@EnableJpaAuditing` 启用 JPA 审计功能，让实体上的 `@CreatedDate`、`@LastModifiedDate`、`@CreatedBy`、`@LastModifiedBy` 注解自动填充创建/修改时间和操作人。

**为什么放在 config 包而不是 infrastructure？** 这是一个全局性的 JPA 配置，不属于某个具体的持久化实现，所以独立放在 config 包。

---

## 4. 领域层（Domain Layer）

> 领域层是 DDD 的核心。这里包含纯业务逻辑，不依赖任何外部框架（除了 JPA 注解作为持久化映射）。

### 4.1 领域模型（domain/model）

#### 4.1.1 `Book.java` —— 聚合根：图书

**DDD 角色**：**聚合根（Aggregate Root）**

Book 是编目上下文中最重要的聚合根，管理图书的完整生命周期。

**核心设计要点：**

| 设计点 | 实现方式 | DDD 意义 |
|--------|----------|----------|
| 标识 | `@EmbeddedId BookId id` | 使用共享模块的强类型 ID，而非裸 String |
| 乐观锁 | `@Version Long version` | 防止并发修改冲突 |
| 审计字段 | `@CreatedDate`, `@LastModifiedDate` 等 | 自动记录创建/修改时间和操作人 |
| 构造控制 | `protected Book()` + `private Book(...)` | JPA 要求无参构造器，但 `protected` 防止外部直接实例化 |
| 工厂方法 | `Book.create(...)` | 静态工厂方法，确保创建时总是生成新 ID 和正确的初始状态 |
| 值对象嵌入 | `@Embedded ISBN isbn` | ISBN 作为值对象嵌入，验证逻辑内聚 |
| 聚合内实体 | `@OneToMany List<BookAuthor> authors` | 作者关联是聚合内部的一部分，级联保存 |
| 集合保护 | `Collections.unmodifiableList(authors)` | 返回不可变集合，防止外部绕过领域逻辑直接修改 |

**状态机：图书生命周期**

```
DRAFT ──publish()──→ PUBLISHED ──unpublish()──→ UNPUBLISHED
  │                                                 │
  └──delete()──→ DELETED                ──publish()──→ PUBLISHED
                                          │
  PUBLISHED 不能直接 delete，必须先 unpublish
```

**业务规则（编码在领域模型中）：**

- `publish()`：只有 DRAFT 或 UNPUBLISHED 状态才能发布；必须有至少一个作者、一个出版社、一个分类
- `delete()`：已发布的图书不能直接删除，必须先取消发布
- `addAuthor()`：不允许重复添加同一个作者
- `updateBasicInfo()`：已删除的图书不能修改

---

#### 4.1.2 `BookAuthor.java` —— 实体：图书-作者关联

**DDD 角色**：**聚合内实体（Entity within Aggregate）**

BookAuthor 是 Book 聚合内部的实体，表示"某本书的某位作者"这一关联关系。

```java
@Entity
@Table(name = "book_authors")
@IdClass(BookAuthor.BookAuthorId.class)  // 复合主键
public class BookAuthor {
    @Id private String bookId;
    @Id private String authorId;
    private String authorName;   // 冗余存储作者姓名（避免每次关联查询）
    private AuthorRole role;     // 角色枚举
    private int sortOrder;       // 排序序号
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| 复合主键 | `@IdClass(BookAuthorId.class)`，由 bookId + authorId 组成 |
| 冗余字段 | `authorName` 存储了作者姓名的快照，这是 DDD 中的**值对象冗余**策略——避免每次展示图书时关联查询作者表 |
| 排序支持 | `sortOrder` 字段维护作者在图书中的排列顺序 |
| 生命周期 | 由 Book 聚合根通过级联（`cascade = CascadeType.ALL, orphanRemoval = true`）管理，不能独立存在 |

---

#### 4.1.3 `Author.java` —— 聚合根：作者

**DDD 角色**：**聚合根（Aggregate Root）**

Author 是独立的聚合根，管理作者的个人信息。

```java
@Entity
@Table(name = "authors")
public class Author {
    @EmbeddedId private AuthorId id;
    private String name;          // 必填，≤200字符
    private String biography;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String nationality;
    @Version private Long version;
    // 审计字段...
}
```

**业务规则：**

- `setName()`：姓名不能为空，不能超过 200 字符
- `setDates()`：死亡日期不能早于出生日期
- `updatePersonalInfo()`：支持部分更新（null 值不覆盖）

**工厂方法模式**：`Author.create(...)` 生成新 ID 并创建实例，确保 ID 总是自动生成的。

---

#### 4.1.4 `Category.java` —— 聚合根：分类（树形结构）

**DDD 角色**：**聚合根（Aggregate Root）+ 自引用树形结构**

Category 实现了一个经典的**树形分类体系**，通过 `parent`/`children` 自引用关系实现。

```java
@Entity
@Table(name = "categories")
public class Category {
    @EmbeddedId private CategoryId id;
    private String name;
    private String description;
    private int level;                              // 层级深度（根=0）
    @ManyToOne(fetch = LAZY) private Category parent;
    @OneToMany(cascade = ALL, orphanRemoval = true) private List<Category> children;
}
```

**设计要点：**

| 设计点 | 说明 |
|--------|------|
| `level` 字段 | 记录分类在树中的深度，根分类 level=0，子分类 level=parent.level+1 |
| `createRoot()` | 静态工厂方法，创建根分类（无父节点，level=0） |
| `addChild()` | 实例方法，在当前分类下添加子分类，自动设置 level |
| `checkCircularReference()` | 防止循环引用（当前未在 addChild 中调用，但预留了检测逻辑） |
| 集合保护 | `getChildren()` 返回不可变列表 |

---

#### 4.1.5 `Publisher.java` —— 聚合根：出版社

**DDD 角色**：**聚合根（Aggregate Root）**

Publisher 是相对简单的聚合根，管理出版社的联系信息。

```java
@Entity
@Table(name = "publishers")
public class Publisher {
    @EmbeddedId private PublisherId id;
    private String name;        // 必填，≤200字符
    private String description;
    private String address;
    private String phone;
    private String email;
    private String website;
    @Version private Long version;
}
```

**设计特点**：结构相对简单，没有复杂的业务规则，但遵循了与其他聚合根相同的设计模式（工厂方法、字段校验、乐观锁、审计字段）。

---

#### 4.1.6 `ISBN.java` —— 值对象：国际标准书号

**DDD 角色**：**值对象（Value Object）**

ISBN 是一个典型的值对象实现，具有以下特征：

```java
@Embeddable
public class ISBN {
    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    private String value;

    // 构造时执行完整验证
    public ISBN(String value) {
        String cleaned = cleanISBN(value);         // 去掉横杠和空格
        validateCleanedISBN(cleaned);              // 格式和校验位验证
        this.value = cleaned;
    }
}
```

**值对象的三大特征：**

1. **不可变性**：构造后 `value` 不可修改（无 setter）
2. **相等性**：重写了 `equals()`/`hashCode()`，两个 ISBN 相等当且仅当 `value` 相同
3. **自验证**：构造函数中执行完整的验证逻辑

**验证逻辑：**

- `cleanISBN()`：去除空格和连字符
- `validateISBN10()`：验证 ISBN-10 格式（9位数字 + 1位校验码/X）并检查加权和校验
- `validateISBN13()`：验证 ISBN-13 格式（13位数字）并检查加权校验位

**附加方法：**

- `isISBN10()` / `isISBN13()`：判断 ISBN 版本
- `getFormattedValue()`：返回带连杠的格式化字符串

**`@Embeddable` 注解**：ISBN 作为值对象嵌入到 Book 实体中（`@Embedded`），不会创建独立的数据库表，而是将 `isbn` 列直接放在 `books` 表中。

---

#### 4.1.7 `enums/BookStatus.java` —— 枚举：图书状态

```java
public enum BookStatus {
    DRAFT,        // 草稿（新创建的默认状态）
    PUBLISHED,    // 已发布
    UNPUBLISHED,  // 已取消发布
    DELETED       // 已删除（软删除）
}
```

**DDD 意义**：定义了图书的生命周期状态。注意 `DELETED` 是软删除状态，不是从数据库物理删除。

---

#### 4.1.8 `enums/AuthorRole.java` —— 枚举：作者角色

```java
public enum AuthorRole {
    AUTHOR,       // 主作者
    CO_AUTHOR,    // 合著者
    EDITOR,       // 编辑
    TRANSLATOR,   // 译者
    CONTRIBUTOR   // 贡献者
}
```

**DDD 意义**：精确定义作者与图书的关系类型，比简单的"作者"字段更具表现力。

---

### 4.2 领域事件（domain/event）

> 领域事件表示领域中已经发生的、有业务意义的事情。在 DDD 中，事件用于实现聚合间和上下文间的解耦通信。

所有领域事件都继承自 `library-shared` 模块中的 `DomainEvent` 基类，该基类提供了 `eventId`、`eventType`、`occurredOn` 等通用属性。

#### 4.2.1 `BookCreatedEvent.java`

```java
public class BookCreatedEvent extends DomainEvent {
    private final String bookId;
    private final String isbn;
    private final String title;
}
```

**触发时机**：`BookManagementService.createBook()` 成功保存新书后发布。

**携带信息**：新书的 ID、ISBN 和标题——这是其他上下文（如库存上下文需要为新书创建副本）最需要的信息。

---

#### 4.2.2 `BookUpdatedEvent.java`

```java
public class BookUpdatedEvent extends DomainEvent {
    private final String bookId;
    private final String title;
}
```

**触发时机**：图书基本信息更新后发布。

---

#### 4.2.3 `BookPublishedEvent.java`

```java
public class BookPublishedEvent extends DomainEvent {
    private final String bookId;
    private final String isbn;
    private final String title;
}
```

**触发时机**：图书从草稿/未发布状态变为已发布状态时发布。

**携带 ISBN**：因为发布后外部系统（如搜索引擎、推荐系统）可能需要用 ISBN 来关联。

---

#### 4.2.4 `BookDeletedEvent.java`

```java
public class BookDeletedEvent extends DomainEvent {
    private final String bookId;
    private final String title;
}
```

**触发时机**：图书被软删除后发布。

**注意**：不携带 ISBN，因为删除后的业务场景通常只需要知道"哪本书被删了"。

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
- `errorCode`：每个子类定义自己的错误码字符串（如 `"BOOK_NOT_FOUND"`）
- 继承 `RuntimeException`：是非受检异常，不强制调用方处理

---

#### 4.3.2 具体异常类

| 异常类 | 错误码 | 触发场景 | HTTP 映射 |
|--------|--------|----------|-----------|
| `BookNotFoundException` | `BOOK_NOT_FOUND` | 通过 ID 查找图书不存在 | 404 |
| `AuthorNotFoundException` | `AUTHOR_NOT_FOUND` | 通过 ID 查找作者不存在 | 404 |
| `CategoryNotFoundException` | `CATEGORY_NOT_FOUND` | 通过 ID 查找分类不存在 | 404 |
| `PublisherNotFoundException` | `PUBLISHER_NOT_FOUND` | 通过 ID 查找出版社不存在 | 404 |
| `DuplicateISBNException` | `DUPLICATE_ISBN` | 创建图书时 ISBN 已存在 | 409 Conflict |
| `DuplicateAuthorException` | `DUPLICATE_AUTHOR` | 重复添加同一个作者到图书 | 400 |
| `InvalidISBNException` | `INVALID_ISBN` | ISBN 格式或校验位不正确 | 400 |
| `InvalidOperationException` | `INVALID_OPERATION` | 状态不允许的操作（如删除已发布图书） | 400 |

每个异常类的实现都非常简洁，例如：

```java
public class BookNotFoundException extends DomainException {
    public BookNotFoundException(String message) {
        super("BOOK_NOT_FOUND", message);
    }
}
```

**错误码到 HTTP 状态的映射**：在 `GlobalExceptionHandler` 中，根据 errorCode 是否包含 `"NOT_FOUND"` 或 `"DUPLICATE"` 来映射到 404 或 409。

---

### 4.4 仓储接口（domain/repository）

> 仓储（Repository）是 DDD 中领域模型与数据存储之间的桥梁。**领域层只定义接口**，具体实现在基础设施层。

#### 4.4.1 `BookRepository.java`

```java
public interface BookRepository extends JpaRepository<Book, BookId>, CustomBookRepository {
    Optional<Book> findByIsbn(ISBN isbn);
    boolean existsByIsbn(ISBN isbn);
    List<Book> findByStatus(BookStatus status);
    Page<Book> findByStatus(BookStatus status, Pageable pageable);
    Page<Book> findByTitleContaining(String title, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors ba WHERE ba.authorName LIKE %:name%")
    Page<Book> findByAuthorName(@Param("name") String authorName, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.status = :status AND b.title LIKE %:title%")
    Page<Book> findByStatusAndTitle(@Param("status") BookStatus status, @Param("title") String title, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.publisherId = :publisherId")
    Page<Book> findByPublisherId(@Param("publisherId") String publisherId, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.categoryIds c WHERE c = :categoryId")
    Page<Book> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);
}
```

**DDD 定位**：继承 `JpaRepository` 获得 CRUD 能力，同时继承 `CustomBookRepository` 获得复杂查询能力。

**方法类型分析：**

| 方法类型 | 示例 | 说明 |
|----------|------|------|
| Spring Data 派生查询 | `findByIsbn()`, `existsByIsbn()` | 由 Spring Data 自动实现，根据方法名生成 SQL |
| JPQL 自定义查询 | `findByAuthorName()` | 使用 `@Query` + JPQL，处理关联查询 |
| 复杂动态查询 | `search()`（来自 CustomBookRepository） | 使用 Criteria API，由基础设施层的 `BookRepositoryImpl` 实现 |

---

#### 4.4.2 `CustomBookRepository.java`

```java
public interface CustomBookRepository {
    Page<Book> search(BookSearchCriteria criteria, Pageable pageable);
}
```

**DDD 定位**：自定义仓储接口，定义了需要 Criteria API 实现的动态组合查询。

**为什么需要分离？** Spring Data JPA 的 `@Query` 无法优雅处理"多个可选条件动态组合"的场景。自定义接口 + 基础设施层实现，是 DDD 推荐的做法——领域层定义"我需要什么"，基础设施层决定"怎么实现"。

---

#### 4.4.3 `AuthorRepository.java`

```java
public interface AuthorRepository extends JpaRepository<Author, AuthorId> {
    Page<Author> findByNameContaining(String name, Pageable pageable);
}
```

简洁的 JPA 仓储，提供基本的 CRUD + 按名称模糊搜索。

---

#### 4.4.4 `CategoryRepository.java`

```java
public interface CategoryRepository extends JpaRepository<Category, CategoryId> {
    List<Category> findByParentIsNull();
}
```

额外提供了查询所有根分类的方法。

---

#### 4.4.5 `PublisherRepository.java`

```java
public interface PublisherRepository extends JpaRepository<Publisher, PublisherId> {
    Page<Publisher> findByNameContaining(String name, Pageable pageable);
}
```

与 AuthorRepository 结构类似，提供基本 CRUD + 名称搜索。

---

### 4.5 领域服务（domain/service）

> 领域服务用于承载**不适合放在单个实体或值对象中的业务逻辑**，例如跨聚合的协调、外部仓储的调用等。

#### 4.5.1 `BookManagementService.java`

**DDD 角色**：**领域服务**—— 图书聚合的核心业务编排

```java
@Service
@Transactional(readOnly = true)
public class BookManagementService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CatalogDomainEventPublisher eventPublisher;
}
```

**职责：**

| 方法 | 业务逻辑 |
|------|----------|
| `createBook()` | 检查 ISBN 重复 → 创建 Book → 保存 → 发布 BookCreatedEvent |
| `addAuthorToBook()` | 查找 Book → 查找 Author（验证存在）→ 调用 Book.addAuthor() → 保存 |
| `removeAuthorFromBook()` | 查找 Book → 调用 Book.removeAuthor() → 保存 |
| `setPublisher()` | 查找 Book → 调用 Book.setPublisher() → 保存 |
| `addCategory()` / `removeCategory()` | 查找 Book → 调用 Book 的方法 → 保存 |
| `publishBook()` | 查找 Book → 调用 Book.publish()（内含业务规则校验）→ 保存 → 发布 BookPublishedEvent |
| `updateBook()` | 查找 Book → 调用 Book.updateBasicInfo() → 保存 → 发布 BookUpdatedEvent |
| `deleteBook()` | 查找 Book → 调用 Book.delete()（内含业务规则校验）→ 保存 → 发布 BookDeletedEvent |

**DDD 设计要点：**

1. **事务管理**：类级别 `@Transactional(readOnly = true)`，写操作方法覆盖为 `@Transactional`
2. **事件发布**：状态变更操作完成后发布领域事件，实现与其他上下文的解耦
3. **跨聚合协调**：`addAuthorToBook()` 需要同时查找 Book 和 Author，这是典型的"需要领域服务"的场景——Book 聚合不能直接访问 Author 仓储
4. **保护聚合不变量**：业务规则（如"不能删除已发布的书"）编码在 Book 实体中，而非服务中

---

#### 4.5.2 `AuthorManagementService.java`

**DDD 角色**：**领域服务**—— 作者聚合的 CRUD 编排

职责单一：创建、更新、查询、删除作者。不需要发布领域事件（因为作者信息变更不影响其他上下文的核心业务流程）。

**方法模式**：`findXxxOrThrow()` 私有辅助方法，找到则返回，未找到则抛出 `AuthorNotFoundException`。

---

#### 4.5.3 `CategoryManagementService.java`

**DDD 角色**：**领域服务**—— 分类树的构建与维护

**特殊设计**：

```java
public Category addChildCategory(CategoryId parentId, String name, String description) {
    Category parent = findCategoryOrThrow(parentId);
    Category child = parent.addChild(name, description);  // 调用聚合根的工厂方法
    categoryRepository.save(parent);                      // 保存父节点（级联保存子节点）
    return child;
}
```

注意这里保存的是 **parent** 而非 child，因为 `CascadeType.ALL` 会自动级联保存新创建的子分类。

---

#### 4.5.4 `PublisherManagementService.java`

**DDD 角色**：**领域服务**—— 出版社聚合的 CRUD 编排

结构与其他管理服务类似，额外提供 `searchPublishers()` 分页搜索功能。

---

#### 4.5.5 `ISBNValidationService.java`

**DDD 角色**：**领域服务**—— ISBN 验证与转换的专项服务

```java
@Service
public class ISBNValidationService {
    public boolean isValidISBN(String isbnValue) { ... }       // 验证 ISBN
    public ISBN convertToISBN13(ISBN isbn10) { ... }           // ISBN-10 → ISBN-13 转换
    public boolean lookupExternalRegistry(ISBN isbn) { ... }   // 外部注册表查询（桩实现）
}
```

**为什么是领域服务？** ISBN 的验证和转换逻辑不属于 Book 实体本身（Book 只关心"我有一个 ISBN"），也不属于 ISBN 值对象（ISBN 值对象只负责自身格式的合法性）。跨 ISBN 实例的操作（如版本转换）和外部系统集成适合放在领域服务中。

---

## 5. 应用层（Application Layer）

> 应用层是 DDD 中的**"编排层"**，它不包含业务逻辑，而是协调领域对象完成用例。它的职责是：接收外部请求 → 转换参数 → 调用领域服务 → 转换输出 → 返回结果。

### 5.1 应用服务（application/service）

#### `BookApplicationService.java`

**DDD 角色**：**应用服务（Application Service）**

```java
@Service
@Transactional(readOnly = true)
public class BookApplicationService {
    private final BookManagementService bookManagementService;  // 领域服务
    private final BookRepository bookRepository;                 // 仓储（直接用于查询）
}
```

**BookManagementService vs BookApplicationService 的区别：**

| 维度 | BookManagementService（领域服务） | BookApplicationService（应用服务） |
|------|------|------|
| 输入 | 领域对象（ISBN, BookId, LocalDate） | 外部格式（String, Command 对象） |
| 输出 | 领域对象（Book） | DTO（BookDTO） |
| 职责 | 业务规则校验和领域事件发布 | 参数转换和结果转换 |
| 调用者 | 应用服务 | 控制器 |

**典型方法流程**（以 `createBook` 为例）：

```
Controller 调用 → BookApplicationService.createBook(CreateBookCommand)
  1. 将 String isbn 转换为 ISBN 值对象
  2. 将 String publicationDate 转换为 LocalDate
  3. 调用 bookManagementService.createBook(...)
  4. 将返回的 Book 实体转换为 BookDTO
  5. 返回 BookDTO
```

这就是应用服务的核心价值——**在领域对象和外部表示之间做转换**，让领域层保持纯粹。

---

### 5.2 命令对象（application/command）

> 命令（Command）是 CQRS 模式中"写操作"的输入对象，封装了执行某个操作所需的全部参数。

#### `CreateBookCommand.java`

```java
public record CreateBookCommand(
    @NotBlank @Size(max = 20) String isbn,
    @NotBlank @Size(max = 500) String title,
    @Size(max = 2000) String description,
    String publicationDate,
    Integer pageCount,
    String language
) {}
```

**设计要点：**

- 使用 Java 16+ 的 `record` 类型——天然不可变，符合值对象理念
- `@NotBlank`、`@Size` 是 Bean Validation 注解，在 Controller 层由框架自动验证
- `publicationDate` 是 String 类型而非 LocalDate——因为外部 API 通常传字符串，转换由应用服务负责

---

#### `UpdateBookCommand.java`

```java
public record UpdateBookCommand(
    @Size(max = 500) String title,
    @Size(max = 2000) String description,
    String publicationDate,
    Integer pageCount,
    String language
) {}
```

与 CreateBookCommand 的区别：**所有字段都是可选的**（没有 `@NotBlank`），因为更新操作支持部分更新。

---

### 5.3 查询对象（application/query）

#### `BookSearchCriteria.java`

```java
public record BookSearchCriteria(
    String title,
    String authorName,
    BookStatus status,
    String publisherId,
    String categoryId,
    String language
) {
    public boolean hasAnyFilter() { ... }
}
```

**DDD 定位**：查询条件对象，与命令对象对应——命令是"写"，查询是"读"。这是 CQRS 的简化实现。

**`hasAnyFilter()`**：判断是否有任何过滤条件，避免无条件查询时的不必要性能开销。

---

### 5.4 数据传输对象（application/dto）

#### `ApiResponse.java` —— 统一响应信封

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    String errorCode
) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> error(String errorCode, String error) { ... }
}
```

**设计要点：**

- 泛型 `<T>` 支持任意数据类型
- 成功时：`success=true, data=实际数据, error=null`
- 失败时：`success=false, data=null, error=错误信息, errorCode=错误码`
- 静态工厂方法 `ok()` / `error()` 提供便捷构造

---

#### `BookDTO.java`

```java
public record BookDTO(
    String id, String isbn, String title, String description,
    LocalDate publicationDate, Integer pageCount, String language,
    String status, String publisherId,
    List<AuthorDTO> authors, List<String> categoryIds,
    Long version, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static BookDTO from(Book book) { ... }  // 领域对象 → DTO
}
```

**关键设计**：`from(Book)` 是一个**组装器方法（Assembler）**，将 Book 聚合根（可能包含关联的 BookAuthor 列表）展平为一个扁平的 DTO。注意 `status` 从枚举变成了 String，`id` 从 BookId 变成了 String——DTO 总是使用基本类型，方便 JSON 序列化。

---

#### `AuthorDTO.java`

```java
public record AuthorDTO(
    String id, String name, String biography,
    LocalDate birthDate, LocalDate deathDate, String nationality,
    String role, Long version, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static AuthorDTO from(Author author) { ... }
    public static AuthorDTO from(BookAuthor bookAuthor) { ... }  // 两个 from 重载
}
```

**双重 `from()` 方法**：这是 DTO 层的一个亮点设计——

- `from(Author)`：从独立的作者聚合根转换（role 为 null）
- `from(BookAuthor)`：从图书内的关联实体转换（role 有值，但 biographical 信息为 null）

---

#### `CategoryDTO.java`

```java
public record CategoryDTO(
    String id, String name, String description, int level,
    String parentId, List<CategoryDTO> children,  // 递归结构
    Long version, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static CategoryDTO from(Category category) { ... }
}
```

**递归结构**：`children` 字段是 `List<CategoryDTO>`，天然支持树形 JSON 输出。

---

#### `PublisherDTO.java`

```java
public record PublisherDTO(
    String id, String name, String description,
    String address, String phone, String email, String website,
    Long version, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static PublisherDTO from(Publisher publisher) { ... }
}
```

简洁的字段映射，没有特殊逻辑。

---

## 6. 基础设施层（Infrastructure Layer）

> 基础设施层为领域层提供技术实现。它实现领域层定义的接口（如仓储），处理数据库、消息队列等技术细节。

### 6.1 `BookRepositoryImpl.java` —— 复杂查询实现

```java
@Repository
public class BookRepositoryImpl implements CustomBookRepository {
    private final EntityManager entityManager;
}
```

**DDD 定位**：实现领域层 `CustomBookRepository` 接口的**基础设施组件**。

**为什么使用 Criteria API？**

`search()` 方法需要根据 `BookSearchCriteria` 动态组合查询条件——用户可能只传 title，可能同时传 title + status，也可能什么都不传。Criteria API 支持在运行时动态构建查询：

```java
private List<Predicate> buildPredicates(BookSearchCriteria criteria, CriteriaBuilder cb, Root<Book> root) {
    List<Predicate> predicates = new ArrayList<>();
    if (criteria.title() != null) predicates.add(cb.like(...));
    if (criteria.status() != null) predicates.add(cb.equal(...));
    if (criteria.authorName() != null) predicates.add(cb.like(root.join("authors")...));
    if (criteria.categoryId() != null) predicates.add(cb.isMember(...));
    return predicates;  // 动态组合的 AND 条件
}
```

**Spring Data JPA 的命名约定**：类名必须是 `BookRepositoryImpl`（= 接口名 `BookRepository` + `Impl` 后缀），Spring Data 会自动发现并将其与 `BookRepository` 接口合并。

---

### 6.2 `CatalogDomainEventPublisher.java` —— 事件发布器

```java
@Component("catalogDomainEventPublisher")
public class CatalogDomainEventPublisher {
    private final DomainEventPublisher localPublisher;            // Spring 本地事件
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate; // Kafka 远程事件（可选）

    public CatalogDomainEventPublisher(
            DomainEventPublisher localPublisher,
            ObjectProvider<KafkaTemplate<String, DomainEvent>> kafkaTemplateProvider,  // 可选注入
            Environment environment) {
        this.localPublisher = localPublisher;
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();  // 可能是 null
    }
}
```

**DDD 定位**：**领域事件发布服务**，双通道发布策略。

**双通道设计：**

| 通道 | 机制 | 用途 | 范围 |
|------|------|------|------|
| 本地 | `DomainEventPublisher`（Spring ApplicationEventPublisher） | 同 JVM 内的事件处理 | 编目上下文内部 |
| 远程 | `KafkaTemplate` → Kafka Topic | 跨服务的事件传播 | 其他微服务上下文 |

**可选 Kafka 依赖**：使用 `ObjectProvider<KafkaTemplate>` 使 Kafka 变为可选——生产环境有 Kafka 则远程发布，测试环境无 Kafka 则跳过。这确保了领域事件发布的**优雅降级**。

---

## 7. 接口层（Interfaces Layer）

> 接口层是系统与外部世界的边界。它接收 HTTP 请求，调用应用服务，返回 HTTP 响应。**不包含任何业务逻辑。**

### 7.1 `BookController.java`

```java
@RestController
@RequestMapping("/api/catalog/books")
@Tag(name = "Books", description = "Book management API")
public class BookController {
    private final BookApplicationService bookApplicationService;
}
```

**API 端点一览：**

| HTTP 方法 | 路径 | 操作 | DDD 用例 |
|-----------|------|------|----------|
| `POST` | `/api/catalog/books` | 创建图书 | 命令 |
| `GET` | `/api/catalog/books/{id}` | 获取单本图书 | 查询 |
| `GET` | `/api/catalog/books` | 获取所有图书 | 查询 |
| `PUT` | `/api/catalog/books/{id}` | 更新图书信息 | 命令 |
| `POST` | `/api/catalog/books/{id}/publish` | 发布图书 | 命令（状态变更） |
| `POST` | `/api/catalog/books/{id}/unpublish` | 取消发布 | 命令（状态变更） |
| `DELETE` | `/api/catalog/books/{id}` | 删除图书 | 命令 |
| `GET` | `/api/catalog/books/search` | 搜索图书（分页） | 查询 |
| `POST` | `/api/catalog/books/{id}/authors` | 添加作者 | 命令（聚合间关联） |
| `DELETE` | `/api/catalog/books/{id}/authors/{authorId}` | 移除作者 | 命令 |
| `PUT` | `/api/catalog/books/{id}/publisher` | 设置出版社 | 命令 |
| `POST` | `/api/catalog/books/{id}/categories` | 添加分类 | 命令 |
| `DELETE` | `/api/catalog/books/{id}/categories/{categoryId}` | 移除分类 | 命令 |

**设计要点：**

- **CQRS 体现**：POST/PUT/DELETE 是命令（写），GET 是查询（读）；状态变更操作（publish/unpublish）使用 POST 而非 PUT
- **统一响应**：所有端点返回 `ApiResponse<T>` 信封
- **Swagger 注解**：`@Operation(summary = ...)` 生成 API 文档
- **参数验证**：`@Valid @RequestBody CreateBookCommand` 触发 Bean Validation

---

### 7.2 `AuthorController.java`

```java
@RestController
@RequestMapping("/api/catalog/authors")
```

| HTTP 方法 | 路径 | 操作 |
|-----------|------|------|
| `POST` | `/api/catalog/authors` | 创建作者 |
| `GET` | `/api/catalog/authors/{id}` | 获取作者 |
| `GET` | `/api/catalog/authors` | 获取所有作者 |
| `PUT` | `/api/catalog/authors/{id}` | 更新作者 |
| `DELETE` | `/api/catalog/authors/{id}` | 删除作者 |

**特点**：请求体使用 Controller 内部定义的 `record`（`CreateAuthorRequest`、`UpdateAuthorRequest`），因为这些类型只在 Controller 层使用，不需要像 Book 那样独立的 Command 类。

---

### 7.3 `CategoryController.java`

```java
@RestController
@RequestMapping("/api/catalog/categories")
```

| HTTP 方法 | 路径 | 操作 |
|-----------|------|------|
| `POST` | `/api/catalog/categories` | 创建根分类 |
| `POST` | `/api/catalog/categories/{parentId}/children` | 添加子分类 |
| `PUT` | `/api/catalog/categories/{id}` | 更新分类 |
| `GET` | `/api/catalog/categories/{id}` | 获取分类 |
| `GET` | `/api/catalog/categories` | 获取所有分类 |
| `GET` | `/api/catalog/categories/roots` | 获取根分类列表 |
| `DELETE` | `/api/catalog/categories/{id}` | 删除分类 |

---

### 7.4 `PublisherController.java`

```java
@RestController
@RequestMapping("/api/catalog/publishers")
```

| HTTP 方法 | 路径 | 操作 |
|-----------|------|------|
| `POST` | `/api/catalog/publishers` | 创建出版社 |
| `PUT` | `/api/catalog/publishers/{id}` | 更新出版社 |
| `GET` | `/api/catalog/publishers/{id}` | 获取出版社 |
| `GET` | `/api/catalog/publishers` | 获取所有出版社 |
| `GET` | `/api/catalog/publishers/search` | 按名称搜索出版社（分页） |
| `DELETE` | `/api/catalog/publishers/{id}` | 删除出版社 |

---

### 7.5 `GlobalExceptionHandler.java` —— 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)       → 根据错误码映射 HTTP 状态
    @ExceptionHandler(MethodArgumentNotValidException.class) → 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)         → 400 Bad Request
}
```

**DDD 定位**：**跨切面（Cross-Cutting Concern）**，在接口层统一处理所有异常。

**错误码 → HTTP 状态映射逻辑：**

```java
private HttpStatus mapToHttpStatus(DomainException ex) {
    if (code.contains("NOT_FOUND")) return HttpStatus.NOT_FOUND;     // 404
    if (code.contains("DUPLICATE")) return HttpStatus.CONFLICT;      // 409
    return HttpStatus.BAD_REQUEST;                                    // 400
}
```

**返回格式统一**：所有异常都包装成 `ApiResponse.error(errorCode, message)`，前端只需处理一种响应格式。

---

## 8. 层间调用流程

以 **"创建图书"** 为例，展示一次完整请求的层间调用链：

```
HTTP POST /api/catalog/books
  │
  ▼
┌─────────────────────────────────────────────────────────┐
│ BookController.createBook(CreateBookCommand)             │  ← interfaces 层
│   @Valid 触发 Bean Validation                             │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ BookApplicationService.createBook(CreateBookCommand)     │  ← application 层
│   1. new ISBN(command.isbn())    // String → 值对象       │
│   2. LocalDate.parse(...)        // String → LocalDate    │
│   3. 调用 bookManagementService                          │
│   4. BookDTO.from(book)          // Book → DTO            │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ BookManagementService.createBook(isbn, title, ...)       │  ← domain 层（服务）
│   1. bookRepository.existsByIsbn(isbn) → 检查重复        │
│   2. Book.create(...)            // 调用聚合根工厂方法     │
│   3. bookRepository.save(book)   // 持久化                │
│   4. eventPublisher.publish(BookCreatedEvent)             │
└───────┬───────────────────────────────┬─────────────────┘
        │                               │
        ▼                               ▼
┌──────────────────┐  ┌────────────────────────────────────┐
│ BookRepository    │  │ CatalogDomainEventPublisher         │  ← infrastructure 层
│ (Spring Data JPA  │  │   → localPublisher.publish(event)  │    Spring 本地事件
│  自动实现)        │  │   → kafkaTemplate.send(event)       │    Kafka 远程事件
└──────────────────┘  └────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│ PostgreSQL (生产) / H2 (测试)                             │  ← 数据库
│ INSERT INTO books (id, isbn, title, status, ...)          │
└──────────────────────────────────────────────────────────┘
```

---

## 9. 文件清单速查表

| # | 文件路径 | DDD 层 | DDD 角色 | 核心职责 |
|---|---------|--------|----------|----------|
| 1 | `CatalogApplication.java` | Bootstrap | 启动类 | Spring Boot 入口 |
| 2 | `config/JpaConfig.java` | Config | 配置 | 启用 JPA 审计 |
| 3 | `domain/model/Book.java` | Domain | **聚合根** | 图书生命周期管理 |
| 4 | `domain/model/BookAuthor.java` | Domain | 聚合内实体 | 图书-作者关联 |
| 5 | `domain/model/Author.java` | Domain | **聚合根** | 作者信息管理 |
| 6 | `domain/model/Category.java` | Domain | **聚合根** | 树形分类管理 |
| 7 | `domain/model/Publisher.java` | Domain | **聚合根** | 出版社信息管理 |
| 8 | `domain/model/ISBN.java` | Domain | **值对象** | ISBN 验证与格式化 |
| 9 | `domain/model/enums/BookStatus.java` | Domain | 枚举 | 图书状态枚举 |
| 10 | `domain/model/enums/AuthorRole.java` | Domain | 枚举 | 作者角色枚举 |
| 11 | `domain/event/BookCreatedEvent.java` | Domain | 领域事件 | 图书已创建 |
| 12 | `domain/event/BookUpdatedEvent.java` | Domain | 领域事件 | 图书已更新 |
| 13 | `domain/event/BookPublishedEvent.java` | Domain | 领域事件 | 图书已发布 |
| 14 | `domain/event/BookDeletedEvent.java` | Domain | 领域事件 | 图书已删除 |
| 15 | `domain/exception/DomainException.java` | Domain | 异常基类 | 错误码 + 消息 |
| 16 | `domain/exception/BookNotFoundException.java` | Domain | 异常 | 图书不存在 |
| 17 | `domain/exception/AuthorNotFoundException.java` | Domain | 异常 | 作者不存在 |
| 18 | `domain/exception/CategoryNotFoundException.java` | Domain | 异常 | 分类不存在 |
| 19 | `domain/exception/PublisherNotFoundException.java` | Domain | 异常 | 出版社不存在 |
| 20 | `domain/exception/DuplicateISBNException.java` | Domain | 异常 | ISBN 重复 |
| 21 | `domain/exception/DuplicateAuthorException.java` | Domain | 异常 | 重复添加作者 |
| 22 | `domain/exception/InvalidISBNException.java` | Domain | 异常 | ISBN 无效 |
| 23 | `domain/exception/InvalidOperationException.java` | Domain | 异常 | 非法状态操作 |
| 24 | `domain/repository/BookRepository.java` | Domain | 仓储接口 | 图书数据访问 |
| 25 | `domain/repository/CustomBookRepository.java` | Domain | 仓储接口 | 自定义查询接口 |
| 26 | `domain/repository/AuthorRepository.java` | Domain | 仓储接口 | 作者数据访问 |
| 27 | `domain/repository/CategoryRepository.java` | Domain | 仓储接口 | 分类数据访问 |
| 28 | `domain/repository/PublisherRepository.java` | Domain | 仓储接口 | 出版社数据访问 |
| 29 | `domain/service/BookManagementService.java` | Domain | **领域服务** | 图书业务编排 |
| 30 | `domain/service/AuthorManagementService.java` | Domain | 领域服务 | 作者业务编排 |
| 31 | `domain/service/CategoryManagementService.java` | Domain | 领域服务 | 分类业务编排 |
| 32 | `domain/service/PublisherManagementService.java` | Domain | 领域服务 | 出版社业务编排 |
| 33 | `domain/service/ISBNValidationService.java` | Domain | 领域服务 | ISBN 验证/转换 |
| 34 | `application/service/BookApplicationService.java` | Application | **应用服务** | 图书用例编排 |
| 35 | `application/command/CreateBookCommand.java` | Application | 命令 | 创建图书入参 |
| 36 | `application/command/UpdateBookCommand.java` | Application | 命令 | 更新图书入参 |
| 37 | `application/query/BookSearchCriteria.java` | Application | 查询 | 搜索条件 |
| 38 | `application/dto/ApiResponse.java` | Application | DTO | 统一响应信封 |
| 39 | `application/dto/BookDTO.java` | Application | DTO | 图书数据传输 |
| 40 | `application/dto/AuthorDTO.java` | Application | DTO | 作者数据传输 |
| 41 | `application/dto/CategoryDTO.java` | Application | DTO | 分类数据传输 |
| 42 | `application/dto/PublisherDTO.java` | Application | DTO | 出版社数据传输 |
| 43 | `infrastructure/persistence/jpa/BookRepositoryImpl.java` | Infrastructure | 仓储实现 | Criteria API 动态查询 |
| 44 | `infrastructure/messaging/CatalogDomainEventPublisher.java` | Infrastructure | 事件发布 | Spring + Kafka 双通道发布 |
| 45 | `interfaces/rest/BookController.java` | Interfaces | REST 控制器 | 图书 API |
| 46 | `interfaces/rest/AuthorController.java` | Interfaces | REST 控制器 | 作者 API |
| 47 | `interfaces/rest/CategoryController.java` | Interfaces | REST 控制器 | 分类 API |
| 48 | `interfaces/rest/PublisherController.java` | Interfaces | REST 控制器 | 出版社 API |
| 49 | `interfaces/rest/GlobalExceptionHandler.java` | Interfaces | 全局异常处理 | 统一错误响应 |

**总计：49 个源文件**
