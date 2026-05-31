package com.library.payment.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.payment.application.handler.FineIncurredEventHandler;
import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.payment.domain.repository.PaymentRepository;
import com.library.shared.domain.model.PatronId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FinePaymentEventSteps {

    @Autowired
    private FineIncurredEventHandler handler;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String patronId;

    @Given("^a patron exists with ID \"([^\"]*)\"$")
    public void createPatronWithId(String id) {
        patronId = id;
    }

    @When("^the module receives a fine event for the patron with amount \"([^\"]*)\" and (\\d+) overdue days$")
    public void receiveFineIncurredEvent(String amount, int overdueDays) throws Exception {
        String json = """
            {"eventType":"FineIncurredEvent","eventId":"evt-fine-001",
             "patronId":{"value":"%s"},"amount":"%s","overdueDays":%d}
            """.formatted(patronId, amount, overdueDays);
        handler.handle(objectMapper.readTree(json));
    }

    @Then("^the system should create a fine payment record for the patron with amount \"([^\"]*)\"$")
    public void verifyPaymentCreated(String expectedAmount) {
        List<Payment> payments = paymentRepository.findByPatronId(PatronId.of(patronId));
        assertThat(payments).hasSize(1);
        Payment payment = payments.get(0);
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal(expectedAmount));
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.FINE_PAYMENT);
    }
}
