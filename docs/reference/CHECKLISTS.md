# Development Checklists

> **Source**: Migrated from CLAUDE.md. Use these checklists when adding features or creating new domain constructs.

---

## 1. New Feature Development Checklist

When adding a new feature to an existing bounded context:

### Domain Layer (`domain/`)

- [ ] Entity/Value Object in `domain/model/`
- [ ] Domain event (if state-changing) in `domain/event/` extending `DomainEvent`
- [ ] Domain exception (if business rule) in `domain/exception/` extending `DomainException`
- [ ] Repository interface in `domain/repository/` extending `JpaRepository`

### Application Layer (`application/`)

- [ ] Command object in `application/command/`
- [ ] Application service in `application/service/`
- [ ] DTO + `ApiResponse<T>` mapping in `application/dto/`

### Infrastructure Layer (`infrastructure/`)

- [ ] Event publisher bean in `infrastructure/messaging/` (Double Publishing pattern)
- [ ] Custom repository impl (if needed) in `infrastructure/persistence/`

### Interface Layer (`interfaces/`)

- [ ] REST controller in `interfaces/rest/`
- [ ] Error handling via `GlobalExceptionHandler`

### Tests

- [ ] Domain unit tests (JUnit 5 + Mockito + AssertJ)
- [ ] Application service unit tests
- [ ] API integration tests (MockMvc + `@SpringBootTest`)
- [ ] If cross-context: E2E tests in all 3 test modules

---

## 2. Aggregate Root Checklist

Every new aggregate MUST have:

- [ ] `@EmbeddedId` with custom ID class extending `AggregateId`
- [ ] `@Version private Long version` for optimistic locking
- [ ] `created_at` and `updated_at` audit columns
- [ ] Static `create()` factory method (no public constructor)
- [ ] Business methods (no public setters for domain state)
- [ ] Domain exception for business rule violations (extending `DomainException`)

### Reference Implementation

See `library-catalog/src/main/java/com/library/catalog/domain/model/Book.java`

---

## 3. Event Publishing Checklist

For any state-changing operation:

- [ ] Domain event defined in `domain/event/` extending `DomainEvent`
- [ ] `*DomainEventPublisher` bean in `infrastructure/messaging/` (named `@Component`)
- [ ] Publisher uses `ObjectProvider<KafkaTemplate>` for optional Kafka injection
- [ ] Topic name configurable via `Environment.getProperty()`
- [ ] `try-catch` wrapping Kafka send, `whenComplete` for async logging
- [ ] Kafka graceful degradation: if `kafkaTemplate == null`, log debug and skip

### Reference Implementation

See `library-catalog/src/main/java/com/library/catalog/infrastructure/messaging/CatalogDomainEventPublisher.java`

---

## 4. Event Consuming Checklist

For new cross-context event handlers:

- [ ] Handler class in `application/handler/` with `handle(JsonNode event)` method
- [ ] Consumer class in `infrastructure/messaging/` with `@KafkaListener`
- [ ] Consumer uses `ObjectMapper.readTree()` + switch on `eventType`
- [ ] Unknown eventTypes logged at DEBUG, not thrown (prevents poison pill)
- [ ] Errors logged at ERROR, not re-thrown (prevents consumer crash loop)
- [ ] Consumer group ID follows pattern: `library.<context>.consumer.<source-context>`

### Reference Implementation

- Consumer: `library-patron/src/main/java/com/library/patron/infrastructure/messaging/CirculationEventConsumer.java`
- Handler: `library-patron/src/main/java/com/library/patron/application/handler/BookBorrowedEventHandler.java`

---

## 5. Cross-Context Event Flow Checklist

When adding a new event flow between bounded contexts:

- [ ] Event class in publisher's `domain/event/` extending `DomainEvent`
- [ ] Event added to publisher's `*DomainEventPublisher.publish()` calls
- [ ] Handler class in consumer's `application/handler/` with `@Transactional`
- [ ] Handler registered in consumer's `*EventConsumer` switch statement
- [ ] E2E test in `library-e2e-test` (JUnit 5)
- [ ] BDD test in `library-integration-test` (Cucumber `.feature` + `*Steps.java`)
- [ ] Staging test in `library-staging-test` (Cucumber, same `.feature` + adapted `*Steps.java`)

> **Rule**: If it touches Kafka events between bounded contexts → add to ALL THREE test modules.
