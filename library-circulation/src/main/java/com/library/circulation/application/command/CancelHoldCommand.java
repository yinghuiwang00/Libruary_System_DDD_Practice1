package com.library.circulation.application.command;

import jakarta.validation.constraints.NotBlank;

public class CancelHoldCommand {

    @NotBlank(message = "Hold ID must not be blank")
    private String holdId;

    private String reason;

    public CancelHoldCommand() {
    }

    public CancelHoldCommand(String holdId, String reason) {
        this.holdId = holdId;
        this.reason = reason;
    }

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
