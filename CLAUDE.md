# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise Library Management System — 7 bounded contexts + shared module, built with DDD, Spring Boot 3.2.5, Kafka.

**Status**: All 12 stages complete. 11 Maven modules, 318 main Java files, 163 test files, 46 BDD features. CI: build (H2) + staging (PostgreSQL + Kafka).

---

## ⚠️ CRITICAL RULES

### library-shared Stability (ALL contexts depend on it)

1. **NEVER** remove/rename public class, method, or field in `library-shared`
2. **NEVER** change `AggregateId` constructor or `DomainEvent` JSON field names (`eventId`, `occurredAt`, `eventType`, `version`)
3. Only **ADD**; deprecate-then-remove is the only safe removal path

> Full detail → [docs/reference/BACKWARD_COMPATIBILITY.md](docs/reference/BACKWARD_COMPATIBILITY.md)

### Kafka Event Contract

1. **NEVER** rename an existing `eventType` string (breaks all consumer `switch` statements)
2. **NEVER** remove a JSON field consumers read or change a field's type
3. Adding new fields / eventTypes is safe

> Full detail → [docs/reference/BACKWARD_COMPATIBILITY.md](docs/reference/BACKWARD_COMPATIBILITY.md)

### Module Boundaries

1. **NEVER** add compile dependency between bounded contexts (use Kafka events)
2. Only `library-shared` and test modules (`library-e2e-test`, `library-integration-test`, `library-staging-test`) may depend on multiple contexts
3. New cross-context communication **MUST** go through Kafka events, NOT direct API calls

---

## Quick-Start Paths

### "I need to add a feature to an existing context"

1. Identify the bounded context → `DDD_Explanation/{05-11}` for that context's patterns
2. Follow the layer-by-layer checklist → [docs/reference/CHECKLISTS.md](docs/reference/CHECKLISTS.md)
3. Write tests per layer → [docs/reference/TESTING_GUIDE.md](docs/reference/TESTING_GUIDE.md)

### "I need to add a cross-context event flow"

1. Define event in publisher's `domain/event/` extending `DomainEvent`
2. Publish via publisher's `*DomainEventPublisher` → see `library-catalog/.../messaging/CatalogDomainEventPublisher.java`
3. Create handler in consumer's `application/handler/` → see `library-patron/.../handler/BookBorrowedEventHandler.java`
4. Create consumer in consumer's `infrastructure/messaging/` → see `library-patron/.../messaging/CirculationEventConsumer.java`
5. Add E2E tests to all 3 test modules → [docs/reference/TESTING_GUIDE.md](docs/reference/TESTING_GUIDE.md)

### "I need to fix a bug"

1. Identify the bounded context and DDD layer
2. Check `DDD_Explanation/{05-11}` for that context's architecture
3. Write a failing test first, then fix

### "I need to add a test"

→ [docs/reference/TESTING_GUIDE.md](docs/reference/TESTING_GUIDE.md) — test types, location rules, Cucumber conventions

### "I need to create a new bounded context"

→ [docs/guides/NEW_BOUNDED_CONTEXT.md](docs/guides/NEW_BOUNDED_CONTEXT.md) — full step-by-step guide

---

## Architecture at a Glance

### Module Map

| Module | Port | Description | Events Published | Kafka Consumers |
|--------|------|-------------|:---:|:---:|
| library-shared | — | Shared IDs, events, value objects | 1 (base) | 0 |
| library-catalog | 8081 | Books, ISBN, authors, publishers, categories | 4 | 0 |
| library-inventory | 8082 | Book copies, multi-branch inventory, libraries | 8 | 2 |
| library-circulation | 8083 | Borrow/return, holds, fines, renewals | 14 | 1 |
| library-patron | 8084 | Membership, borrowing permissions | 6 | 2 |
| library-payment | 8085 | Fine payments, refunds | 6 | 1 |
| library-analytics | 8086 | Statistics, reporting | 4 | 2 |
| library-notification | 8087 | Due reminders, alerts, multi-channel | 4 | 4 |

