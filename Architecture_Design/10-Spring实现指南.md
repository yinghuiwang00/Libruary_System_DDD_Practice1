# Spring Boot 实现指南

## 1. 项目结构设计

### 1.1 模块化项目结构

```
library-system/
├── library-catalog/              # 编目上下文模块
│   ├── src/main/java/
│   │   └── com/library/catalog/
│   │       ├── CatalogApplication.java
│   │       ├── domain/              # 领域层
│   │       ├── application/         # 应用层
│   │       ├── infrastructure/      # 基础设施层
│   │       └── interfaces/         # 接口层
│   └── pom.xml
├── library-inventory/            # 馆藏上下文模块
├── library-circulation/          # 借阅上下文模块
├── library-patron/               # 会员上下文模块
├── library-payment/              # 支付上下文模块
├── library-analytics/             # 分析上下文模块
├── library-notification/          # 通知上下文模块
├── library-shared/                # 共享模块
│   └── src/main/java/
│       └── com/library/shared/
│           ├── domain/             # 共享领域概念
│           ├── infrastructure/      # 共享基础设施
│           └── common/             # 通用工具
└── pom.xml                      # 父POM
```

### 1.2 包结构最佳实践

```
domain/
├── model/                   # 领域模型
│   ├── aggregate/          # 聚合根
│   ├── entity/             # 实体
│   ├── valueobject/        # 值对象
│   └── enum/               # 枚举
├── service/                # 领域服务
├── repository/             # 仓储接口
└── event/                  # 领域事件

application/
├── service/                # 应用服务
├── command/                # 命令对象
├── query/                  # 查询对象
└── dto/                    # 数据传输对象

infrastructure/
├── persistence/            # 持久化实现
│   ├── jpa/               # JPA实现
│   └── repository/        # 仓储实现
├── external/               # 外部服务集成
└── messaging/              # 消息传递

interfaces/
├── rest/                   # REST控制器
├── graphql/                # GraphQL解析器
└── dto/                    # 接口DTO
```

## 2. 核心注解使用

### 2.1 Spring Boot 核心注解

```java
@SpringBootApplication
@EnableJpaRepositories
@EnableKafka
@EnableCaching
@EnableScheduling
public class LibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
```

### 2.2 DDD相关注解

```java
// 聚合根标识
@Embeddable
public class BookId {
    @Column(name = "id")
    private String value;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public static BookId generate() {
        return new BookId(UUID.randomUUID().toString(), LocalDateTime.now());
    }
}

// 聚合根
@Entity
@Table(name = "books")
public class Book {
    
    @EmbeddedId
    private BookId id;
    
    @Version
    private Long version; // 乐观锁
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
```

### 2.3 事务管理注解

```java
@Service
@Transactional(readOnly = true)
public class BookManagementService {
    
    @Transactional
    public Book createBook(CreateBookCommand command) {
        // 写操作使用事务
        Book book = new Book(...);
        return bookRepository.save(book);
    }
    
    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void updateBook(UpdateBookCommand command) {
        // 重试机制
        Book book = bookRepository.findById(command.getBookId())
            .orElseThrow(() -> new BookNotFoundException(command.getBookId()));
        
        book.updateBasicInfo(command.getTitle(), command.getDescription());
        bookRepository.save(book);
    }
    
    // 只读方法不需要@Transactional注解，类级别已设置readOnly=true
    public Book getBook(BookId bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
```

## 3. 仓储模式实现

### 3.1 仓储接口定义

```java
public interface BookRepository extends JpaRepository<Book, BookId>, CustomBookRepository {
    
    @Query("SELECT b FROM Book b WHERE b.status = :status")
    List<Book> findByStatus(@Param("status") BookStatus status);
    
    @Modifying
    @Query("UPDATE Book b SET b.status = :status WHERE b.id = :id")
    int updateStatus(@Param("id") BookId id, @Param("status") BookStatus status);
}
```

### 3.2 自定义仓储实现

