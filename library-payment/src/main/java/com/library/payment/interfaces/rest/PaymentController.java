package com.library.payment.interfaces.rest;

import com.library.payment.application.command.*;
import com.library.payment.application.dto.ApiResponse;
import com.library.payment.application.dto.PaymentDTO;
import com.library.payment.application.dto.RefundDTO;
import com.library.payment.application.service.PaymentApplicationService;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "APIs for managing payments and refunds")
public class PaymentController {

    private final PaymentApplicationService paymentService;

    public PaymentController(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Create a new payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@RequestBody Map<String, Object> body) {
        CreatePaymentCommand command = new CreatePaymentCommand(
            PatronId.of((String) body.get("patronId")),
            PaymentType.valueOf((String) body.get("paymentType")),
            new BigDecimal(body.get("amount").toString()),
            PaymentMethod.valueOf((String) body.get("paymentMethod")),
            (String) body.get("description")
        );
        PaymentDTO payment = paymentService.createPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process a pending payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> processPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
            PaymentId.of(id),
            body.get("externalTransactionId")
        );
        PaymentDTO payment = paymentService.processPayment(command);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a processing payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> completePayment(@PathVariable String id) {
        CompletePaymentCommand command = new CompletePaymentCommand(PaymentId.of(id));
        PaymentDTO payment = paymentService.completePayment(command);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark a payment as failed")
    public ResponseEntity<ApiResponse<PaymentDTO>> failPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        FailPaymentCommand command = new FailPaymentCommand(
            PaymentId.of(id),
            body.get("reason")
        );
        PaymentDTO payment = paymentService.failPayment(command);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> cancelPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        CancelPaymentCommand command = new CancelPaymentCommand(
            PaymentId.of(id),
            body.get("reason")
        );
        PaymentDTO payment = paymentService.cancelPayment(command);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable String id) {
        PaymentDTO payment = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping
    @Operation(summary = "List payments by patron ID")
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getPaymentsByPatron(
            @RequestParam String patronId) {
        List<PaymentDTO> payments = paymentService.getPaymentsByPatron(patronId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{id}/refunds")
    @Operation(summary = "Request a refund for a payment")
    public ResponseEntity<ApiResponse<RefundDTO>> requestRefund(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        RequestRefundCommand command = new RequestRefundCommand(
            PaymentId.of(id),
            new BigDecimal(body.get("amount").toString()),
            (String) body.get("reason")
        );
        RefundDTO refund = paymentService.requestRefund(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(refund));
    }

    @GetMapping("/{id}/refunds")
    @Operation(summary = "List refunds for a payment")
    public ResponseEntity<ApiResponse<List<RefundDTO>>> getRefundsByPayment(@PathVariable String id) {
        List<RefundDTO> refunds = paymentService.getRefundsByPayment(id);
        return ResponseEntity.ok(ApiResponse.success(refunds));
    }

    @PostMapping("/refunds/{id}/process")
    @Operation(summary = "Process a pending refund")
    public ResponseEntity<ApiResponse<RefundDTO>> processRefund(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        ProcessRefundCommand command = new ProcessRefundCommand(
            RefundId.of(id),
            body.get("externalRefundId")
        );
        RefundDTO refund = paymentService.processRefund(command);
        return ResponseEntity.ok(ApiResponse.success(refund));
    }

    @PostMapping("/refunds/{id}/complete")
    @Operation(summary = "Complete a processing refund")
    public ResponseEntity<ApiResponse<RefundDTO>> completeRefund(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        CompleteRefundCommand command = new CompleteRefundCommand(
            RefundId.of(id),
            body.get("refundMethod")
        );
        RefundDTO refund = paymentService.completeRefund(command);
        return ResponseEntity.ok(ApiResponse.success(refund));
    }
}
