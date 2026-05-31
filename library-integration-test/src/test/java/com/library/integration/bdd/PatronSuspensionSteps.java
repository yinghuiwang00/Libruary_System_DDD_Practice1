package com.library.integration.bdd;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class PatronSuspensionSteps {

    private static final String PATRON_TOPIC = "library.patron.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private E2EScenarioState state;

    @When("a PatronSuspendedEvent is published for that patron with reason {string}")
    public void publishPatronSuspendedEvent(String reason) {
        String eventJson = buildEventJson("PatronSuspendedEvent",
            "patronId", idObject(state.getPatronId()),
            "reason", jsonString(reason)
        );
        config.getKafkaTemplate().send(PATRON_TOPIC, eventJson);
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
