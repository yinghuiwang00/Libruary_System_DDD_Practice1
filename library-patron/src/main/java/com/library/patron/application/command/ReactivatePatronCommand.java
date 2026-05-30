package com.library.patron.application.command;

import com.library.shared.domain.model.PatronId;

public class ReactivatePatronCommand {

    private PatronId patronId;
    private String reason;

    public ReactivatePatronCommand() {
    }

    public ReactivatePatronCommand(PatronId patronId, String reason) {
        this.patronId = patronId;
        this.reason = reason;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
