package com.library.payment.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.payment.domain.service.PaymentService;
import com.library.shared.domain.model.PatronId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component("paymentFineIncurredEventHandler")
public class FineIncurredEventHandler {
    private static final Logger log = LoggerFactory.getLogger(FineIncurredEventHandler.class);
    private final PaymentService paymentService;
    public FineIncurredEventHandler(PaymentService paymentService) { this.paymentService = paymentService; }

    @Transactional
    public void handle(JsonNode event) {
        String patronId = event.get("patronId").get("value").asText();
        BigDecimal amount = new BigDecimal(event.get("amount").asText());
        int overdueDays = event.get("overdueDays").asInt();
        log.info("Handling FineIncurredEvent for patron: {}, amount: {}", patronId, amount);
        try {
            paymentService.createPayment(PatronId.of(patronId), PaymentType.FINE_PAYMENT, amount, PaymentMethod.PENDING, "Library fine, " + overdueDays + " days overdue");
        } catch (Exception e) { log.error("Failed to create fine payment for patron {}: {}", patronId, e.getMessage(), e); }
    }
}
