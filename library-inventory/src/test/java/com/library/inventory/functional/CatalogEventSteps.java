package com.library.inventory.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.handler.BookCreatedEventHandler;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogEventSteps {

    @Autowired
    private BookCreatedEventHandler handler;

    @Autowired
    private CopyInventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String currentBookId;

    @When("the module receives a new book created event with book ID {string}")
    public void receiveBookCreatedEvent(String bookId) throws Exception {
        currentBookId = bookId;
        String eventJson = """
            {
                "eventType": "BookCreatedEvent",
                "eventId": "evt-book-created-bdd-001",
                "bookId": "%s"
            }
            """.formatted(bookId);
        JsonNode event = objectMapper.readTree(eventJson);
        handler.handle(event);
    }

    @Then("the inventory record for that book should be created")
    public void verifyInventoryCreated() {
        var inventories = inventoryRepository.findByBookId(currentBookId);
        assertThat(inventories).hasSize(1);
        assertThat(inventories.get(0).getBookId()).isEqualTo(currentBookId);
        assertThat(inventories.get(0).getTotalCopies()).isEqualTo(0);
        assertThat(inventories.get(0).getAvailableCopies()).isEqualTo(0);
    }
}
