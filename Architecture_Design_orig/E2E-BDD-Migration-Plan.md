# Plan: E2E BDD Cross-Context Integration Tests

## Context

The `library-e2e-test` module has 9 JUnit 5 test methods across 7 test classes that verify cross-context Kafka event flows using `@SpringBootTest` + `@EmbeddedKafka` + Awaitility. This plan creates a new `library-integration-test` module using **Cucumber BDD** to provide business-readable Given/When/Then coverage for the same 8 cross-context use cases.

Two complementary test layers:
- `library-e2e-test` — JUnit 5 direct tests (existing, unchanged)
- `library-integration-test` — Cucumber BDD scenarios with business-readable Given/When/Then (new)

---

## Phase B: Create BDD Integration Test Module

### Task Overview

| # | Task | Depends On | Estimated Files |
|---|------|-----------|----------------|
| B1 | Create module skeleton (pom.xml + root pom update) | — | 2 |
| B2 | Copy infrastructure files from library-e2e-test | B1 | 4 |
| B3 | Create Cucumber test infrastructure | B2 | 3 |
| B4 | UC-1: Borrow Book feature + steps | B3 | 2 |
| B5 | UC-2: Return Book feature + steps | B3 | 2 |
| B6 | UC-3: Hold Book feature + steps | B3 | 2 |
| B7 | UC-4+5: Fine/Payment feature + steps | B3 | 2 |
| B8 | UC-6: New Book Cataloging feature + steps | B3 | 2 |
| B9 | UC-7: Patron Suspension feature + steps | B3 | 2 |
| B10 | UC-8: Low Stock Alert feature + steps | B3 | 2 |
| B11 | Verify full build + all tests pass | B4–B10 | 0 |

---

### B1: Create Module Skeleton

**Create `library-integration-test/pom.xml`**

Based on `library-e2e-test/pom.xml`, add Cucumber dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.library</groupId>
        <artifactId>library-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>library-integration-test</artifactId>
    <name>Library Integration Test (BDD)</name>
    <description>Cucumber BDD end-to-end cross-context integration tests via Kafka</description>

    <dependencies>
        <!-- All bounded context modules -->
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-shared</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-catalog</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-inventory</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-circulation</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-patron</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-payment</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-analytics</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>library-notification</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Kafka Test -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Cucumber BDD -->
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
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Test Framework -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.awaitility</groupId>
            <artifactId>awaitility</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <failIfNoTests>false</failIfNoTests>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Update root `pom.xml` `<modules>` section** — add `library-integration-test` after `library-e2e-test`.

---

### B2: Copy Infrastructure Files

Copy these 4 files **unchanged** from `library-e2e-test/src/test/` to `library-integration-test/src/test/`:

| Source | Target |
|--------|--------|
| `java/com/library/integration/IntegrationTestApplication.java` | same path |
| `java/com/library/integration/config/IntegrationTestBeansConfig.java` | same path |
| `java/com/library/integration/config/EmbeddedKafkaConfig.java` | same path |
| `resources/application.yml` | same path |

---

### B3: Create Cucumber Test Infrastructure

#### File: `src/test/java/com/library/integration/bdd/CucumberTestSuite.java`

```java
package com.library.integration.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = "cucumber.glue", value = "com.library.integration.bdd")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class CucumberTestSuite {
}
```

Pattern source: `library-patron/.../CucumberTestSuite.java`

#### File: `src/test/java/com/library/integration/bdd/CucumberSpringConfig.java`

```java
package com.library.integration.bdd;

import com.library.integration.IntegrationTestApplication;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Cucumber-Spring glue for BDD integration tests.
 * Combines CucumberContextConfiguration with the setup logic from BaseEndToEndTest.
 */
@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext
@CucumberContextConfiguration
public class CucumberSpringConfig {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KafkaTemplate<String, String> kafkaTemplate;

    @Before
    public void setUp() {
        // Wait for all Kafka listener containers to be assigned
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        }

        // Create producer
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);

        // Clean all DB tables (reverse dependency order for FK constraints)
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("DELETE FROM notifications");
            jdbcTemplate.execute("DELETE FROM analytics_reports");
            jdbcTemplate.execute("DELETE FROM payments");
            jdbcTemplate.execute("DELETE FROM book_copies");
            jdbcTemplate.execute("DELETE FROM copy_inventories");
            jdbcTemplate.execute("DELETE FROM book_authors");
            jdbcTemplate.execute("DELETE FROM book_categories");
            jdbcTemplate.execute("DELETE FROM books");
            jdbcTemplate.execute("DELETE FROM patrons");
            jdbcTemplate.execute("DELETE FROM libraries");
        });
    }

    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }
}
```

