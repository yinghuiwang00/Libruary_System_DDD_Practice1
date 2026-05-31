package com.library.patron.application.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookBorrowedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BookBorrowedEventHandler.class);
    private final PatronRepository patronRepository;
    public BookBorrowedEventHandler(PatronRepository patronRepository) { this.patronRepository = patronRepository; }

    @Transactional
    public void handle(JsonNode event) {
        String id = event.get("patronId").get("value").asText();
        log.info("Handling BookBorrowedEvent for patron: {}", id);
        try {
            Patron p = patronRepository.findById(PatronId.of(id)).orElseThrow(() -> new IllegalArgumentException("Patron not found: " + id));
            p.recordLoan();
            patronRepository.save(p);
        } catch (Exception e) { log.error("Failed to record loan for patron {}: {}", id, e.getMessage(), e); }
    }
}
