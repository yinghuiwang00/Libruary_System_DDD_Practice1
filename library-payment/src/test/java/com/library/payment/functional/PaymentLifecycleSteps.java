package com.library.payment.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.payment.application.dto.ApiResponse;
import com.library.payment.application.dto.PaymentDTO;
import com.library.payment.application.dto.RefundDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PaymentLifecycleSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentScenarioState state;

    // --- Given steps ---

    @Given("a payment is created for patron {string} with amount {double}")
    public void aPaymentIsCreatedForPatronWithAmount(String patronId, double amount) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("patronId", patronId);
        body.put("paymentType", "FINE_PAYMENT");
        body.put("amount", amount);
        body.put("paymentMethod", "CREDIT_CARD");
        body.put("description", "Cucumber test payment");

        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();

        state.setMvcResult(result);
        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        if (response.getData() != null) {
            state.addPaymentId(response.getData().getId());
        }
    }

    @Given("another payment is created for patron {string} with amount {double}")
    public void anotherPaymentIsCreatedForPatronWithAmount(String patronId, double amount) throws Exception {
        aPaymentIsCreatedForPatronWithAmount(patronId, amount);
    }

    @Given("a completed payment for patron {string} with amount {double}")
    public void aCompletedPaymentForPatronWithAmount(String patronId, double amount) throws Exception {
        // Create the payment
        aPaymentIsCreatedForPatronWithAmount(patronId, amount);

        // Process it
        Map<String, String> processBody = Map.of("externalTransactionId", "TXN-AUTO-" + System.nanoTime());
        mockMvc.perform(post("/api/payments/{id}/process", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processBody)))
            .andReturn();

        // Complete it
        mockMvc.perform(post("/api/payments/{id}/complete", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    }

    // --- When steps ---

    @When("the payment is processed with external transaction {string}")
    public void thePaymentIsProcessedWithExternalTransaction(String txnId) throws Exception {
        Map<String, String> body = Map.of("externalTransactionId", txnId);
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/process", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the payment is completed")
    public void thePaymentIsCompleted() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/complete", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the payment is cancelled with reason {string}")
    public void thePaymentIsCancelledWithReason(String reason) throws Exception {
        Map<String, String> body = Map.of("reason", reason);
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/cancel", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the payment fails with reason {string}")
    public void thePaymentFailsWithReason(String reason) throws Exception {
        Map<String, String> body = Map.of("reason", reason);
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/fail", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("a refund of {double} is requested with reason {string}")
    public void aRefundOfIsRequestedWithReason(double amount, String reason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("reason", reason);

        MvcResult result = mockMvc.perform(post("/api/payments/{id}/refunds", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();

        state.setMvcResult(result);
        ApiResponse<RefundDTO> response = readRefundResponse(result);
        if (response.getData() != null) {
            state.setRefundId(response.getData().getId());
        }
    }

    @When("the refund is processed")
    public void theRefundIsProcessed() throws Exception {
        Map<String, String> body = Map.of("externalRefundId", "EXT-REF-" + System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/payments/refunds/{id}/process", state.getRefundId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the refund is completed")
    public void theRefundIsCompleted() throws Exception {
        Map<String, String> body = Map.of("refundMethod", "ORIGINAL_METHOD");
        MvcResult result = mockMvc.perform(post("/api/payments/refunds/{id}/complete", state.getRefundId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        state.setMvcResult(result);
    }

    @When("the refund is processed and completed")
    public void theRefundIsProcessedAndCompleted() throws Exception {
        theRefundIsProcessed();
        theRefundIsCompleted();
    }

    @When("I query payments for patron {string}")
    public void iQueryPaymentsForPatron(String patronId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/payments")
                .param("patronId", patronId)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        state.setMvcResult(result);
    }

    // --- Then steps ---

    @Then("the payment status is {string}")
    public void thePaymentStatusIs(String expectedStatus) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/payments/{id}", state.getPaymentId()))
            .andReturn();
        ApiResponse<PaymentDTO> response = readPaymentResponse(getResult);
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    @Then("the payment amount is {double}")
    public void thePaymentAmountIs(double expectedAmount) throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/payments/{id}", state.getPaymentId()))
            .andReturn();
        ApiResponse<PaymentDTO> response = readPaymentResponse(getResult);
        assertThat(response.getData().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(expectedAmount));
    }

    @Then("the refund should be in {string} status")
    public void theRefundShouldBeInStatus(String expectedStatus) throws Exception {
        // The mvcResult from the refund request should contain the refund status
        ApiResponse<RefundDTO> response = readRefundResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    @Then("the payment refund amount should be {double}")
    public void thePaymentRefundAmountShouldBe(double expectedRefundAmount) throws Exception {
        // Query refunds for the payment and sum completed refund amounts
        MvcResult result = mockMvc.perform(get("/api/payments/{id}/refunds", state.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<RefundDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, RefundDTO.class)
            ));

        BigDecimal totalRefunded = response.getData().stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()))
            .map(RefundDTO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalRefunded).isEqualByComparingTo(BigDecimal.valueOf(expectedRefundAmount));
    }

    @Then("I should see {int} payments")
    public void iShouldSeePayments(int expectedCount) throws Exception {
        String json = state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<PaymentDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentDTO.class)
            ));
        assertThat(response.getData()).hasSize(expectedCount);
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    private ApiResponse<PaymentDTO> readPaymentResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PaymentDTO.class));
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<RefundDTO> readRefundResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, RefundDTO.class));
    }
}
