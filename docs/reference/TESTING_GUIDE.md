# Testing Guide

> **Source**: Migrated from CLAUDE.md + supplemented with project conventions.

---

## 1. Test Types

| Layer | Test Type | Framework | Scope |
|-------|-----------|-----------|-------|
| Domain | Unit Tests | JUnit 5 + Mockito + AssertJ | Aggregates, value objects, domain services |
| Application | Unit Tests | JUnit 5 + Mockito | Application services, event handlers |
| API | Integration Tests | MockMvc + H2 `@SpringBootTest` | REST controllers, request/response mapping |
| Cross-context | BDD E2E Tests | Cucumber 7.15.0 | Happy path flows via `.feature` + `*Steps.java` |
| Cross-context | Staging Tests | Cucumber 7.15.0 + real infra | Same BDD scenarios against PostgreSQL + Kafka |

---

## 2. Test Location Rules

| Test Type | Module | Database | Kafka | Package |
|-----------|--------|----------|-------|---------|
| Unit tests | Each bounded context module | H2 (in-memory) | EmbeddedKafka | `com.library.<context>.<layer>` |
| Integration tests | Each bounded context module | H2 (in-memory) | EmbeddedKafka | `com.library.<context>.<layer>` |
| E2E BDD (JUnit 5) | `library-e2e-test` | H2 (in-memory) | EmbeddedKafka | `com.library.e2e` |
| E2E BDD (Cucumber) | `library-integration-test` | H2 (in-memory) | EmbeddedKafka | `com.library.integration` |
| Staging BDD (Cucumber) | `library-staging-test` | PostgreSQL (real) | Kafka (real) | `com.library.staging` |

---

## 3. Cucumber BDD Conventions

### File Locations

- **Feature files**: `src/test/resources/features/integration/<feature-name>.feature`
- **Step definitions**: `src/test/java/com/library/<module>/bdd/<FeatureName>Steps.java`
- **Test suite**: `CucumberTestSuite.java` with `@Suite` + `@IncludeEngines("cucumber")`

### Dependencies (Cucumber 7.15.0)

```xml
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
```

### Surefire Configuration

Must include in `pom.xml` surefire plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/CucumberTestSuite.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## 4. When to Add Tests

| Scenario | library-e2e-test | library-integration-test | library-staging-test |
|----------|:---:|:---:|:---:|
| New single-context feature | ❌ | ❌ | ❌ |
| New cross-context event flow | ✅ | ✅ | ✅ |
| Modified existing cross-context flow | ✅ | ✅ | ✅ |
| New Kafka topic/consumer | ✅ | ✅ | ✅ |
| Bug fix within single context | ❌ | ❌ | ❌ |

**Rule of thumb**: If it touches Kafka events between bounded contexts → add to ALL THREE test modules.

---

## 5. Three-Module Cross-Context Test Pattern

When a new cross-context event flow is added, write the same BDD scenario across 3 modules:

### Step 1: Write E2E test (library-e2e-test)

JUnit 5 test class that:
1. Uses `@SpringBootTest` with embedded Kafka
2. Sends an event to the publisher's topic
3. Uses Awaitility to wait for the consumer to process
4. Asserts the final state in the consumer's database

### Step 2: Write BDD test (library-integration-test)

Cucumber `.feature` + `*Steps.java` that:
1. Uses H2 + EmbeddedKafka
2. Follows Given/When/Then pattern
3. Same scenario as E2E but in BDD format

### Step 3: Write staging test (library-staging-test)

Same `.feature` + adapted `*Steps.java` that:
1. Uses real PostgreSQL + Kafka (port 29092)
2. Manages test data lifecycle (`@Before`/`@After`)
3. Same scenario, real infrastructure

---

## 6. Coverage Target

- **Minimum**: 80%+ per module
- **Verify**: `mvn test` reports coverage via surefire
- **Focus priority**: Domain logic > Application services > Controllers
- **Coverage tool**: JaCoCo (configured per module)
