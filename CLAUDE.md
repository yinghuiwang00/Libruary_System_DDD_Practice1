# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise Library Management System built with Domain-Driven Design (DDD) principles. Currently in planning/design phase with comprehensive architecture documentation but no implementation code yet.

**Status:** Early stage - Architecture design complete, implementation pending

## Architecture

### Bounded Contexts (Planned Module Structure)

The system is organized into 7 bounded contexts, each as a separate Spring Boot module:

- **library-catalog/** - Book information management, ISBN integration, metadata
- **library-inventory/** - Book copy management, multi-branch inventory sync
- **library-circulation/** - Borrow/return flows, reservations, fine calculation
- **library-patron/** - Membership, authentication, borrowing permissions
- **library-payment/** - Fine payments, third-party payment integration
- **library-analytics/** - Statistics, popular books, reporting
- **library-notification/** - Due reminders, reservation alerts, multi-channel notifications
- **library-shared/** - Shared domain concepts, common infrastructure

### DDD Layering (Per Module)

```
domain/          # Domain layer (pure business logic)
  ├── model/
  │   ├── aggregate/     # Aggregate roots
  │   ├── entity/        # Entities
  │   ├── valueobject/   # Value objects
  │   └── enum/          # Enums
  ├── service/           # Domain services
  ├── repository/        # Repository interfaces
  └── event/             # Domain events

application/      # Application layer (orchestration)
  ├── service/           # Application services
  ├── command/           # Command objects
  ├── query/             # Query objects
  └── dto/               # DTOs

infrastructure/  # Infrastructure layer (technical concerns)
  ├── persistence/        # JPA entities, repository implementations
  ├── external/           # External service integrations
  └── messaging/          # Message queue integration

interfaces/       # Interface layer (APIs)
  ├── rest/              # REST controllers
  ├── graphql/           # GraphQL resolvers
  └── dto/               # API DTOs
```

### Key Architectural Patterns

- **Event-Driven Architecture**: Domain events for cross-context communication
- **CQRS**: Separate read and write models for query optimization
- **Event Sourcing**: Store domain events for audit and state reconstruction
- **Saga Pattern**: Handle distributed transactions across services
- **Optimistic Locking**: `@Version` field on aggregates for concurrency control

## Technology Stack

- **Framework**: Spring Boot 3.2+
- **Database**: PostgreSQL with Spring Data JPA
- **Messaging**: Apache Kafka or RabbitMQ
- **Cache**: Redis for distributed caching
- **Search**: Elasticsearch
- **API**: REST and/or GraphQL
- **Security**: Spring Security + JWT + OAuth2
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Monitoring**: Prometheus + Grafana
- **Tracing**: OpenTelemetry + Jaeger
- **Containerization**: Docker + Kubernetes

## Build and Test Commands

*Note: These commands assume Maven multi-module project structure will be created*

```bash
# Build all modules
mvn clean install

# Build specific module
cd library-catalog && mvn clean install

# Run tests
mvn test

# Run tests for specific module
cd library-catalog && mvn test

# Run specific test class
mvn test -Dtest=BookServiceTest

# Run specific test method
mvn test -Dtest=BookServiceTest#testCreateBook

# Skip tests during build
mvn clean install -DskipTests

# Run Spring Boot application
cd library-catalog && mvn spring-boot:run

# Package for deployment
mvn clean package

# Generate API documentation (SpringDoc)
# Access at http://localhost:8080/swagger-ui.html when running
```

## Key Design Decisions (See Architecture_Design/11-架构决策记录ADR.md)

- **ADR-001**: Event Sourcing architecture chosen for audit trail and state reconstruction
- **ADR-002**: CQRS pattern for read/write separation and query optimization
- **ADR-003**: Saga pattern for distributed transaction management
- **ADR-004**: Kafka/RabbitMQ as message middleware for event-driven communication
- **ADR-005**: Multi-level caching strategy (local cache + Redis) for performance

## Important Code Patterns

### Aggregate Root Pattern

```java
@Entity
@Table(name = "books")
public class Book {

    @EmbeddedId
    private BookId id;

    @Version
    private Long version; // Optimistic locking

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Repository Pattern

```java
// Domain layer - interface
public interface BookRepository extends JpaRepository<Book, BookId>, CustomBookRepository {
    @Query("SELECT b FROM Book b WHERE b.status = :status")
    List<Book> findByStatus(@Param("status") BookStatus status);
}

// Infrastructure layer - implementation
@Repository
public class BookRepositoryImpl implements CustomBookRepository {
    @PersistenceContext
    private EntityManager entityManager;
}
```

### Domain Event Publishing

```java
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event); // Same JVM
        publishToMessageQueue(event);      // Cross-service
    }
}
```

### Transaction Management

```java
@Service
@Transactional(readOnly = true)
public class BookManagementService {

    @Transactional
    public Book createBook(CreateBookCommand command) {
        // Write operations require explicit @Transactional
    }

    // Read-only methods inherit class-level @Transactional(readOnly = true)
    public Book getBook(BookId bookId) { }
}
```

### Configuration Properties

```java
@Configuration
@ConfigurationProperties(prefix = "library")
@Data
public class LibraryProperties {
    private int maxBooksPerPatron = 5;
    private int loanPeriodDays = 30;
    private BigDecimal dailyFineRate = new BigDecimal("0.50");
}
```

## Testing Strategy

- **Unit Tests**: Individual domain logic, services
- **Integration Tests**: Repository operations, API endpoints
- **Functional Tests**: Cucumber BDD tests covering main positive (happy path) flows only
- **Test Plan**:  refer to Architecture_Design/15-TESTPLAN.md for the testplan of Functional Test and Integration Test
- **80%+ Coverage Required** (per user rules)
- **TDD Approach**: Write tests first, implement to pass tests (per `~/.claude/rules/common/testing.md`)

### Functional Testing with Cucumber

Use Cucumber for BDD-style functional tests. Scope to **main positive flows for each component only** — edge cases and error paths belong in unit tests.

```gherkin
# Example: Borrowing a book (happy path)
Feature: Book Borrowing
  Scenario: Patron successfully borrows an available book
    Given a patron with valid membership
    And a book copy is available
    When the patron borrows the book
    Then the book status changes to "borrowed"
    And the patron's borrowed count increases by 1
```
Dependencies: `io.cucumber:cucumber-java`, `io.cucumber:cucumber-spring`, `io.cucumber:cucumber-junit-platform-engine`

Test configuration:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
}
```

## Documentation References

All architecture and design documents are in `Architecture_Design/`:

- `企业级图书馆管理系统-DDD架构设计文档.md` - Overall architecture overview
- `01-详细设计计划.md` - Design task breakdown
- `02-08` - Individual bounded context designs
- `09-领域模型总体设计.md` - Overall domain model
- `10-Spring实现指南.md` - Spring Boot implementation guide with code examples
- `11-架构决策记录ADR.md` - Architecture decision records
- `12-业务流程可视化.md` - Business flow diagrams
- `13-部署架构和扩展设计.md` - Deployment architecture
- `14-实现最佳实践指南.md` - Implementation best practices

**When implementing features**, always reference the corresponding context design document and the Spring implementation guide.

## Development Progress

**Detailed Development Plan**: See `DEVELOPMENT_PLAN.md` for complete step-by-step development roadmap.


### How to Continue Development

When starting or resuming development:

1. **Open** `DEVELOPMENT_PLAN.md`
2. **Find** last completed task (look for checked `[ ]` items)
3. **Execute** the next unchecked task following the plan
4. **Verify** all acceptance criteria before marking complete
5. **Update** the plan by checking `[ ]` boxes as you complete tasks

### Current Development Status

The system is in **implementation phase** - Catalog Context (Stage 2) is ~75% complete.

**Completed**:
- Project initialization (Stage 1)
- Catalog Context domain layer (IDs, ISBN, entities, aggregates, exceptions, repositories)
- Catalog Context application layer (commands, DTOs, application service)
- Catalog Context interface layer (BookController, AuthorController, GlobalExceptionHandler)
- Shared module (domain events, aggregate IDs)

**Next Step**: Continue with remaining Catalog Context tasks:
1. Task 2.3.2: 自定义仓储实现 (Custom repository with Criteria API)
2. Task 2.4.1: ISBNValidationService (external API integration)
3. Task 2.7.x: 基础设施层 (JPA mapping, database config)
4. Task 2.8.x: 事件驱动 (Kafka integration)
5. Task 2.9.x: 集成测试和CI/CD

### Development Stages Overview

| Stage | Context | Status | Time Estimate |
|-------|----------|--------|---------------|
| 1 | Project Initialization | **Complete** | 2 hours |
| 2 | Catalog Context | **~75% Done** | 50 hours |
| 3 | Inventory Context | Not Started | 20 hours |
| 4 | Circulation Context | Not Started | 25 hours |
| 5 | Patron Context | Not Started | 14 hours |
| 6 | Payment Context | Not Started | 14 hours |
| 7 | Analytics Context | Not Started | 10 hours |
| 8 | Notification Context | Not Started | 12 hours |
| 9 | Shared Module | **~50% Done** | 6 hours |
| 10 | Cross-Context Integration | Not Started | 22 hours |

**Total Estimated Time**: ~213 hours (5-6 weeks full-time)

## Development Workflow

1. **Before coding**: Read the relevant bounded context design document (02-08) in `Architecture_Design/`
2. **For Spring specifics**: Reference `10-Spring实现指南.md` for patterns and examples
3. **Follow DDD layering**: Respect domain/application/infrastructure/interfaces boundaries
4. **Use TDD**: Write tests first, implement to pass tests (80%+ coverage required)
   - Use **tdd-guide** agent
   - Write tests first (RED)
   - Implement to pass tests (GREEN)
   - Refactor (IMPROVE)
   - Verify 80%+ coverage
5. **Event-driven**: Use domain events for cross-context communication
6. **Transaction boundaries**: Use `@Transactional` appropriately, read-only for queries
7. **Versioning**: All aggregates must have `@Version` for optimistic locking
8. **Audit fields**: Include `created_at`, `updated_at`, `created_by`, `updated_by` where applicable
9. **Code Review**
   - Use **code-reviewer** agent immediately after writing code
   - Address CRITICAL and HIGH issues
   - Fix MEDIUM issues when possible
10. **Commit & Push**
   - Detailed commit messages
   - Follow conventional commits format
   - See [git-workflow.md](~/.claude/rules/common/git-workflow.md) for commit message format and PR process

## Security Guidelines

- No hardcoded secrets in code (use environment variables)
- All user inputs validated at system boundaries
- Parameterized queries for SQL injection prevention
- XSS prevention in user-facing code
- CSRF protection enabled
- Proper authentication/authorization checks

**Before ANY commit**, run through the security checklist in `~/.claude/rules/common/security.md`.
