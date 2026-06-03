# CI Pipeline Details

> **Source**: Migrated from CLAUDE.md. Full CI configuration reference.

---

## 1. Pipeline Structure

```
push / PR → main / develop
  │
  ├── build (H2 + EmbeddedKafka)         ~4 min
  │   ├── Build all modules (skip tests)
  │   ├── Test library-shared
  │   ├── Test library-catalog
  │   ├── Test library-inventory
  │   ├── Test library-circulation
  │   ├── Test library-patron
  │   ├── Test library-payment
  │   ├── Test library-analytics
  │   ├── Test library-notification
  │   ├── Test library-e2e-test
  │   ├── Test library-integration-test
  │   └── Upload test-reports artifact
  │
  └── staging (PostgreSQL + Kafka)       ~2 min
      ├── needs: build ✅
      ├── Build all modules (skip tests)
      ├── Create database (PostgreSQL service container)
      ├── Test library-staging-test (9 scenarios)
      └── Upload staging-test-reports artifact
```

Both jobs must pass for CI to succeed.

---

## 2. CI Configuration

| Item | Value |
|------|-------|
| Workflow file | `.github/workflows/ci.yml` |
| Runner | `ubuntu-latest` |
| JDK | 17 (temurin) |
| Build cache | Maven |
| Skip CI | Add `[skip ci]` to commit message |
| Skip build only | Add `[skip build]` to commit message |

### GitHub Actions Versions

| Action | Version |
|--------|---------|
| `actions/checkout` | v6 |
| `actions/setup-java` | v5 |
| `actions/upload-artifact` | v7 (Node.js 24) |

---

## 3. Staging Job Service Containers

### PostgreSQL

```yaml
postgres:
  image: postgres:16
  env:
    POSTGRES_PASSWORD: postgres
  ports: ["5432:5432"]
  options: >-
    --health-cmd pg_isready
    --health-interval 10s
    --health-timeout 5s
    --health-retries 5
```

### Zookeeper

```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  env:
    ZOOKEEPER_CLIENT_PORT: 2181
  ports: ["2181:2181"]
```

### Kafka

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  env:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
  ports: ["29092:29092"]
```

**⚠️ Critical**: Kafka container MUST listen on port 29092 internally (`KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092`) to match `KAFKA_ADVERTISED_LISTENERS`. Otherwise the Kafka controller cannot connect to itself inside the container.

---

## 4. Staging Test Environment Lifecycle

The `library-staging-test` module manages its own environment lifecycle:

| Phase | PostgreSQL | Kafka |
|-------|-----------|-------|
| `@BeforeAll` | Hibernate `create-drop` creates tables | Delete old topics → rebuild with 3 partitions → delete consumer groups |
| `@Before` (each scenario) | DELETE all table data | — |
| `@After` (each scenario) | Print snapshot (row counts) | Print snapshot (offsets) |
| `@AfterAll` | Hibernate `create-drop` drops tables | Delete topics + delete consumer groups |

---

## 5. Environment Variables

Staging test `application.yml` accepts these environment variables:

| Variable | Default | CI Override | Purpose |
|----------|---------|-------------|---------|
| `DB_USERNAME` | `postgres` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `dev_pg_2026` | `postgres` | PostgreSQL password |
| `SPRING.KAFKA.BOOTSTRAP-SERVERS` | `localhost:29092` | `localhost:29092` | Kafka broker address |

In CI, `DB_PASSWORD` is overridden to `postgres` via the workflow YAML to match the service container password.

---

## 6. Staging Profile

The staging test module is activated via Maven profile:

```bash
# Run staging tests only
mvn test -Pstaging -pl library-staging-test

# Full build including staging
mvn clean install -Pstaging
```

Parent `pom.xml` has two profiles:
- **default**: All modules except `library-staging-test`
- **staging**: All modules including `library-staging-test`
