package com.library.integration.bdd;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class HoldBookSteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private E2EScenarioState state;

    @When("a HoldPlacedEvent is published for that patron with hold {string}, book {string}, queue position {int}")
    public void publishHoldPlacedEvent(String holdId, String bookId, int queuePosition) {
        String eventJson = buildEventJson("HoldPlacedEvent",
            "holdId", idObject(holdId),
            "bookId", idObject(bookId),
            "patronId", idObject(state.getPatronId()),
            "queuePosition", String.valueOf(queuePosition),
            "placedAt", jsonString("2026-05-31T10:00:00")
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
    }

    @When("a HoldFulfilledEvent is published for that patron with hold {string}, book {string}, copy {string}")
    public void publishHoldFulfilledEvent(String holdId, String bookId, String copyId) {
        String eventJson = buildEventJson("HoldFulfilledEvent",
            "holdId", idObject(holdId),
            "bookId", idObject(bookId),
            "patronId", idObject(state.getPatronId()),
            "copyId", idObject(copyId),
            "availableUntil", jsonString("2026-06-07T10:00:00"),
            "fulfilledAt", jsonString("2026-05-31T10:00:00")
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
