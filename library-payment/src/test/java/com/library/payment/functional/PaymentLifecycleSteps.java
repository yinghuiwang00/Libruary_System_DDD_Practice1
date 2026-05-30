package com.library.payment.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.payment.application.dto.ApiResponse;
import com.library.payment.application.dto.PaymentDTO;
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
            state.setPaymentId(response.getData().getId());
        }
    }

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

    @SuppressWarnings("unchecked")
    private ApiResponse<PaymentDTO> readPaymentResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PaymentDTO.class));
    }
}
