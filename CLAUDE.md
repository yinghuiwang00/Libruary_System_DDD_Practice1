# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise Library Management System built with Domain-Driven Design (DDD) principles.

**Status:** All 7 bounded contexts + shared module + cross-context integration (Kafka) implemented. 11 Maven modules, 318 main Java files, 152 test artifacts.

## Architecture

### Module Map

| Module | Port | Description | Events | Kafka Consumers |
|--------|------|-------------|--------|-----------------|
| library-shared | - | Shared IDs, events, value objects | 1 (base) | 0 |
| library-catalog | 8081 | Books, ISBN, authors, publishers, categories | 4 | 0 |
| library-inventory | 8082 | Book copies, multi-branch inventory, libraries | 8 | 2 |
| library-circulation | 8083 | Borrow/return, holds, fines, renewals | 14 | 1 |
| library-patron | 8084 | Membership, borrowing permissions | 6 | 2 |
| library-payment | 8085 | Fine payments, refunds | 6 | 1 |
| library-analytics | 8086 | Statistics, reporting | 4 | 2 |
| library-notification | 8087 | Due reminders, alerts, multi-channel | 4 | 4 |
| library-e2e-test | - | JUnit 5 E2E tests (9 tests) | - | - |
| library-integration-test | - | Cucumber BDD E2E tests (9 scenarios) | - | - |
| library-staging-test | - | Cucumber BDD staging tests (9 scenarios, real infra) | - | - |

### DDD Layering (Actual Package Structure)

```
domain/
  ├── model/          # Aggregates, entities, value objects, enums
  │   └── enums/      # Enum classes
  ├── service/        # Domain services
  ├── repository/     # Repository interfaces (JPA repositories)
  ├── event/          # Domain events
  └── exception/      # Domain exceptions (with error codes)

application/
  ├── service/        # Application services (orchestration)
  ├── handler/        # Cross-context event handlers
  ├── command/        # Command objects
  ├── query/          # Query/criteria objects
  └── dto/            # Data transfer objects + ApiResponse

infrastructure/
  ├── persistence/    # Custom repository implementations (Criteria API)
  ├── messaging/      # Kafka publishers (*DomainEventPublisher) + consumers (*EventConsumer)
  └── config/         # JPA, module-specific config

interfaces/
  └── rest/           # Controllers + GlobalExceptionHandler
```

### Key Patterns

