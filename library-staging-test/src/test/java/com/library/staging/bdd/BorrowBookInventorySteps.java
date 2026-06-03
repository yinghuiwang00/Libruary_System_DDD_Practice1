package com.library.staging.bdd;

import com.library.inventory.domain.model.BookCopy;
import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Location;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.inventory.domain.repository.BookCopyRepository;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Staging step definitions for borrow-book-inventory feature.
 * Uses real PostgreSQL + Kafka infrastructure.
 */
public class BorrowBookInventorySteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";

    @Autowired
    private CopyInventoryRepository copyInventoryRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private StagingCucumberConfig config;

    @Autowired
    private StagingScenarioState state;

    private String copyId;

    @Given("a book {string} exists with inventory in library {string} and an available copy")
    public void createBookWithInventory(String bookId, String libraryCode) {
        CopyInventory inventory = CopyInventory.create(bookId, libraryCode, libraryCode, "SYSTEM");
        BookCopy copy = inventory.addCopy("BAR-STG-001", Location.simple(libraryCode, "A01-A-001"), "SYSTEM");
        copyInventoryRepository.save(inventory);
        this.copyId = copy.getId().getValue();
    }

    @When("a BookBorrowedEvent is published for the patron and that copy and book {string}")
    public void publishBorrowEvent(String bookId) {
        String eventJson = buildEventJson("BookBorrowedEvent",
            "loanId", idObject("loan-stg-001"),
            "copyId", idObject(copyId),
            "patronId", idObject(state.getPatronId()),
            "bookId", idObject(bookId),
            "loanDate", jsonString("2026-06-01T10:00:00"),
            "dueDate", jsonString("2026-07-01T10:00:00")
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
    }

    @Then("the copy status should be {string}")
    public void verifyCopyStatus(String expectedStatus) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            BookCopy copy = bookCopyRepository.findById(
                com.library.shared.domain.model.CopyId.of(copyId)
            ).orElseThrow(() -> new AssertionError("Copy not found: " + copyId));
            assertThat(copy.getStatus().name()).isEqualTo(expectedStatus);
        });
    }

    // --- JSON helpers ---
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
