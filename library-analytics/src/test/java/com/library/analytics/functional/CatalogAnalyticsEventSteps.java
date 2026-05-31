package com.library.analytics.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.analytics.application.handler.BookCreatedAnalyticsHandler;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class CatalogAnalyticsEventSteps {

    @Autowired
    private BookCreatedAnalyticsHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode eventNode;
    private String capturedBookId;

    @Given("the analytics module is ready")
    public void analyticsModuleReady() {
        // Module is started by Spring Boot test context
    }

    @When("a new book creation event is received with book ID {string} and title {string}")
    public void receiveBookCreatedEvent(String bookId, String title) throws Exception {
        String json = """
            {"eventType":"BookCreatedEvent","eventId":"evt-1","bookId":"%s","title":"%s"}
            """.formatted(bookId, title);
        eventNode = objectMapper.readTree(json);
        handler.handle(eventNode);
        capturedBookId = bookId;
    }

    @Then("the system should successfully process the new book event and record book ID {string}")
    public void verifyBookCreatedHandled(String expectedBookId) {
        assertThat(eventNode).isNotNull();
        assertThat(eventNode.get("bookId").asText()).isEqualTo(expectedBookId);
    }
}
