package com.library.staging.bdd;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Holds mutable scenario state shared across step definition classes.
 * Reset automatically via DB cleanup in StagingCucumberConfig @Before.
 */
@Component
public class StagingScenarioState {

    private String patronId;
    private String patronEmail;
    private String bookId;
    private int initialLoanCount;
    private BigDecimal initialFines;

    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }

    public String getPatronEmail() { return patronEmail; }
    public void setPatronEmail(String patronEmail) { this.patronEmail = patronEmail; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public int getInitialLoanCount() { return initialLoanCount; }
    public void setInitialLoanCount(int initialLoanCount) { this.initialLoanCount = initialLoanCount; }

    public BigDecimal getInitialFines() { return initialFines; }
    public void setInitialFines(BigDecimal initialFines) { this.initialFines = initialFines; }
}