Test modules: `library-e2e-test` (JUnit 5), `library-integration-test` (Cucumber BDD), `library-staging-test` (Cucumber + real infra)

### DDD Layering (Package Structure)

```
domain/
  ├── model/          # Aggregates, entities, value objects, enums
  │   └── enums/
  ├── service/        # Domain services
  ├── repository/     # Repository interfaces (JPA)
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

---

## Key Patterns (see actual code)

| Pattern | Reference File |
|---------|---------------|
| Aggregate Root | `library-catalog/src/main/java/com/library/catalog/domain/model/Book.java` |
| Domain Event | `library-circulation/src/main/java/com/library/circulation/domain/event/BookBorrowedEvent.java` |
| Double Publishing (local + Kafka) | `library-catalog/src/main/java/com/library/catalog/infrastructure/messaging/CatalogDomainEventPublisher.java` |
| Kafka Consumer (anti-corruption) | `library-patron/src/main/java/com/library/patron/infrastructure/messaging/CirculationEventConsumer.java` |
| Event Handler | `library-patron/src/main/java/com/library/patron/application/handler/BookBorrowedEventHandler.java` |
| REST Controller + Exception Handling | `library-catalog/src/main/java/com/library/catalog/interfaces/rest/BookController.java` |

Pattern explanations: `DDD_Explanation/01` (layers), `/02` (aggregates), `/03` (events), `/04` (testing)

---

## Build and Test Commands

```bash
mvn clean install                      # Build all modules
mvn clean install -DskipTests          # Build without tests
cd library-catalog && mvn clean install # Build specific module
mvn test                               # Run all tests
cd library-circulation && mvn test     # Test specific module
mvn test -Dtest=LoanTest               # Run specific test class
mvn test -Dtest=LoanTest#testCheckout   # Run specific test method
cd library-catalog && mvn spring-boot:run  # Run a module
cd library-e2e-test && mvn test        # E2E tests (H2 + EmbeddedKafka)
mvn test -Pstaging -pl library-staging-test  # Staging tests (PostgreSQL + Kafka)
```

---

## Development Workflow

### Stage Commit & Push

每完成一个阶段性工作，立即 commit + push：

```bash
git add . && git commit -m "<type>: <description>" && git push origin main
```

**Commit types**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`
**Skip CI**: `[skip ci]` or `[skip build]` in commit message

### Before Writing Any Code

| Question | Document |
|----------|----------|
| Which context am I modifying? | `Architecture_Design/02-08` (per-context design) |
| How to implement in Spring? | `Architecture_Design/10` (Spring guide) |
| What DDD patterns to follow? | `DDD_Explanation/01-04` (layers, aggregates, events, testing) |
| How to test? | `Architecture_Design/15` (test strategy) |
| Is it cross-context? | `Architecture_Design/16` (Kafka strategy) |

---

## Testing Rules

| Layer | Framework | Scope |
|-------|-----------|-------|
| Domain | JUnit 5 + Mockito + AssertJ | Aggregates, value objects, domain services |
| Application | JUnit 5 + Mockito | Services, event handlers |
| API | MockMvc + H2 `@SpringBootTest` | REST controllers |
| Cross-context | Cucumber 7.15.0 BDD | `.feature` + `*Steps.java` |
| Staging | Cucumber + PostgreSQL + Kafka | Real infrastructure |

**When to add cross-context tests**: If it touches Kafka events between bounded contexts → add to ALL THREE test modules (`library-e2e-test`, `library-integration-test`, `library-staging-test`).

**Coverage target**: 80%+ per module.

> Full testing guide → [docs/reference/TESTING_GUIDE.md](docs/reference/TESTING_GUIDE.md)

---

## CI Pipeline

```
push/PR → main
├── build (H2 + EmbeddedKafka, ~4 min)
│   └── Test all 10 modules → upload surefire-reports
└── staging (PostgreSQL + Kafka, ~2 min) — needs: build ✅
    └── Test library-staging-test → upload staging-test-reports
```

