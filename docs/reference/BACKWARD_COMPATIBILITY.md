# Backward Compatibility Rules

> **Source**: Migrated from CLAUDE.md. These rules are CRITICAL — violations break existing bounded contexts, Kafka consumers, or database schemas.

---

## 1. library-shared Stability Contract

`library-shared` is depended on by ALL 7 bounded contexts. Changes here cascade everywhere.

### MUST NOT

1. **NEVER remove or rename** a public class, method, or field in `library-shared`
2. **NEVER change** the constructor signature of `AggregateId` or any ID subclass (e.g., `BookId`, `PatronId`)
3. **NEVER change** the JSON field names in `DomainEvent`: `eventId`, `occurredAt`, `eventType`, `version`
4. **NEVER change** the database column name `id` in `AggregateId`

### Safe Operations

5. **Adding** new ID types, value objects, or methods is safe — existing code ignores them
6. **Deprecating** then removing in a later version is the only safe removal path

### Why

All bounded contexts depend on `library-shared` for:
- ID types (`AggregateId` subclasses) used as `@EmbeddedId` in every aggregate
- `DomainEvent` base class used by all 46 domain events
- Value objects (`Money`, `Email`, `PhoneNumber`, `Address`) used across contexts

A breaking change here requires updating all 7 contexts simultaneously.

---

## 2. Kafka Event JSON Contract

Consumers parse events as `ConsumerRecord<String, String>` JSON using `ObjectMapper.readTree()`. The contract is:

```json
{
  "eventType": "BookBorrowedEvent",
  "eventId": "uuid",
  "patronId": {"value": "string"},
  "bookId": "string",
  "amount": 15.00
}
```

### MUST NOT

1. **NEVER rename** an existing `eventType` string — all consumers use `switch (eventType)` and will silently ignore unknown types
2. **NEVER remove** a JSON field that consumers read — check all handlers before removing
3. **NEVER change** the type of a JSON field (string→number, object→string, etc.) — Jackson deserialization will fail

### Safe Operations

4. **Adding** new fields is safe — consumers use `JsonNode` and only read known fields
5. **Adding** new eventTypes is safe — consumers have `default -> log.debug("Ignoring event type: {}", eventType)`
6. **New Kafka topics** must be registered in `EmbeddedKafkaConfig` and `@EmbeddedKafka` annotations in test modules

### Why

12 `@KafkaListener` consumers across 5 topics route events to 19 handlers via `switch (eventType)`. Any rename breaks the routing silently (event gets logged as "Ignoring" instead of processed).

---

## 3. Aggregate Schema Compatibility

All aggregates use `@EmbeddedId` + `@Version` for optimistic locking. Database schema changes:

### MUST NOT

1. **NEVER remove or rename** a column that maps to an `@EmbeddedId` field — JPA will fail to load entities
2. **NEVER change** `@Column(name=...)` mappings on existing fields — breaks all queries and schema validation

### Safe Operations

3. **Adding** new columns with defaults or `nullable = true` is safe — existing rows get null
4. **Adding** new `@Embedded` value objects is safe if column names don't conflict

### Why

Hibernate's `ddl-auto: update` (dev) or `validate` (prod) will fail or silently drop data if column mappings change. Production deployments use schema validation, so any mismatch prevents startup.

---

## 4. Module Boundary Rules

### MUST NOT

1. **NEVER add a compile dependency** from one bounded context to another — use Kafka events instead
2. **NEVER import** `com.library.catalog.domain.*` from `library-patron` or vice versa
3. New cross-context communication **MUST** go through Kafka events, NOT direct API calls or shared database tables

### Allowed Cross-Module Dependencies

| Module | May Depend On |
|--------|--------------|
| Any bounded context | `library-shared` only |
| `library-e2e-test` | All bounded contexts (test scope) |
| `library-integration-test` | All bounded contexts (test scope) |
| `library-staging-test` | All bounded contexts (test scope) |

### Why

DDD bounded contexts must be independently deployable and testable. Direct compile dependencies create tight coupling that defeats the purpose of the microservices-oriented architecture.

---

## Quick Reference: What Breaks What

| Change | Breaks | Recovery |
|--------|--------|----------|
| Remove class from library-shared | All 7 contexts fail to compile | Add back immediately |
| Rename eventType string | Consumer silently ignores event | Fix consumer switch + redeploy all |
| Change JSON field type | Jackson deserialization error | Revert + deploy |
| Remove DB column | JPA schema validation fails | Add column back |
| Add cross-context import | Maven build fails (enforced by architecture) | Remove import, use Kafka event |
