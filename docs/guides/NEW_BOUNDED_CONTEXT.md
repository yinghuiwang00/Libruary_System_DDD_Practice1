# Guide: Adding a New Bounded Context

> Step-by-step guide to adding a brand new bounded context module to the Library Management System.

---

## Prerequisites

Before starting, review:
- `Architecture_Design/10-Spring实现指南.md` for Spring implementation patterns
- `DDD_Explanation/01-DDD分层架构实现.md` for the 4-layer architecture
- Any existing bounded context (e.g., `library-catalog`) as reference

---

## Steps

### Step 1: Create Maven Module

1. Create directory: `library-<context>/`
2. Create `library-<context>/pom.xml` inheriting from parent:

```xml
<parent>
    <groupId>com.library</groupId>
    <artifactId>library-system</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>
<artifactId>library-<context></artifactId>
<dependencies>
    <dependency>
        <groupId>com.library</groupId>
        <artifactId>library-shared</artifactId>
        <version>${project.version}</version>
    </dependency>
    <!-- Spring Boot, Kafka, JPA, Swagger, Test deps — copy from library-catalog/pom.xml -->
</dependencies>
```

3. Add module to parent `pom.xml` in both profiles (`default` and `staging`)

### Step 2: Create Package Structure

```
library-<context>/src/main/java/com/library/<context>/
├── domain/
│   ├── model/          # Aggregates, entities, value objects, enums
│   │   └── enums/
│   ├── service/        # Domain services
│   ├── repository/     # Repository interfaces (JPA)
│   ├── event/          # Domain events
│   └── exception/      # Domain exceptions
├── application/
│   ├── service/        # Application services
│   ├── handler/        # Cross-context event handlers
│   ├── command/        # Command objects
│   ├── query/          # Query/criteria objects
│   └── dto/            # DTOs + ApiResponse
├── infrastructure/
│   ├── persistence/    # Custom repository implementations
│   ├── messaging/      # Kafka publishers + consumers
│   └── config/         # JPA, module config
└── interfaces/
    └── rest/           # Controllers + GlobalExceptionHandler
```

### Step 3: Add ID Type to library-shared

In `library-shared/src/main/java/com/library/shared/domain/model/`:

```java
@Embeddable
public class <Entity>Id extends AggregateId {
    // Follow existing ID pattern (BookId, PatronId, etc.)
}
```

### Step 4: Implement Domain Layer

**4a. Aggregate Root** — `domain/model/<Entity>.java`
- `@Entity` with `@EmbeddedId` (`<Entity>Id extends AggregateId`)
- `@Version private Long version`
- Static `create()` factory method
- Business methods (no public setters)
- `created_at` / `updated_at` audit columns
- Reference: `library-catalog/.../domain/model/Book.java`

**4b. Domain Events** — `domain/event/`
- Extend `DomainEvent` from `library-shared`
- Include all relevant IDs and data
- Reference: `library-circulation/.../domain/event/BookBorrowedEvent.java`

**4c. Domain Exceptions** — `domain/exception/`
- Extend `DomainException` with error code
- One exception per business rule violation
- Reference: `library-circulation/.../domain/exception/`

**4d. Repository Interface** — `domain/repository/`
- Extend `JpaRepository<Entity, EntityId>`
- Optional: `CustomXxxRepository` for complex queries
- Reference: `library-catalog/.../domain/repository/BookRepository.java`

### Step 5: Implement Application Layer

**5a. Commands** — `application/command/`
- Immutable data classes for write operations
- Reference: `library-circulation/.../application/command/BorrowBookCommand.java`

**5b. DTOs** — `application/dto/`
- Data classes with `static fromDomain()` factory
- Reference: `library-circulation/.../application/dto/LoanDTO.java`

**5c. Application Service** — `application/service/`
- `@Transactional(readOnly = true)` at class level
- `@Transactional` on write methods
- Orchestrates domain objects, no business logic
- Reference: `library-catalog/.../application/service/BookApplicationService.java`

### Step 6: Implement Infrastructure Layer

**6a. Kafka Event Publisher** — `infrastructure/messaging/`
- Named `@Component("<context>DomainEventPublisher")`
- Double Publishing: Spring local + Kafka async
- `ObjectProvider<KafkaTemplate>` for optional Kafka
- Topic from `Environment.getProperty()`
- Reference: `library-catalog/.../messaging/CatalogDomainEventPublisher.java`

**6b. Kafka Event Consumer** (if consuming other context events) — `infrastructure/messaging/`
- `@KafkaListener(topics = "library.<source>.events", groupId = "library.<context>.consumer.<source>")`
- `ObjectMapper.readTree()` + `switch (eventType)`
- Route to handlers in `application/handler/`
- Reference: `library-patron/.../messaging/CirculationEventConsumer.java`

**6c. Event Handlers** — `application/handler/`
- `@Transactional handle(JsonNode event)`
- Reference: `library-patron/.../handler/BookBorrowedEventHandler.java`

### Step 7: Implement Interface Layer

**7a. REST Controller** — `interfaces/rest/`
- `@RestController` + `@RequestMapping("/api/<context>")`
- SpringDoc OpenAPI annotations
- Returns `ApiResponse<T>`
- Reference: `library-catalog/.../rest/BookController.java`

**7b. GlobalExceptionHandler** — `interfaces/rest/`
- `@RestControllerAdvice`
- Catch `DomainException` → `ApiResponse<T>` with error
- Reference: `library-catalog/.../rest/GlobalExceptionHandler.java`

### Step 8: Configure Application

**8a. `application.yml`** — `src/main/resources/`

```yaml
server:
  port: 808<N>  # Next available port
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/library_<context>
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update
  kafka:
    bootstrap-servers: ${SPRING.KAFKA.BOOTSTRAP-SERVERS:localhost:29092}
    producer:
      properties:
        spring.kafka.topic.domain-events: library.<context>.events
```

**8b. Test `application.yml`** — `src/test/resources/`
- H2 in PostgreSQL mode
- EmbeddedKafka
- Reference: any existing module's test `application.yml`

### Step 9: Write Tests

Follow the testing guide at `docs/reference/TESTING_GUIDE.md`:

1. Domain unit tests (JUnit 5 + Mockito + AssertJ)
2. Application service unit tests
3. API integration tests (MockMvc + `@SpringBootTest`)
4. If cross-context: E2E tests in all 3 test modules

### Step 10: Register Kafka Topic (if needed)

1. Add topic to `EmbeddedKafkaConfig` annotations in test modules
2. Add topic to staging test's `@BeforeAll` topic creation
3. Verify consumer group IDs are unique

---

## Verification Checklist

- [ ] `mvn clean install` succeeds for new module
- [ ] `mvn test` passes for new module
- [ ] Swagger UI accessible at `http://localhost:<port>/swagger-ui.html`
- [ ] Kafka events published/consumed correctly
- [ ] No compile dependency on other bounded contexts
- [ ] All checklists in `docs/reference/CHECKLISTS.md` completed
