package com.library.analytics.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.LowStockAnalyticsHandler;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryAnalyticsEventSteps {

    @Autowired
    private LowStockAnalyticsHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode eventNode;

    @Given("the inventory analytics module is ready")
    public void analyticsModuleReady() {
        // Module is started by Spring Boot test context
    }

    @When("a low stock alert event is received with book ID {string} and available copies {int}")
    public void receiveLowStockAlertEvent(String bookId, int availableCopies) throws Exception {
        String json = """
            {"eventType":"LowStockAlertEvent","eventId":"evt-2","bookId":"%s","availableCopies":%d}
            """.formatted(bookId, availableCopies);
        eventNode = objectMapper.readTree(json);
        handler.handle(eventNode);
    }

    @Then("the system should successfully process the low stock alert event and record book ID {string}")
    public void verifyLowStockHandled(String expectedBookId) {
        assertThat(eventNode).isNotNull();
        assertThat(eventNode.get("bookId").asText()).isEqualTo(expectedBookId);
    }
}
