package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.LowStockNotificationHandler;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class LowStockNotificationEventSteps {

    @Autowired
    private LowStockNotificationHandler lowStockHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @When("the notification module receives a low stock alert event for book {string} with {int} available copies")
    public void receiveLowStockAlertEvent(String bookId, int availableCopies) throws Exception {
        String json = """
            {"eventType":"LowStockAlertEvent","eventId":"evt-lowstock-test",
             "bookId":"%s","availableCopies":%d,"libraryId":{"value":"lib-1"}}
            """.formatted(bookId, availableCopies);
        lowStockHandler.handle(objectMapper.readTree(json));
    }
}
