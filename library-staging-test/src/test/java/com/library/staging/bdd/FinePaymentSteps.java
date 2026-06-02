package com.library.staging.bdd;

import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

public class FinePaymentSteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";
    private static final String PAYMENT_TOPIC = "library.payment.events";

    @Autowired
    private StagingCucumberConfig config;

    @Autowired
    private StagingScenarioState state;

    @Autowired
    private PatronRepository patronRepository;

    @Given("a patron {string} with email {string} exists with outstanding fines of {double}")
    public void createPatronWithFines(String name, String email, double fines) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        patron.addFine(BigDecimal.valueOf(fines));
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @When("a FineIncurredEvent is published for that patron with fine {string}, loan {string}, amount {double}")
    public void publishFineIncurredEvent(String fineId, String loanId, double amount) {
        String eventJson = buildEventJson("FineIncurredEvent",
            "fineId", idObject(fineId),
            "loanId", idObject(loanId),
            "patronId", idObject(state.getPatronId()),
            "amount", String.valueOf(amount),
            "overdueDays", "10",
            "incurredAt", jsonString("2026-05-31T10:00:00")
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
    }

    @When("a PaymentCompletedEvent is published for that patron with payment {string}, amount {double}, reference {string}")
    public void publishPaymentCompletedEvent(String paymentId, double amount, String reference) {
        String eventJson = buildEventJson("PaymentCompletedEvent",
            "paymentId", idObject(paymentId),
            "patronId", idObject(state.getPatronId()),
            "amount", String.valueOf(amount),
            "referenceNumber", jsonString(reference),
            "paymentDate", jsonString("2026-05-31T12:00:00")
        );
        config.getKafkaTemplate().send(PAYMENT_TOPIC, eventJson);
    }

    @Then("the patron's outstanding fines should be {double}")
    public void verifyOutstandingFines(double expectedFines) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(
                PatronId.of(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getOutstandingFines()).isEqualByComparingTo(BigDecimal.valueOf(expectedFines));
        });
    }

    // --- JSON helpers ---
    private String buildEventJson(String eventType, String... keyValuePairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"eventType\":\"").append(eventType).append("\"");
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            sb.append(",\"").append(keyValuePairs[i]).append("\":").append(keyValuePairs[i + 1]);
        }
        sb.append("}");
        return sb.toString();
    }

    private String idObject(String idValue) {
        return "{\"value\":\"" + idValue + "\"}";
    }

    private String jsonString(String value) {
        return "\"" + value + "\"";
    }
}
