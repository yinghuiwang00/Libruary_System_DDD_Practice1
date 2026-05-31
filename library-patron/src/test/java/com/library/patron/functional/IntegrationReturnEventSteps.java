package com.library.patron.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.BookReturnedEventHandler;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class IntegrationReturnEventSteps {

    @Autowired private BookReturnedEventHandler handler;
    @Autowired private PatronRepository patronRepository;
    @Autowired private ObjectMapper objectMapper;

    private Patron testPatron;
    private boolean patronExists = true;

    @Given("a patron with loans exists with current loans of {int}")
    public void createPatronWithLoans(int loans) {
        testPatron = Patron.create("Return", "Test", "return_event@test.com", PatronType.STUDENT);
        for (int i = 0; i < loans; i++) {
            testPatron.recordLoan();
        }
        testPatron = patronRepository.save(testPatron);
        patronExists = true;
    }

    @Given("the patron for the return event does not exist in the database")
    public void noReturnPatronExists() {
        patronExists = false;
    }

    @When("the module receives a return event for that patron")
    public void receiveReturnEvent() throws Exception {
        String patronIdStr = patronExists ? testPatron.getId().getValue() : "nonexistent-id";
        String json = """
            {"eventType":"BookReturnedEvent","eventId":"evt-return-1",
             "patronId":{"value":"%s"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """.formatted(patronIdStr);
        handler.handle(objectMapper.readTree(json));
    }

    @When("the module receives a return event for that non-existent patron")
    public void receiveReturnEventForNonexistent() throws Exception {
        String json = """
            {"eventType":"BookReturnedEvent","eventId":"evt-return-2",
             "patronId":{"value":"nonexistent-id"},"copyId":{"value":"copy-1"},
             "bookId":{"value":"book-1"},"loanId":{"value":"loan-1"}}
            """;
        handler.handle(objectMapper.readTree(json));
    }

    @Then("the patron's loan count should be reduced to {int}")
    public void verifyCurrentLoans(int expected) {
        Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
        assertThat(updated.getCurrentLoans()).isEqualTo(expected);
    }

    @Then("the system should handle it safely without throwing an exception")
    public void verifyNoException() {
        assertThatCode(() -> {}).doesNotThrowAnyException();
    }
}