Pattern source: `library-patron/.../CucumberSpringConfig.java` + `library-e2e-test/.../BaseEndToEndTest.java`

#### File: `src/test/java/com/library/integration/bdd/E2EScenarioState.java`

Shared `@Component` that holds scenario state across step definitions:

```java
package com.library.integration.bdd;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Holds mutable scenario state shared across step definition classes.
 * Reset automatically via DB cleanup in CucumberSpringConfig @Before.
 */
@Component
public class E2EScenarioState {

    private String patronId;
    private String patronEmail;
    private String bookId;
    private int initialLoanCount;
    private BigDecimal initialFines;

    // Getters and Setters
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }

    public String getPatronEmail() { return patronEmail; }
    public void setPatronEmail(String patronEmail) { this.patronEmail = patronEmail; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public int getInitialLoanCount() { return initialLoanCount; }
    public void setInitialLoanCount(int initialLoanCount) { this.initialLoanCount = initialLoanCount; }

    public BigDecimal getInitialFines() { return initialFines; }
    public void setInitialFines(BigDecimal initialFines) { this.initialFines = initialFines; }
}
```

---

### B4: UC-1 — Borrow Book

**Source test**: `BorrowBookEndToEndTest.java` — 1 scenario
**Flow**: Circulation `BookBorrowedEvent` → Patron updates loan count + Notification created

#### Feature File: `src/test/resources/features/integration/borrow-book.feature`

```gherkin
Feature: Borrow Book Cross-Context Integration
  As the library system
  When a book is borrowed (Circulation publishes BookBorrowedEvent)
  I want the Patron context to update loan count
  And the Notification context to create a notification

  Scenario: Borrow book updates patron loan count and creates notification
    Given a patron "John Doe" with email "john.borrow@test.com" exists
    When a BookBorrowedEvent is published for that patron with loan "loan-001", copy "copy-001", book "book-001"
    Then the patron's current loan count should be 1
    And the patron's total borrowed count should be 1
    And a notification should exist for the patron
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/BorrowBookSteps.java`

Key implementation details:
- `@Given` creates patron via `Patron.create()` + `patronRepository.save()`, stores `patronId` in `E2EScenarioState`
- `@When` builds `BookBorrowedEvent` JSON and sends to `library.circulation.events` via `CucumberSpringConfig.getKafkaTemplate()`
- `@Then` uses `await().atMost(10, SECONDS).untilAsserted(...)` for async assertions

```java
package com.library.integration.bdd;

import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BorrowBookSteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private E2EScenarioState state;

    @Given("a patron {string} with email {string} exists")
    public void createPatron(String name, String email) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
        state.setPatronEmail(email);
    }

    @When("a BookBorrowedEvent is published for that patron with loan {string}, copy {string}, book {string}")
    public void publishBorrowEvent(String loanId, String copyId, String bookId) {
        String eventJson = buildEventJson("BookBorrowedEvent",
            "loanId", idObject(loanId),
            "copyId", idObject(copyId),
            "patronId", idObject(state.getPatronId()),
            "bookId", idObject(bookId),
            "loanDate", jsonString("2026-05-31T10:00:00"),
            "dueDate", jsonString("2026-06-30T10:00:00")
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
    }

    @Then("the patron's current loan count should be {int}")
    public void verifyLoanCount(int expected) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(
                new com.library.shared.domain.model.PatronId(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(expected);
        });
    }

    @Then("the patron's total borrowed count should be {int}")
    public void verifyTotalBorrowed(int expected) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(
                new com.library.shared.domain.model.PatronId(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getTotalBorrowed()).isEqualTo(expected);
        });
    }

    @Then("a notification should exist for the patron")
    public void verifyNotification() {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(state.getPatronId());
            assertThat(notifications).isNotEmpty();
        });
    }

    // --- JSON helper methods (same as BaseEndToEndTest) ---
    private String buildEventJson(String eventType, String... keyValuePairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"eventType\":\"").append(eventType).append("\"");
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            sb.append(",\"").append(keyValuePairs[i]).append("\":").append(keyValuePairs[i + 1]);
        }
        sb.append("}");
        return sb.toString();
    }

    private String idObject(String idValue) {
        return "{\"value\":\"" + idValue + "\"}";
    }

    private String jsonString(String value) {
        return "\"" + value + "\"";
    }
}
```

