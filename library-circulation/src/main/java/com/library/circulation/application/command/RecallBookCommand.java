package com.library.circulation.application.command;

import jakarta.validation.constraints.NotBlank;

public class RecallBookCommand {

    @NotBlank(message = "Loan ID must not be blank")
    private String loanId;

    private String reason;

    public RecallBookCommand() {
    }

    public RecallBookCommand(String loanId, String reason) {
        this.loanId = loanId;
        this.reason = reason;
    }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
