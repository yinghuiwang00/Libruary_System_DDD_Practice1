package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.HoldNotificationHandler;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class HoldNotificationEventSteps {

    @Autowired
    private HoldNotificationHandler holdHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @When("the notification module receives a hold placed event for patron {string} with queue position {int}")
    public void receiveHoldPlacedEvent(String patronId, int queuePosition) throws Exception {
        String json = """
            {"eventType":"HoldPlacedEvent","eventId":"evt-hold-placed-test",
             "patronId":{"value":"%s"},"bookId":{"value":"book-1"},
             "holdId":{"value":"hold-1"},"queuePosition":%d}
            """.formatted(patronId, queuePosition);
        holdHandler.handleHoldPlaced(objectMapper.readTree(json));
    }

    @When("the notification module receives a hold fulfilled event for patron {string}")
    public void receiveHoldFulfilledEvent(String patronId) throws Exception {
        String json = """
            {"eventType":"HoldFulfilledEvent","eventId":"evt-hold-fulfilled-test",
             "patronId":{"value":"%s"},"bookId":{"value":"book-1"},
             "holdId":{"value":"hold-1"}}
            """.formatted(patronId);
        holdHandler.handleHoldFulfilled(objectMapper.readTree(json));
    }
}
