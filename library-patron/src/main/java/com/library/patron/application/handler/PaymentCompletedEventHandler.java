package com.library.patron.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
public class PaymentCompletedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedEventHandler.class);
    private final PatronRepository patronRepository;
    public PaymentCompletedEventHandler(PatronRepository patronRepository) { this.patronRepository = patronRepository; }

    @Transactional
    public void handle(JsonNode event) {
        String id = event.get("patronId").get("value").asText();
        BigDecimal amount = new BigDecimal(event.get("amount").asText());
        log.info("Handling PaymentCompletedEvent for patron: {}, amount: {}", id, amount);
        try {
            Patron p = patronRepository.findById(PatronId.of(id)).orElseThrow(() -> new IllegalArgumentException("Patron not found: " + id));
            p.payFine(amount);
            patronRepository.save(p);
        } catch (Exception e) { log.error("Failed to record payment for patron {}: {}", id, e.getMessage(), e); }
    }
}