- **Aggregate Root**: `@Entity` with `@EmbeddedId` (custom ID class extending `AggregateId`), `@Version` for optimistic locking, static `create()` factory method
- **Repository**: Interface extends `JpaRepository` + optional `CustomXxxRepository`; custom impl uses Criteria API
- **Double Publishing**: Each `*DomainEventPublisher` publishes to both Spring `ApplicationEventPublisher` (local, sync, always succeeds) and Kafka `KafkaTemplate` (async, optional via `ObjectProvider`, graceful degradation)
- **Kafka Consumer**: `@KafkaListener` receives `ConsumerRecord<String, String>` → `ObjectMapper.readTree()` → switch on `eventType` → route to handler
- **Transactions**: Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`
- **Exception Hierarchy**: `DomainException` base with error code, caught by `GlobalExceptionHandler` → `ApiResponse<T>`

## Backward Compatibility Rules (CRITICAL)

### library-shared Is a Stability Contract

`library-shared` is depended on by ALL 7 bounded contexts. Changes here cascade everywhere.

**MUST follow these rules:**

1. **NEVER remove or rename** a public class/method/field in `library-shared`
2. **NEVER change** the constructor signature of `AggregateId` or any ID subclass
3. **NEVER change** the JSON field names in `DomainEvent` (`eventId`, `occurredAt`, `eventType`, `version`)
4. **NEVER change** the database column name `id` in `AggregateId`
5. **Adding** new ID types, value objects, or methods is safe
6. **Deprecating** then removing in a later version is the only safe removal path

### Kafka Event JSON Contract

Consumers parse events as `ConsumerRecord<String, String>` JSON. The contract is:

```json
{
  "eventType": "BookBorrowedEvent",   // REQUIRED — consumers switch on this
  "eventId": "uuid",                  // from DomainEvent base
  "patronId": {"value": "string"},    // ID fields are always {"value": "..."}
  "bookId": "string",                 // or plain string depending on event
  "amount": 15.00                     // numeric fields are plain numbers
}
```

**MUST follow these rules:**

1. **NEVER rename** an existing `eventType` string (breaks all consumers' switch statements)
2. **NEVER remove** a JSON field that consumers read (check all handlers before removing)
3. **NEVER change** the type of a JSON field (string→number, object→string, etc.)
4. **Adding** new fields is safe (consumers ignore unknown fields)
5. **Adding** new eventTypes is safe (consumers have `default -> log.debug("Ignoring...")`)
6. **New Kafka topics** must be registered in `EmbeddedKafkaConfig` and `@EmbeddedKafka` annotations

### Aggregate Schema Compatibility

All aggregates use `@EmbeddedId` + `@Version`. Database schema changes:

1. **NEVER remove or rename** a column that maps to an `@EmbeddedId` field
2. **NEVER change** `@Column(name=...)` mappings on existing fields
3. **Adding** new columns with defaults or nullable is safe
4. **Adding** new `@Embedded` value objects is safe if columns don't conflict

### Module Boundary Rules

1. **NEVER add a compile dependency** from one bounded context to another (use Kafka events instead)
2. **NEVER import** `com.library.catalog.domain.*` from `library-patron` or vice versa
3. Only `library-shared` and `library-integration-test`/`library-e2e-test` may depend on multiple contexts
4. New cross-context communication MUST go through Kafka events, NOT direct API calls

## Development Rules

### Before Writing Any Code

1. Check `Architecture_Design/` for the relevant bounded context design doc (02-08)
2. Check `DDD_Explanation/` for implementation patterns used in this project
3. Identify which bounded context you're modifying — stay within its boundary
4. If adding cross-context behavior: define the event first, then consumer

### Aggregate Root Checklist

Every new aggregate MUST have:
- [ ] `@EmbeddedId` with custom ID class extending `AggregateId`
- [ ] `@Version private Long version` for optimistic locking
- [ ] `created_at` and `updated_at` audit columns
- [ ] Static `create()` factory method (no public constructor)
- [ ] Business methods (no public setters for domain state)
- [ ] Domain exception for business rule violations (extending `DomainException`)

### Event Publishing Checklist

For any state-changing operation:
- [ ] Domain event defined in `domain/event/` extending `DomainEvent`
- [ ] `*DomainEventPublisher` bean in `infrastructure/messaging/` (named bean)
- [ ] Publisher uses `ObjectProvider<KafkaTemplate>` for optional Kafka
- [ ] Topic name configurable via `Environment.getProperty()`
- [ ] `try-catch` wrapping Kafka send, `whenComplete` for async logging

### Event Consuming Checklist

For new cross-context event handlers:
- [ ] Handler class in `application/handler/` with `handle(JsonNode event)` method
- [ ] Consumer class in `infrastructure/messaging/` with `@KafkaListener`
- [ ] Consumer uses `ObjectMapper.readTree()` + switch on `eventType`
- [ ] Unknown eventTypes logged at DEBUG, not thrown
- [ ] Errors logged at ERROR, not re-thrown (prevent poison pill)

### Testing Requirements

1. **Unit Tests**: Domain models, services, value objects (JUnit 5 + Mockito + AssertJ)
2. **Integration Tests**: API endpoints with MockMvc + H2 (`@SpringBootTest`)
3. **BDD Tests**: Happy path flows via Cucumber (`.feature` + `*Steps.java`)
4. **E2E Tests**: If cross-context, add scenario to BOTH `library-e2e-test` AND `library-integration-test`
5. **Coverage**: 80%+ required
6. **Cucumber deps**: `cucumber-java:7.15.0`, `cucumber-spring:7.15.0`, `cucumber-junit-platform-engine:7.15.0`
7. **Surefire**: Must include `<include>**/CucumberTestSuite.java</include>` in surefire config

## Technology Stack

- **Runtime**: Java 17, Spring Boot 3.2.5
- **Database**: PostgreSQL (prod) / H2 in PostgreSQL mode (test), Spring Data JPA, Hibernate
- **Messaging**: Apache Kafka (spring-kafka), EmbeddedKafka for tests
- **Build**: Maven multi-module (parent pom + 11 child modules)
- **API Docs**: SpringDoc OpenAPI 2.5.0 (Swagger UI per module)
- **Testing**: JUnit 5, Mockito, AssertJ, Awaitility, Cucumber 7.15.0, MockMvc

## Build and Test Commands

```bash
# Build all modules
mvn clean install

