package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.PatronStatusNotificationHandler;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class PatronStatusNotificationEventSteps {

    @Autowired
    private PatronStatusNotificationHandler patronStatusHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @When("the notification module receives a patron suspended event for patron {string} with reason {string}")
    public void receivePatronSuspendedEvent(String patronId, String reason) throws Exception {
        String json = """
            {"eventType":"PatronSuspendedEvent","eventId":"evt-suspend-test",
             "patronId":{"value":"%s"},"reason":"%s"}
            """.formatted(patronId, reason);
        patronStatusHandler.handlePatronSuspended(objectMapper.readTree(json));
    }
}