---

### B5: UC-2 — Return Book

**Source test**: `ReturnBookEndToEndTest.java` — 1 scenario
**Flow**: Circulation `BookReturnedEvent` → Patron decrements loan count + Notification created

#### Feature File: `src/test/resources/features/integration/return-book.feature`

```gherkin
Feature: Return Book Cross-Context Integration
  As the library system
  When a book is returned (Circulation publishes BookReturnedEvent)
  I want the Patron context to decrement loan count
  And the Notification context to create a notification

  Scenario: Return book decrements patron loan count and creates notification
    Given a patron "Jane Smith" with email "jane.return@test.com" exists with 1 active loan
    When a BookReturnedEvent is published for that patron with loan "loan-001", copy "copy-001", book "book-001"
    Then the patron's current loan count should be 0
    And a notification should exist for the patron
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/ReturnBookSteps.java`

Key differences from BorrowBookSteps:
- `@Given` creates patron then calls `patron.recordLoan()` before save (initialLoanCount = 1)
- `@When` builds `BookReturnedEvent` JSON (includes `returnDate`, `fineAmount: 0`)
- `@Then` reuses `verifyLoanCount(0)` and `verifyNotification()` from BorrowBookSteps (or uses `SharedSteps`)

**Note on step deduplication**: The `verifyLoanCount` and `verifyNotification` Then steps are identical to BorrowBookSteps. To avoid Cucumber "ambiguous step" errors, either:
- Option A: Extract common steps into `SharedSteps.java` (recommended)
- Option B: Use unique step text per feature (e.g., "the patron's loan count after return should be 0")

**Recommended**: Extract shared Then steps into `SharedSteps.java`:

#### File: `src/test/java/com/library/integration/bdd/SharedSteps.java`

```java
package com.library.integration.bdd;

import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Shared step definitions used across multiple feature files.
 * Extracted to avoid Cucumber "ambiguous step definition" errors.
 */
public class SharedSteps {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private E2EScenarioState state;

    // --- Shared Given steps ---

    @Given("a patron {string} with email {string} exists")
    public void createPatron(String name, String email) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @Given("a patron {string} with email {string} exists with {int} active loan(s)")
    public void createPatronWithLoans(String name, String email, int loans) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        for (int i = 0; i < loans; i++) {
            patron.recordLoan();
        }
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @Given("the main library with code {string} exists")
    public void createLibrary(String code) {
        Library library = Library.create(code, "Test Library " + code);
        libraryRepository.save(library);
    }

    // --- Shared Then steps ---

    @Then("the patron's current loan count should be {int}")
    public void verifyPatronLoanCount(int expected) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(
                new PatronId(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(expected);
        });
    }

    @Then("a notification should exist for the patron")
    public void verifyNotificationForPatron() {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(state.getPatronId());
            assertThat(notifications).isNotEmpty();
        });
    }
}
```

With `SharedSteps.java` in place, individual step files only contain **Given/When steps unique to their use case** and domain-specific **Then** steps.

---

### B6: UC-3 — Hold Book

**Source test**: `HoldBookEndToEndTest.java` — 2 scenarios
**Flow**: Circulation `HoldPlacedEvent` / `HoldFulfilledEvent` → Notification created

#### Feature File: `src/test/resources/features/integration/hold-book.feature`

```gherkin
Feature: Hold Book Cross-Context Integration
  As the library system
  When a hold is placed or fulfilled (Circulation publishes hold events)
  I want the Notification context to create appropriate notifications

  Scenario: Hold placed creates notification for patron
    Given a patron "Bob Johnson" with email "bob.hold@test.com" exists as faculty
    When a HoldPlacedEvent is published for that patron with hold "hold-001", book "book-001", queue position 1
    Then a notification should exist for the patron

  Scenario: Hold fulfilled creates notification for patron
    Given a patron "Bob Johnson" with email "bob.hold@test.com" exists as faculty
    When a HoldFulfilledEvent is published for that patron with hold "hold-001", book "book-001", copy "copy-001"
    Then a notification should exist for the patron
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/HoldBookSteps.java`

