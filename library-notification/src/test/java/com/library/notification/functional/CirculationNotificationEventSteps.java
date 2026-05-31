package com.library.notification.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.notification.application.handler.BorrowedNotificationHandler;
import com.library.notification.application.handler.ReturnedNotificationHandler;
import com.library.notification.application.handler.OverdueNotificationHandler;
import com.library.notification.application.handler.FineNotificationHandler;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class CirculationNotificationEventSteps {

    @Autowired
    private BorrowedNotificationHandler borrowedHandler;

    @Autowired
    private ReturnedNotificationHandler returnedHandler;

    @Autowired
    private OverdueNotificationHandler overdueHandler;

    @Autowired
    private FineNotificationHandler fineHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @When("the notification module receives a borrow event for patron {string}")
    public void receiveBorrowEvent(String patronId) throws Exception {
        String json = """
            {"eventType":"BookBorrowedEvent","eventId":"evt-borrow-test",
             "patronId":{"value":"%s"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """.formatted(patronId);
        borrowedHandler.handle(objectMapper.readTree(json));
    }

    @When("the notification module receives a return event for patron {string}")
    public void receiveReturnEvent(String patronId) throws Exception {
        String json = """
            {"eventType":"BookReturnedEvent","eventId":"evt-return-test",
             "patronId":{"value":"%s"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """.formatted(patronId);
        returnedHandler.handle(objectMapper.readTree(json));
    }

    @When("the notification module receives an overdue event for patron {string} with {int} days overdue")
    public void receiveOverdueEvent(String patronId, int daysOverdue) throws Exception {
        String json = """
            {"eventType":"OverdueNoticeEvent","eventId":"evt-overdue-test",
             "patronId":{"value":"%s"},"daysOverdue":%d}
            """.formatted(patronId, daysOverdue);
        overdueHandler.handle(objectMapper.readTree(json));
    }

    @When("the notification module receives a fine event for patron {string} with fine amount {string}")
    public void receiveFineEvent(String patronId, String amount) throws Exception {
        String json = """
            {"eventType":"FineIncurredEvent","eventId":"evt-fine-test",
             "patronId":{"value":"%s"},"amount":"%s","fineId":{"value":"fine-1"}}
            """.formatted(patronId, amount);
        fineHandler.handle(objectMapper.readTree(json));
    }
}
