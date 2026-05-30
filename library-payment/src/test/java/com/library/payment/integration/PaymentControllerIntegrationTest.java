package com.library.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.payment.application.dto.ApiResponse;
import com.library.payment.application.dto.PaymentDTO;
import com.library.payment.application.dto.RefundDTO;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayment_shouldReturn201() throws Exception {
        Map<String, Object> body = buildCreatePaymentBody("patron-001", 25.00);

        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getPatronId()).isEqualTo("patron-001");
        assertThat(response.getData().getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.getData().getStatus()).isEqualTo("PENDING");
        assertThat(response.getData().getPaymentType()).isEqualTo("FINE_PAYMENT");
        assertThat(response.getData().getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(response.getData().getReferenceNumber()).isNotBlank();
    }

    @Test
    void processPayment_shouldReturn200() throws Exception {
        String paymentId = createPaymentViaApi("patron-001", 50.00);

        Map<String, String> processBody = Map.of("externalTransactionId", "TXN-12345");
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/process", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processBody)))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void completePayment_shouldReturn200() throws Exception {
        String paymentId = createAndProcessPayment("patron-001", 30.00, "TXN-COMPLETE");

        MvcResult result = mockMvc.perform(post("/api/payments/{id}/complete", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getData().getPaymentDate()).isNotNull();
    }

    @Test
    void failPayment_shouldReturn200() throws Exception {
        String paymentId = createAndProcessPayment("patron-001", 40.00, "TXN-FAIL");

        Map<String, String> failBody = Map.of("reason", "Insufficient funds");
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/fail", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failBody)))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void cancelPayment_shouldReturn200() throws Exception {
        String paymentId = createPaymentViaApi("patron-001", 15.00);

        Map<String, String> cancelBody = Map.of("reason", "Patron requested cancellation");
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/cancel", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelBody)))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void getPayment_shouldReturn200() throws Exception {
        String paymentId = createPaymentViaApi("patron-001", 100.00);

        MvcResult result = mockMvc.perform(get("/api/payments/{id}", paymentId))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<PaymentDTO> response = readPaymentResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(paymentId);
        assertThat(response.getData().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void getPaymentsByPatron_shouldReturn200() throws Exception {
        createPaymentViaApi("patron-list", 10.00);
        createPaymentViaApi("patron-list", 20.00);

        MvcResult result = mockMvc.perform(get("/api/payments")
                .param("patronId", "patron-list"))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<PaymentDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    void requestRefund_shouldReturn201() throws Exception {
        String paymentId = createAndCompletePayment("patron-001", 75.00);

        Map<String, Object> refundBody = new HashMap<>();
        refundBody.put("amount", 75.00);
        refundBody.put("reason", "Overcharge");

        MvcResult result = mockMvc.perform(post("/api/payments/{id}/refunds", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundBody)))
            .andExpect(status().isCreated())
            .andReturn();

        ApiResponse<RefundDTO> response = readRefundResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getData().getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(response.getData().getStatus()).isEqualTo("PENDING");
        assertThat(response.getData().getReason()).isEqualTo("Overcharge");
    }

    @Test
    void processRefund_shouldReturn200() throws Exception {
        String paymentId = createAndCompletePayment("patron-001", 60.00);
        String refundId = createRefundViaApi(paymentId, 60.00, "Wrong amount");

        Map<String, String> processBody = Map.of("externalRefundId", "EXT-REF-001");
        MvcResult result = mockMvc.perform(post("/api/payments/refunds/{id}/process", refundId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processBody)))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<RefundDTO> response = readRefundResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void completeRefund_shouldReturn200() throws Exception {
        String paymentId = createAndCompletePayment("patron-001", 55.00);
        String refundId = createAndProcessRefund(paymentId, 55.00, "Duplicate charge");

        Map<String, String> completeBody = Map.of("refundMethod", "ORIGINAL_METHOD");
        MvcResult result = mockMvc.perform(post("/api/payments/refunds/{id}/complete", refundId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeBody)))
            .andExpect(status().isOk())
            .andReturn();

        ApiResponse<RefundDTO> response = readRefundResponse(result);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void getRefundsByPayment_shouldReturn200() throws Exception {
        String paymentId = createAndCompletePayment("patron-001", 100.00);
        createRefundViaApi(paymentId, 50.00, "Partial refund");

        MvcResult result = mockMvc.perform(get("/api/payments/{id}/refunds", paymentId))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<RefundDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                objectMapper.getTypeFactory().constructCollectionType(List.class, RefundDTO.class)));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void getPayment_withInvalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", "nonexistent-id"))
            .andExpect(status().isNotFound());
    }

    // --- Helper methods ---

    private Map<String, Object> buildCreatePaymentBody(String patronId, double amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("patronId", patronId);
        body.put("paymentType", PaymentType.FINE_PAYMENT.name());
        body.put("amount", amount);
        body.put("paymentMethod", PaymentMethod.CREDIT_CARD.name());
        body.put("description", "Test payment");
        return body;
    }

    private String createPaymentViaApi(String patronId, double amount) throws Exception {
        Map<String, Object> body = buildCreatePaymentBody(patronId, amount);
        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();
        return readPaymentResponse(result).getData().getId();
    }

    private String createAndProcessPayment(String patronId, double amount, String txnId) throws Exception {
        String paymentId = createPaymentViaApi(patronId, amount);
        Map<String, String> processBody = Map.of("externalTransactionId", txnId);
        mockMvc.perform(post("/api/payments/{id}/process", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processBody)))
            .andExpect(status().isOk());
        return paymentId;
    }

    private String createAndCompletePayment(String patronId, double amount) throws Exception {
        String paymentId = createAndProcessPayment(patronId, amount, "TXN-COMPLETE-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/payments/{id}/complete", paymentId))
            .andExpect(status().isOk());
        return paymentId;
    }

    private String createRefundViaApi(String paymentId, double amount, String reason) throws Exception {
        Map<String, Object> refundBody = new HashMap<>();
        refundBody.put("amount", amount);
        refundBody.put("reason", reason);
        MvcResult result = mockMvc.perform(post("/api/payments/{id}/refunds", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundBody)))
            .andExpect(status().isCreated())
            .andReturn();
        return readRefundResponse(result).getData().getId();
    }

    private String createAndProcessRefund(String paymentId, double amount, String reason) throws Exception {
        String refundId = createRefundViaApi(paymentId, amount, reason);
        Map<String, String> processBody = Map.of("externalRefundId", "EXT-REF-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/payments/refunds/{id}/process", refundId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processBody)))
            .andExpect(status().isOk());
        return refundId;
    }

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