# Build specific module
cd library-catalog && mvn clean install

# Run all tests
mvn test

# Run tests for specific module
cd library-circulation && mvn test

# Run specific test class
mvn test -Dtest=LoanTest

# Run specific test method
mvn test -Dtest=LoanTest#testCheckout

# Skip tests
mvn clean install -DskipTests

# Run application (per module)
cd library-catalog && mvn spring-boot:run

# Run E2E tests only
cd library-e2e-test && mvn test
cd library-integration-test && mvn test

# Run staging tests (requires Docker: PostgreSQL + Kafka)
mvn test -Pstaging -pl library-staging-test
```

## Implementation Progress

| Stage | Context | Status | Tests |
|-------|---------|--------|-------|
| 1 | Project Initialization | ✅ Complete | - |
| 2 | Catalog Context | ✅ Complete | 22 unit + 5 steps + 4 features |
| 3 | Inventory Context | ✅ Complete | 9 unit + 4 steps + 5 features |
| 4 | Circulation Context | ✅ Complete | 7 unit + 1 steps + 2 features |
| 5 | Patron Context | ✅ Complete | 7 unit + 6 steps + 8 features |
| 6 | Payment Context | ✅ Complete | 6 unit + 2 steps + 2 features |
| 7 | Analytics Context | ✅ Complete | 7 unit + 3 steps + 3 features |
| 8 | Notification Context | ✅ Complete | 7 unit + 7 steps + 6 features |
| 9 | Shared Module | ✅ Complete | 6 unit tests |
| 10 | Cross-Context Integration | ✅ Partial | 9 E2E JUnit5 + 9 E2E BDD |
| 11 | Staging Test (Real Infra) | ✅ Complete | 9 BDD scenarios (PostgreSQL + Kafka) |
| 12 | CI Staging Job (GitHub Actions) | ✅ Complete | build (H2) + staging (PostgreSQL+Kafka) 双 job |

**Remaining**: Saga coordinator, API Gateway, distributed tracing.

### Shared Module (library-shared)

- `AggregateId` base class + 16 ID types
- `DomainEvent` base class + `DomainEventPublisher` interface
- Value objects: `Money`, `Email`, `PhoneNumber`, `Address`

## Key File Locations

| What | Where |
|------|-------|
| Design docs | `Architecture_Design/02-08` (per context), `09` (overall), `10` (Spring guide) |
| Implementation patterns | `DDD_Explanation/` (4 docs on layering, aggregates, events, testing) |
| E2E BDD plan | `Architecture_Design/E2E-BDD-Migration-Plan.md` |
| Test Strategy | `Architecture_Design/15-Test-Strategy.md` |
| Staging Test Strategy | `Architecture_Design/17-Staging-Test-Strategy.md` |
| Kafka strategy | `Architecture_Design/Kafka-Strategy.md` |
| Development plan | `DEVELOPMENT_PLAN.md` |


## Infrastructure (Dev Environment)

| Parameter | Value |
|-----------|-------|
| PostgreSQL | port 5432, user `postgres` |
| Redis | port 6379 |
| Kafka (host) | port 29092 |
| Kafka (container) | port 9092 |
| Kafka UI | http://localhost:9000 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000, user `admin` |
| Jaeger UI | http://localhost:16686 |

Credentials are in environment variables, not hardcoded in source.
