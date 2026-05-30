package com.library.patron.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.dto.ApiResponse;
import com.library.patron.application.dto.PatronDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class PatronLifecycleSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatronScenarioState state;

    @Given("a patron {string} is registered with email {string}")
    public void patronIsRegistered(String fullName, String email) throws Exception {
        registerPatron(fullName, email, "STUDENT");
    }

    @Given("a patron {string} is registered with email {string} as a {string}")
    public void patronIsRegisteredWithType(String fullName, String email, String patronType) throws Exception {
        registerPatron(fullName, email, patronType);
    }

    private void registerPatron(String fullName, String email, String patronType) throws Exception {
        String[] parts = fullName.split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : parts[0];

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("firstName", firstName);
        requestBody.put("lastName", lastName);
        requestBody.put("email", email);
        requestBody.put("patronType", patronType);

        MvcResult result = mockMvc.perform(post("/api/patrons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andReturn();

        ApiResponse<PatronDTO> response = readPatronResponse(result);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        state.setPatronId(response.getData().getId());
    }

    @When("the patron is suspended with reason {string}")
    public void patronIsSuspendedWithReason(String reason) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/suspend", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("the patron is reactivated with reason {string}")
    public void patronIsReactivatedWithReason(String reason) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/reactivate", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("the patron is terminated with reason {string}")
    public void patronIsTerminatedWithReason(String reason) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/terminate", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("the patron membership is extended by {int} months")
    public void patronMembershipIsExtended(int months) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("months", months);
        body.put("reason", "Extension");

        MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
            .andReturn();
        ApiResponse<PatronDTO> response = readPatronResponse(getResult);
        LocalDate expiryBefore = response.getData().getMembershipExpiry();
        state.setMembershipExpiryBefore(expiryBefore);

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/extend-membership", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("a fine of {double} is added with reason {string}")
    public void fineIsAdded(double amount, String reason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", BigDecimal.valueOf(amount));
        body.put("reason", reason);

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/fines", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("the patron pays {double} of the fines")
    public void patronPaysFines(double amount) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", BigDecimal.valueOf(amount));

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/fines/pay", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("{double} of the fines are waived")
    public void finesAreWaived(double amount) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", BigDecimal.valueOf(amount));
        body.put("reason", "Administrative waiver");

        state.setMvcResult(mockMvc.perform(post("/api/patrons/{id}/fines/waive", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @When("the patron type is changed to {string}")
    public void patronTypeIsChanged(String newType) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("patronType", newType);

        state.setMvcResult(mockMvc.perform(put("/api/patrons/{id}/type", state.getPatronId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn());
    }

    @Then("the outstanding fines should be {double}")
    public void outstandingFinesShouldBe(double expectedFines) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
            .andReturn();
        ApiResponse<PatronDTO> response = readPatronResponse(getResult);
        assertThat(response.getData().getOutstandingFines())
            .isEqualByComparingTo(BigDecimal.valueOf(expectedFines));
    }

    @Then("the patron status should be {string}")
    public void patronStatusShouldBe(String expectedStatus) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
            .andReturn();
        ApiResponse<PatronDTO> response = readPatronResponse(getResult);
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    @Then("the membership should be extended by {int} months")
    public void membershipShouldBeExtended(int months) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
            .andReturn();
        ApiResponse<PatronDTO> response = readPatronResponse(getResult);
        LocalDate expiryAfter = response.getData().getMembershipExpiry();

        LocalDate expiryBefore = state.getMembershipExpiryBefore();
        LocalDate expectedExpiry;
        if (expiryBefore != null) {
            expectedExpiry = expiryBefore.plusMonths(months);
        } else {
            expectedExpiry = LocalDate.now().plusMonths(months);
        }
        assertThat(expiryAfter).isEqualTo(expectedExpiry);
    }

    @Then("the patron type should be {string}")
    public void patronTypeShouldBe(String expectedType) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
            .andReturn();
        ApiResponse<PatronDTO> response = readPatronResponse(getResult);
        assertThat(response.getData().getPatronType()).isEqualTo(expectedType);
    }

    @Then("the operation should fail")
    public void operationShouldFail() {
        int status = state.getMvcResult().getResponse().getStatus();
        assertThat(status).isBetween(400, 499);
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<PatronDTO> readPatronResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PatronDTO.class));
    }
}
