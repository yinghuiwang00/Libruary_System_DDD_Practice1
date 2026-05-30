package com.library.payment.application.command;

import com.library.shared.domain.model.RefundId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompleteRefundCommand {

    @NotNull(message = "Refund ID must not be null")
    private RefundId refundId;

    @NotBlank(message = "Refund method must not be blank")
    private String refundMethod;

    public CompleteRefundCommand() {
    }

    public CompleteRefundCommand(RefundId refundId, String refundMethod) {
        this.refundId = refundId;
        this.refundMethod = refundMethod;
    }

    public RefundId getRefundId() { return refundId; }
    public void setRefundId(RefundId refundId) { this.refundId = refundId; }
    public String getRefundMethod() { return refundMethod; }
    public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }
}
