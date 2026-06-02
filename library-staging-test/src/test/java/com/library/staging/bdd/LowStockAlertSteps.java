package com.library.staging.bdd;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class LowStockAlertSteps {

    private static final String INVENTORY_TOPIC = "library.inventory.events";

    @Autowired
    private StagingCucumberConfig config;

    @When("a LowStockAlertEvent is published with inventory {string}, book {string}, available copies {int}, threshold {int}")
    public void publishLowStockAlertEvent(String inventoryId, String bookId, int availableCopies, int threshold) {
        String eventJson = buildEventJson("LowStockAlertEvent",
            "inventoryId", jsonString(inventoryId),
            "bookId", jsonString(bookId),
            "availableCopies", String.valueOf(availableCopies),
            "threshold", String.valueOf(threshold),
            "alertedAt", jsonString("2026-05-31T10:00:00")
        );
        config.getKafkaTemplate().send(INVENTORY_TOPIC, eventJson);
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
