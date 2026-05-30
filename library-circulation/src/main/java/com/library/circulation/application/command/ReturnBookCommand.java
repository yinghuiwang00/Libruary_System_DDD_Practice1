package com.library.circulation.application.command;

import jakarta.validation.constraints.NotBlank;

public class ReturnBookCommand {

    @NotBlank(message = "Loan ID must not be blank")
    private String loanId;

    public ReturnBookCommand() {
    }

    public ReturnBookCommand(String loanId) {
        this.loanId = loanId;
    }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
}
