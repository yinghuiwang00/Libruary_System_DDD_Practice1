package com.library.integration.bdd;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class ReturnBookSteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private E2EScenarioState state;

    @When("a BookReturnedEvent is published for that patron with loan {string}, copy {string}, book {string}")
    public void publishReturnEvent(String loanId, String copyId, String bookId) {
        String eventJson = buildEventJson("BookReturnedEvent",
            "loanId", idObject(loanId),
            "copyId", idObject(copyId),
            "patronId", idObject(state.getPatronId()),
            "bookId", idObject(bookId),
            "returnDate", jsonString("2026-06-15T10:00:00"),
            "fineAmount", "0"
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
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
