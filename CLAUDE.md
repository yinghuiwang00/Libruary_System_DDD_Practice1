# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise Library Management System built with Domain-Driven Design (DDD) principles.

**Status:** All 7 bounded contexts implemented - Shared module and cross-context integration remaining

## Architecture

### Bounded Contexts

| Module | Port | Description |
|--------|------|-------------|
| library-catalog | 8081 | Book info, ISBN, metadata, authors, publishers, categories |
| library-inventory | 8082 | Book copies, multi-branch inventory, library locations |
| library-circulation | 8083 | Borrow/return, holds/reservations, fines, renewals |
| library-patron | 8084 | Membership, authentication, borrowing permissions |
| library-payment | 8085 | Fine payments, third-party payment integration |
| library-analytics | 8086 | Statistics, popular books, reporting |
| library-notification | 8087 | Due reminders, reservation alerts, multi-channel |
| library-shared | - | Shared domain concepts (IDs, events, base classes) |

### DDD Layering (Actual Package Structure)

```
domain/
  ├── model/          # Aggregates, entities, value objects, enums (all in model/)
  │   └── enums/      # Enum classes
  ├── service/        # Domain services
  ├── repository/     # Repository interfaces (JPA repositories)
  ├── event/          # Domain events
  └── exception/      # Domain exceptions (with error codes)

application/
  ├── service/        # Application services (orchestration)
  ├── command/        # Command objects
  ├── query/          # Query/criteria objects
  └── dto/            # Data transfer objects + ApiResponse

infrastructure/
  ├── persistence/    # Custom repository implementations (Criteria API)
  ├── messaging/      # Kafka event publishers
  └── config/         # JPA, module-specific config

interfaces/
  └── rest/           # Controllers + GlobalExceptionHandler
```

### Key Patterns

- **Aggregate Root**: `@Entity` with `@EmbeddedId` (custom ID class), `@Version` for optimistic locking
- **Repository**: Interface extends `JpaRepository` + `CustomXxxRepository`, custom impl uses Criteria API
- **Domain Events**: Published via `CatalogDomainEventPublisher` (local Spring event + Kafka)
- **Transactions**: Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`
- **Exception Hierarchy**: `DomainException` base with error code, caught by `GlobalExceptionHandler`

## Technology Stack

- **Runtime**: Java 17, Spring Boot 3.2.5
- **Database**: PostgreSQL (prod) / H2 (test), Spring Data JPA, Hibernate
- **Messaging**: Apache Kafka (spring-kafka)
- **Build**: Maven multi-module
- **API Docs**: SpringDoc OpenAPI (Swagger UI per module)
- **Testing**: JUnit 5, Mockito, AssertJ, Cucumber BDD, MockMvc

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
```

## Development Progress

**Plan**: See `DEVELOPMENT_PLAN.md` for detailed task breakdown with acceptance criteria.
**Design Docs**: See `Architecture_Design/` for bounded context designs (02-08), Spring guide (10), test plan (15).

### How to Continue

1. Open `DEVELOPMENT_PLAN.md`, find next unchecked task
2. Read the corresponding design doc in `Architecture_Design/`
3. Reference `10-Spring实现指南.md` for Spring patterns
4. Follow TDD: write test first, implement, verify 80%+ coverage
5. Check off acceptance criteria in the plan

### Current Status

| Stage | Context | Status | Tests |
|-------|---------|--------|-------|
| 1 | Project Initialization | **Complete** | - |
| 2 | Catalog Context | **Complete** | ~91+ (unit + integration + 4 BDD features) |
| 3 | Inventory Context | **Complete** | 65 (55 unit + 7 integration + 3 BDD features) |
| 4 | Circulation Context | **Complete** | ~62 (54 domain + 5 integration + 1 BDD feature) |
| 5 | Patron Context | **Complete** | 156 (142 unit + 13 integration + 1 BDD feature) |
| 6 | Payment Context | **Complete** | 138 (92 unit + 33 service + 13 integration + 1 BDD feature) |
| 7 | Analytics Context | **Complete** | 133 (71 unit + 26 service + 19 integration + 6 BDD scenarios) |
| 8 | Notification Context | **Complete** | 109 (domain + service + integration + 3 BDD scenarios) |
| 9 | Shared Module | **Complete** | 84 (IDs + events + 4 value objects) |
| 10 | Cross-Context Integration | Not started | 0 |

**Next Task**: Cross-Context Integration (Kafka, Saga, API Gateway).

### Shared Module (library-shared)

Already implemented:
- `AggregateId` base class + 16 ID types: BookId, AuthorId, PublisherId, CategoryId, LibraryId, CopyId, CopyInventoryId, LoanId, HoldId, FineId, PatronId, PaymentId, RefundId, ReportId, DashboardId, NotificationId
- `DomainEvent` base class + `DomainEventPublisher` interface
- Value objects: `Money` (amount + currency, arithmetic), `Email` (validation + normalize), `PhoneNumber` (format validation), `Address` (street/city/postalCode/state/country)

## Development Workflow

1. **Before coding**: Read relevant design doc (02-08) in `Architecture_Design/`
2. **DDD layering**: Respect domain/application/infrastructure/interfaces boundaries
3. **TDD**: Write tests first (RED) -> implement (GREEN) -> refactor (IMPROVE), 80%+ coverage
4. **Domain events**: Publish for all state-changing operations
5. **Transactions**: `@Transactional(readOnly = true)` on services, `@Transactional` on writes
6. **Optimistic locking**: All aggregates must have `@Version`
7. **Audit fields**: `created_at`, `updated_at` on all entities
8. **Code review**: Use code-reviewer agent after writing code
9. **Progress**: Update DEVELOPMENT_PLAN.md checkboxes; save summary in `Progress/`

## Testing Strategy

- **Unit Tests**: Domain models, services, value objects (JUnit 5 + Mockito + AssertJ)
- **Integration Tests**: API endpoints with MockMvc + H2 (`@SpringBootTest`)
- **Functional Tests (Cucumber BDD)**: Happy path flows only; edge cases in unit tests
- **Test Plan**: See `Architecture_Design/15-TESTPLAN.md`
- **Coverage**: 80%+ required
- **Cucumber deps**: `cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`

## Infrastructure

开发环境统一凭据（仅在开发环境使用，生产环境另行配置）：

| 参数 | 值 |
|------|----|
| PostgreSQL 用户名 | `postgres` |
| PostgreSQL 密码 | `dev_pg_2026` |
| PostgreSQL 端口 | `5432` |
| Redis 密码 | `dev_redis_2026` |
| Redis 端口 | `6379` |
| Kafka 端口（主机访问） | `29092` |
| Kafka 端口（容器网络） | `9092` |
| Kafka UI 地址 | `http://localhost:9000` |
| Prometheus 地址 | `http://localhost:9090` |
| Grafana 地址 | `http://localhost:3000` |
| Grafana 用户名 | `admin` |
| Grafana 密码 | `dev_grafana_2026` |
| Jaeger UI 地址 | `http://localhost:16686` |
| Jaeger OTLP (gRPC) | `localhost:4317` |
| Jaeger OTLP (HTTP) | `localhost:4318` |

Credentials are in environment variables, not hardcoded in source.

---
