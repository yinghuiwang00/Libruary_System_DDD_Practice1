package com.library.patron.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.handler.PaymentCompletedEventHandler;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationPaymentEventSteps {

    @Autowired private PaymentCompletedEventHandler handler;
    @Autowired private PatronRepository patronRepository;
    @Autowired private ObjectMapper objectMapper;

    private Patron testPatron;

    @Given("a patron with fines is prepared with a balance of {int}")
    public void createPatronWithFines(int fines) {
        testPatron = Patron.create("Pay", "Test", "pay_event@test.com", PatronType.STUDENT);
        testPatron.addFine(BigDecimal.valueOf(fines));
        testPatron = patronRepository.save(testPatron);
    }

    @When("a payment completed event arrives for that patron with amount {int}")
    public void receivePaymentEvent(int amount) throws Exception {
        String json = """
            {"eventType":"PaymentCompletedEvent","eventId":"evt-pay-1",
             "patronId":{"value":"%s"},"paymentId":{"value":"pay-1"},"amount":"%d"}
            """.formatted(testPatron.getId().getValue(), amount);
        handler.handle(objectMapper.readTree(json));
    }

    @Then("that patron's fine balance should become {int}")
    public void verifyFineBalance(int expected) {
        Patron updated = patronRepository.findById(testPatron.getId()).orElseThrow();
        assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }
}