Unique steps:
- `@Given("... exists as faculty")` — create with `PatronType.FACULTY`
- `@When("a HoldPlacedEvent is published ...")` — build HoldPlacedEvent JSON → CIRCULATION_TOPIC
- `@When("a HoldFulfilledEvent is published ...")` — build HoldFulfilledEvent JSON → CIRCULATION_TOPIC

---

### B7: UC-4+5 — Fine Incurred + Payment Completed

**Source test**: `FinePaymentEndToEndTest.java` — 2 scenarios
**Flow A**: Circulation `FineIncurredEvent` → Patron adds fine balance + Payment record + Notification
**Flow B**: Payment `PaymentCompletedEvent` → Patron reduces fines + Notification

#### Feature File: `src/test/resources/features/integration/fine-payment.feature`

```gherkin
Feature: Fine and Payment Cross-Context Integration
  As the library system
  When a fine is incurred or a payment is completed
  I want the Patron context to update fine balance accordingly
  And the Notification context to create appropriate notifications

  Scenario: Fine incurred updates patron fine balance and creates notification
    Given a patron "Alice Williams" with email "alice.fine@test.com" exists
    When a FineIncurredEvent is published for that patron with fine "fine-001", loan "loan-001", amount 15.00
    Then the patron's outstanding fines should be 15.00
    And a notification should exist for the patron

  Scenario: Payment completed reduces patron fine balance and creates notification
    Given a patron "Alice Williams" with email "alice.fine@test.com" exists with outstanding fines of 25.00
    When a PaymentCompletedEvent is published for that patron with payment "pay-001", amount 25.00, reference "REF-001"
    Then the patron's outstanding fines should be 0.00
    And a notification should exist for the patron
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/FinePaymentSteps.java`

Unique steps:
- `@Given("... exists with outstanding fines of {double}")` — create patron, call `patron.addFine(BigDecimal)`, save
- `@When("a FineIncurredEvent is published ...")` — build FineIncurredEvent JSON → CIRCULATION_TOPIC
- `@When("a PaymentCompletedEvent is published ...")` — build PaymentCompletedEvent JSON → PAYMENT_TOPIC
- `@Then("the patron's outstanding fines should be {double}")` — verify via `assertThat(updated.getOutstandingFines()).isEqualByComparingTo(...)`

---

### B8: UC-6 — New Book Cataloging

**Source test**: `NewBookEndToEndTest.java` — 1 scenario
**Flow**: Catalog `BookCreatedEvent` → Inventory creates CopyInventory record

#### Feature File: `src/test/resources/features/integration/new-book.feature`

```gherkin
Feature: New Book Cataloging Cross-Context Integration
  As the library system
  When a new book is cataloged (Catalog publishes BookCreatedEvent)
  I want the Inventory context to create an inventory record

  Scenario: New book creates inventory record
    Given the main library with code "MAIN-LIB-001" exists
    When a BookCreatedEvent is published with book "book-new-001", ISBN "9780134685991", title "Effective Java"
    Then an inventory record should exist for book "book-new-001" with total copies 0
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/NewBookSteps.java`

Unique steps:
- `@Given("the main library with code {string} exists")` — `Library.create()` + `libraryRepository.save()`
- `@When("a BookCreatedEvent is published ...")` — build BookCreatedEvent JSON → CATALOG_TOPIC
- `@Then("an inventory record should exist for book {string} with total copies {int}")` — verify via `copyInventoryRepository.findByBookId(bookId)`

---

### B9: UC-7 — Patron Suspension

**Source test**: `PatronSuspensionEndToEndTest.java` — 1 scenario
**Flow**: Patron `PatronSuspendedEvent` → Notification created

#### Feature File: `src/test/resources/features/integration/patron-suspension.feature`

```gherkin
Feature: Patron Suspension Cross-Context Integration
  As the library system
  When a patron is suspended (Patron publishes PatronSuspendedEvent)
  I want the Notification context to create a status change notification

  Scenario: Patron suspension creates notification
    Given a patron "Tom Brown" with email "tom.suspend@test.com" exists
    When a PatronSuspendedEvent is published for that patron with reason "Excessive overdue books"
    Then a notification should exist for the patron
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/PatronSuspensionSteps.java`

Unique steps:
- `@When("a PatronSuspendedEvent is published for that patron with reason {string}")` — build PatronSuspendedEvent JSON → PATRON_TOPIC

