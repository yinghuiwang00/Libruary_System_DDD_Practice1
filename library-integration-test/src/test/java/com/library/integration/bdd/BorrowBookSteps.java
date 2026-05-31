package com.library.integration.bdd;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BorrowBookSteps {

    private static final String CIRCULATION_TOPIC = "library.circulation.events";

    @Autowired
    private CucumberSpringConfig config;

    @Autowired
    private E2EScenarioState state;

    @Autowired
    private com.library.patron.domain.repository.PatronRepository patronRepository;

    @When("a BookBorrowedEvent is published for that patron with loan {string}, copy {string}, book {string}")
    public void publishBorrowEvent(String loanId, String copyId, String bookId) {
        String eventJson = buildEventJson("BookBorrowedEvent",
            "loanId", idObject(loanId),
            "copyId", idObject(copyId),
            "patronId", idObject(state.getPatronId()),
            "bookId", idObject(bookId),
            "loanDate", jsonString("2026-05-31T10:00:00"),
            "dueDate", jsonString("2026-06-30T10:00:00")
        );
        config.getKafkaTemplate().send(CIRCULATION_TOPIC, eventJson);
    }

    @Then("the patron's total borrowed count should be {int}")
    public void verifyTotalBorrowed(int expected) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            com.library.patron.domain.model.Patron updated = patronRepository.findById(
                com.library.shared.domain.model.PatronId.of(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getTotalBorrowed()).isEqualTo(expected);
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
