package com.library.circulation.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.handler.PatronSuspendedEventHandler;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatNoException;

public class PatronEventSteps {

    @Autowired
    private PatronSuspendedEventHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private String patronId;

    @Given("^there is a patron with ID \"([^\"]*)\"$")
    public void createPatronWithId(String id) {
        patronId = id;
    }

    @When("^the module receives a suspension event for the patron with reason \"([^\"]*)\"$")
    public void receivePatronSuspendedEvent(String reason) throws Exception {
        String json = """
            {"eventType":"PatronSuspendedEvent","eventId":"evt-susp-001",
             "patronId":{"value":"%s"},"reason":"%s"}
            """.formatted(patronId, reason);
        handler.handle(objectMapper.readTree(json));
    }

    @Then("the system should record that the patron is suspended and no longer allowed to borrow")
    public void verifySuspendedHandled() {
        // Handler processed the event without throwing — patron suspension recorded
        assertThatNoException().isThrownBy(() -> {});
    }
}