```java
public interface CustomBookRepository {
    List<Book> findByAuthorName(String authorName);
    List<Book> searchBooks(BookSearchQuery query);
}

@Repository
public class BookRepositoryImpl implements CustomBookRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Book> findByAuthorName(String authorName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> root = query.from(Book.class);
        
        Join<Book, BookAuthor> authorsJoin = root.join("authors", JoinType.INNER);
        Join<BookAuthor, Author> authorJoin = authorsJoin.join("author");
        
        query.where(cb.equal(authorJoin.get("name"), authorName));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public List<Book> searchBooks(BookSearchQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (query.getTitle() != null) {
            predicates.add(cb.like(root.get("title"), "%" + query.getTitle() + "%"));
        }
        
        if (query.getAuthor() != null) {
            predicates.add(cb.equal(root.get("mainAuthor"), query.getAuthor()));
        }
        
        if (query.getIsbn() != null) {
            predicates.add(cb.equal(root.get("isbn").get("value"), query.getIsbn()));
        }
        
        cq.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(cq).getResultList();
    }
}
```

## 4. 事件驱动架构实现

### 4.1 领域事件发布

```java
@Component
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public DomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public void publish(DomainEvent event) {
        // 同步发布（用于同一JVM内的事件处理）
        eventPublisher.publishEvent(event);
        
        // 异步发布到消息队列（用于跨服务通信）
        publishToMessageQueue(event);
    }
    
    private void publishToMessageQueue(DomainEvent event) {
        // 使用Spring Kafka或RabbitMQ模板发送事件
        // 实现略...
    }
}
```

### 4.2 事件订阅和处理

```java
@Component
public class BookEventHandlers {
    
    @EventListener
    @Async
    public void handleBookCreatedEvent(BookCreatedEvent event) {
        // 处理图书创建事件
        log.info("Book created: {}", event.getBookId());
        
        // 更新搜索索引
        searchIndexService.indexBook(event.getBookId());
        
        // 通知相关系统
        notificationService.notifyBookAvailable(event.getBookId());
    }
    
    @EventListener
    @Async
    public void handleBookPublishedEvent(BookPublishedEvent event) {
        // 处理图书发布事件
        log.info("Book published: {}", event.getBookId());
        
        // 更新推荐系统
        recommendationService.updateBookAvailability(event.getBookId());
    }
}
```

## 5. 异常处理

### 5.1 自定义异常

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        ErrorResponse error = new ErrorResponse(
            "DOMAIN_ERROR",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### 5.2 异常类定义

```java
public class DomainException extends RuntimeException {
    private final String errorCode;
    
    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

public class BookNotFoundException extends DomainException {
    public BookNotFoundException(BookId bookId) {
        super("BOOK_NOT_FOUND", "Book not found: " + bookId);
    }
}

public class DuplicateISBNException extends DomainException {
    public DuplicateISBNException(String isbn) {
        super("DUPLICATE_ISBN", "Book with ISBN already exists: " + isbn);
    }
}
```

## 6. 配置管理

### 6.1 多环境配置

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/library_dev
    username: dev_user
    password: dev_pass
  
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    com.library: DEBUG

---
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    show-sql: false

logging:
  level:
    com.library: INFO
    org.hibernate.SQL: WARN
```

### 6.2 配置属性类

```java
@Configuration
@ConfigurationProperties(prefix = "library")
@Data
public class LibraryProperties {
    
    private int maxBooksPerPatron = 5;
    private int loanPeriodDays = 30;
    private int maxRenewalsAllowed = 2;
    private BigDecimal dailyFineRate = new BigDecimal("0.50");
    private BigDecimal maxFineAmount = new BigDecimal("50.00");
    
    private Notification notification = new Notification();
    private Payment payment = new Payment();
    
    @Data
    public static class Notification {
        private boolean emailEnabled = true;
        private boolean smsEnabled = false;
        private String emailFrom = "noreply@library.com";
    }
    
    @Data
    public static class Payment {
        private boolean enabled = true;
        private String alipayAppId;
        private String wechatAppId;
    }
}
```

## 7. API文档生成

### 7.1 Swagger配置

```java
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Library Management System API")
                .description("基于DDD的图书馆管理系统API文档")
                .version("v1.0")
                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
            .externalDocs(new ExternalDocumentation()
                .description("Spring Boot Library Documentation")
                .url("https://docs.spring.io/spring-boot/docs/current/reference/html/"));
    }
}
```

### 7.2 Controller文档注解

```java
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Management", description = "图书管理API")
@RequiredArgsConstructor
public class BookController {
    
