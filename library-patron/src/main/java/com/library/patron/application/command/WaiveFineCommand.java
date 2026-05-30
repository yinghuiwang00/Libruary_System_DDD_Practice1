package com.library.patron.application.command;

import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;

public class WaiveFineCommand {

    private PatronId patronId;
    private BigDecimal amount;
    private String reason;

    public WaiveFineCommand() {
    }

    public WaiveFineCommand(PatronId patronId, BigDecimal amount, String reason) {
        this.patronId = patronId;
        this.amount = amount;
        this.reason = reason;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