---

### B10: UC-8 — Low Stock Alert

**Source test**: `LowStockAlertEndToEndTest.java` — 1 scenario
**Flow**: Inventory `LowStockAlertEvent` → Notification for LIBRARIAN

#### Feature File: `src/test/resources/features/integration/low-stock-alert.feature`

```gherkin
Feature: Low Stock Alert Cross-Context Integration
  As the library system
  When stock is low (Inventory publishes LowStockAlertEvent)
  I want the Notification context to create an alert for librarians

  Scenario: Low stock alert creates notification for librarian
    When a LowStockAlertEvent is published with inventory "inv-001", book "book-lowstock-001", available copies 1, threshold 2
    Then a notification should exist for recipient "LIBRARIAN"
```

#### Step Definitions: `src/test/java/com/library/integration/bdd/LowStockAlertSteps.java`

Unique steps:
- `@When("a LowStockAlertEvent is published ...")` — build LowStockAlertEvent JSON → INVENTORY_TOPIC
- `@Then("a notification should exist for recipient {string}")` — verify via `notificationRepository.findByRecipientId("LIBRARIAN")`

---

### B11: Verification

```bash
# 1. Full project build (skip tests first)
mvn clean install -DskipTests

# 2. Verify existing E2E tests still pass
cd library-e2e-test && mvn test

# 3. Run new BDD tests
cd library-integration-test && mvn test

# 4. Full project test suite
mvn clean test
```

Expected: 9 JUnit 5 E2E tests + ~10 Cucumber BDD scenarios all pass.

---

## Final File Structure

```
library-integration-test/
  pom.xml
  src/test/
    java/com/library/integration/
      IntegrationTestApplication.java          ← copied from library-e2e-test
      config/
        IntegrationTestBeansConfig.java        ← copied from library-e2e-test
        EmbeddedKafkaConfig.java               ← copied from library-e2e-test
      bdd/
        CucumberTestSuite.java                 ← @Suite + @IncludeEngines("cucumber")
        CucumberSpringConfig.java              ← @SpringBootTest + @CucumberContextConfiguration + @Before cleanup
        E2EScenarioState.java                  ← @Component holding scenario state
        SharedSteps.java                       ← common Given/Then steps + JSON helpers
        BorrowBookSteps.java                   ← UC-1 unique When steps
        ReturnBookSteps.java                   ← UC-2 unique Given/When steps
        HoldBookSteps.java                     ← UC-3 unique Given/When steps
        FinePaymentSteps.java                  ← UC-4+5 unique Given/When/Then steps
        NewBookSteps.java                      ← UC-6 unique Given/When/Then steps
        PatronSuspensionSteps.java             ← UC-7 unique When steps
        LowStockAlertSteps.java                ← UC-8 unique When/Then steps
    resources/
      features/integration/
        borrow-book.feature                    ← UC-1: 1 scenario
        return-book.feature                    ← UC-2: 1 scenario
        hold-book.feature                      ← UC-3: 2 scenarios
        fine-payment.feature                   ← UC-4+5: 2 scenarios
        new-book.feature                       ← UC-6: 1 scenario
        patron-suspension.feature              ← UC-7: 1 scenario
        low-stock-alert.feature                ← UC-8: 1 scenario
      application.yml                          ← copied from library-e2e-test
```

### Total Files Created: 18
- 1 pom.xml
- 4 copied infrastructure files
- 3 Cucumber infrastructure (suite, config, state)
- 8 step definition classes (1 shared + 7 use-case)
- 7 feature files
- 1 application.yml

### Scenario Count: ~10

| Feature | Scenarios |
|---------|-----------|
| borrow-book | 1 |
| return-book | 1 |
| hold-book | 2 |
| fine-payment | 2 |
| new-book | 1 |
| patron-suspension | 1 |
| low-stock-alert | 1 |
| **Total** | **9** |

---

## Implementation Order

Tasks B4–B10 can be done in any order after B3 is complete. Recommended sequence for fastest feedback:

1. **B1 + B2** — Module skeleton + infrastructure (build must succeed)
2. **B3** — Cucumber infrastructure (verify CucumberSpringConfig loads)
3. **B4** — UC-1 Borrow Book (simplest full-cycle test, validates infrastructure)
4. **B11** — Quick verification that UC-1 works
5. **B5–B10** — Remaining UCs in any order
6. **B11** — Final full verification
