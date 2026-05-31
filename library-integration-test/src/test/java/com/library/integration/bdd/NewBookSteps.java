package com.library.integration.bdd;

import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

public class NewBookSteps {

    private static final String CATALOG_TOPIC = "library.catalog.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private E2EScenarioState state;

    @Autowired
    private CopyInventoryRepository copyInventoryRepository;

    @When("a BookCreatedEvent is published with book {string}, ISBN {string}, title {string}")
    public void publishBookCreatedEvent(String bookId, String isbn, String title) {
        state.setBookId(bookId);
        String eventJson = buildEventJson("BookCreatedEvent",
            "bookId", jsonString(bookId),
            "isbn", jsonString(isbn),
            "title", jsonString(title)
        );
        config.getKafkaTemplate().send(CATALOG_TOPIC, eventJson);
    }

    @Then("an inventory record should exist for book {string} with total copies {int}")
    public void verifyInventoryRecord(String bookId, int expectedCopies) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<CopyInventory> inventories = copyInventoryRepository.findByBookId(bookId);
            assertThat(inventories).isNotEmpty();
            assertThat(inventories.get(0).getTotalCopies()).isEqualTo(expectedCopies);
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

    private String jsonString(String value) {
        return "\"" + value + "\"";
    }
}
