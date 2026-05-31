package com.library.patron.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.FineIncurredEventHandler;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.MembershipStatus;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationFineEventSteps {

    @Autowired private FineIncurredEventHandler handler;
    @Autowired private PatronRepository patronRepository;
    @Autowired private ObjectMapper objectMapper;

    private Patron testPatron;

    @Given("a patron exists with a current fine balance of {int}")
    public void createPatronWithFines(int fines) {
        testPatron = Patron.create("Fine", "Test", "fine_event@test.com", PatronType.STUDENT);
        if (fines > 0) {
            testPatron.addFine(BigDecimal.valueOf(fines));
        }
        testPatron = patronRepository.save(testPatron);
    }

    @When("the module receives a fine event for that patron with amount {int}")
    public void receiveFineEvent(int amount) throws Exception {
        String json = """
            {"eventType":"FineIncurredEvent","eventId":"evt-fine-1",
             "patronId":{"value":"%s"},"fineId":{"value":"fine-1"},
             "loanId":{"value":"loan-1"},"amount":"%d","overdueDays":5}
            """.formatted(testPatron.getId().getValue(), amount);
        handler.handle(objectMapper.readTree(json));
    }

    @Then("the patron's fine balance should become {int}")
    public void verifyFineBalance(int expected) {
        Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
        assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Then("the patron status should become SUSPENDED")
    public void verifySuspended() {
        Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
    }
}
