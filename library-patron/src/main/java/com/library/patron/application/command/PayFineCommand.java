package com.library.patron.application.command;

import com.library.shared.domain.model.PatronId;

import java.math.BigDecimal;

public class PayFineCommand {

    private PatronId patronId;
    private BigDecimal amount;

    public PayFineCommand() {
    }

    public PayFineCommand(PatronId patronId, BigDecimal amount) {
        this.patronId = patronId;
        this.amount = amount;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