Both jobs must pass. Workflow: `.github/workflows/ci.yml`

> CI details → [docs/reference/CI_DETAILS.md](docs/reference/CI_DETAILS.md)

---

## Technology Stack

- **Runtime**: Java 17, Spring Boot 3.2.5
- **Database**: PostgreSQL (prod) / H2 PostgreSQL mode (test), Spring Data JPA, Hibernate
- **Messaging**: Apache Kafka (spring-kafka), EmbeddedKafka for tests
- **Build**: Maven multi-module (11 child modules)
- **API Docs**: SpringDoc OpenAPI 2.5.0 (Swagger UI per module)
- **Testing**: JUnit 5, Mockito, AssertJ, Awaitility, Cucumber 7.15.0, MockMvc
- **CI**: GitHub Actions (build + staging jobs)

---

## Implementation Status

**All 12 stages complete.** See [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) for detailed progress.

---

## Documentation Index

### Design & Architecture

| Purpose | Document |
|---------|----------|
| Per-context design | `Architecture_Design/02-08` |
| Spring implementation guide | `Architecture_Design/10` |
| Overall domain model | `Architecture_Design/09` |
| Architecture Decision Records | `Architecture_Design/11` |

### DDD Implementation Patterns

| Purpose | Document |
|---------|----------|
| 4-layer architecture | `DDD_Explanation/01-DDD分层架构实现.md` |
| Aggregate root patterns | `DDD_Explanation/02-聚合根模式实现.md` |
| Event-driven architecture | `DDD_Explanation/03-事件驱动架构实现.md` |
| Testing strategy | `DDD_Explanation/04-测试策略实现.md` |
| Per-context reference | `DDD_Explanation/05-13` |
| Cross-context integration | `DDD_Explanation/13-跨上下文集成实现.md` |

### Testing & CI

| Purpose | Document |
|---------|----------|
| Test strategy (detailed) | `Architecture_Design/15-Test-Strategy.md` |
| Kafka strategy | `Architecture_Design/16-Kafka-Strategy.md` |
| Staging test strategy | `Architecture_Design/17-Staging-Test-Strategy.md` |
| Testing guide (practical) | [docs/reference/TESTING_GUIDE.md](docs/reference/TESTING_GUIDE.md) |
| CI details | [docs/reference/CI_DETAILS.md](docs/reference/CI_DETAILS.md) |

### Operational Reference

| Purpose | Document |
|---------|----------|
| Development checklists | [docs/reference/CHECKLISTS.md](docs/reference/CHECKLISTS.md) |
| Backward compatibility rules | [docs/reference/BACKWARD_COMPATIBILITY.md](docs/reference/BACKWARD_COMPATIBILITY.md) |
| New bounded context guide | [docs/guides/NEW_BOUNDED_CONTEXT.md](docs/guides/NEW_BOUNDED_CONTEXT.md) |
| Local setup guide | [docs/guides/LOCAL_SETUP.md](docs/guides/LOCAL_SETUP.md) |
| Troubleshooting | [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md) |

---

## Local Dev Environment

| Service | Address |
|---------|---------|
| PostgreSQL | `localhost:5432`, user `postgres` |
| Kafka (host) | `localhost:29092` |
| Kafka UI | http://localhost:9000 |
| Redis | `localhost:6379` |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Jaeger | http://localhost:16686 |

Credentials are in environment variables, not hardcoded in source.

> Local setup guide → [docs/guides/LOCAL_SETUP.md](docs/guides/LOCAL_SETUP.md)

---

## Known Issues

1. **Password inconsistency**: `catalog`/`circulation`/`inventory` default to `dev_pg_2026`; `patron`/`payment`/`analytics`/`notification` default to `postgres`. Use env var `DB_PASSWORD` to override consistently.
2. **File counts are approximate**: 318 main Java files, 163 test files, 46 feature files. May drift as code evolves.
