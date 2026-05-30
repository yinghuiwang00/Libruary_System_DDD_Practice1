package com.library.patron.application.command;

import com.library.shared.domain.model.PatronId;

public class ExtendMembershipCommand {

    private PatronId patronId;
    private int months;
    private String reason;

    public ExtendMembershipCommand() {
    }

    public ExtendMembershipCommand(PatronId patronId, int months, String reason) {
        this.patronId = patronId;
        this.months = months;
        this.reason = reason;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
