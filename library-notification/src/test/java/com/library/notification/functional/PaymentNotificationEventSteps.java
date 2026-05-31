package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.PaymentNotificationHandler;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class PaymentNotificationEventSteps {

    @Autowired
    private PaymentNotificationHandler paymentHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @When("the notification module receives a payment completed event for patron {string} with amount {string}")
    public void receivePaymentCompletedEvent(String patronId, String amount) throws Exception {
        String json = """
            {"eventType":"PaymentCompletedEvent","eventId":"evt-payment-test",
             "patronId":{"value":"%s"},"paymentId":{"value":"pay-1"},"amount":"%s"}
            """.formatted(patronId, amount);
        paymentHandler.handle(objectMapper.readTree(json));
    }
}
