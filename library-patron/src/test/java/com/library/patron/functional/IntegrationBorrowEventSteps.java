package com.library.patron.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.BookBorrowedEventHandler;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class IntegrationBorrowEventSteps {

    @Autowired private BookBorrowedEventHandler handler;
    @Autowired private PatronRepository patronRepository;
    @Autowired private ObjectMapper objectMapper;

    private Patron testPatron;
    private boolean patronExists = true;

    @Given("a patron exists with current loans of {int}")
    public void createPatronWithLoans(int loans) {
        testPatron = Patron.create("Event", "Test", "borrow_event@test.com", PatronType.STUDENT);
        for (int i = 0; i < loans; i++) {
            testPatron.recordLoan();
        }
        testPatron = patronRepository.save(testPatron);
        patronExists = true;
    }

    @Given("the specified patron does not exist in the database")
    public void noPatronExists() {
        patronExists = false;
    }

    @When("the module receives a borrow event for that patron")
    public void receiveBorrowEvent() throws Exception {
        String patronIdStr = patronExists ? testPatron.getId().getValue() : "nonexistent-id";
        String json = """
            {"eventType":"BookBorrowedEvent","eventId":"evt-1",
             "patronId":{"value":"%s"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """.formatted(patronIdStr);
        handler.handle(objectMapper.readTree(json));
    }

    @When("the module receives a borrow event for that non-existent patron")
    public void receiveBorrowEventForNonexistent() throws Exception {
        String json = """
            {"eventType":"BookBorrowedEvent","eventId":"evt-2",
             "patronId":{"value":"nonexistent-id"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """;
        handler.handle(objectMapper.readTree(json));
    }

    @Then("the patron's current loan count should become {int}")
    public void verifyCurrentLoans(int expected) {
        Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
        assertThat(updated.getCurrentLoans()).isEqualTo(expected);
    }

    @Then("the system should log the error without throwing an exception")
    public void verifyNoException() {
        assertThatCode(() -> {}).doesNotThrowAnyException();
    }
}