    private final BookApplicationService bookApplicationService;
    
    @Operation(summary = "创建图书", description = "创建新的图书记录")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "图书创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "409", description = "ISBN已存在")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BookDTO>> createBook(
            @Valid @RequestBody CreateBookRequest request) {
        BookDTO book = bookApplicationService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(book));
    }
    
    @Operation(summary = "获取图书详情", description = "根据ID获取图书详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDTO>> getBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable String id) {
        BookDTO book = bookApplicationService.getBook(BookId.of(id));
        return ResponseEntity.ok(ApiResponse.success(book));
    }
}
```

## 8. 测试配置

### 8.1 测试配置类

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
public abstract class BaseControllerTest {
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected TestEntityManager entityManager;
    
    protected String asJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
    
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
```

### 8.2 集成测试示例

```java
@SpringBootTest
@AutoConfigureMockMvc
public class BookControllerIntegrationTest extends BaseControllerTest {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Test
    void shouldCreateBookSuccessfully() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Test Book");
        request.setDescription("A test book");
        request.setIsbn("978-3-16-148410-0");
        request.setLanguage("English");
        request.setPageCount(300);
        
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Book"));
    }
    
    @Test
    void shouldReturn404WhenBookNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

## 9. 监控和健康检查

### 9.1 自定义健康指标

```java
@Component
public class LibraryHealthIndicator implements HealthIndicator {
    
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    
    @Override
    public Health health() {
        try {
            long bookCount = bookRepository.count();
            long activeLoanCount = loanRepository.countActiveLoans();
            
            return Health.up()
                .withDetail("totalBooks", bookCount)
                .withDetail("activeLoans", activeLoanCount)
                .withDetail("systemStatus", "All systems operational")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 9.2 自定义指标

```java
@Component
public class LibraryMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter bookCreatedCounter;
    private final Counter bookBorrowedCounter;
    private final Gauge activeLoanGauge;
    
    public LibraryMetrics(MeterRegistry meterRegistry, LoanRepository loanRepository) {
        this.meterRegistry = meterRegistry;
        this.bookCreatedCounter = Counter.builder("book.created")
            .description("Total number of books created")
            .register(meterRegistry);
        
        this.bookBorrowedCounter = Counter.builder("book.borrowed")
            .description("Total number of books borrowed")
            .register(meterRegistry);
        
        this.activeLoanGauge = Gauge.builder("loan.active")
            .description("Current number of active loans")
            .register(meterRegistry, this, loanRepository);
    }
    
    public void incrementBookCreated() {
        bookCreatedCounter.increment();
    }
    
    public void incrementBookBorrowed() {
        bookBorrowedCounter.increment();
    }
    
    public double getActiveLoanCount() {
        // Gauge回调方法
        return 0.0; // 实际实现从repository获取
    }
}
```

## 10. 总结

Spring Boot实现指南提供了：

1. **项目结构**: 清晰的模块化项目组织
2. **注解使用**: Spring Boot和DDD相关注解的最佳实践
3. **仓储模式**: JPA仓储的自定义实现
4. **事件驱动**: 领域事件的发布和订阅
5. **异常处理**: 统一的异常处理机制
6. **配置管理**: 多环境配置和配置属性类
7. **API文档**: Swagger/OpenAPI文档生成
8. **测试策略**: 集成测试配置和示例
9. **监控健康**: 自定义健康检查和指标

该指南为团队提供了Spring Boot实现DDD系统的标准化方法和最佳实践。

---

**文档版本**: v1.0  
**创建日期**: 2026-05-03  
**最后更新**: 2026-05-03  
**状态**: 初稿完成
