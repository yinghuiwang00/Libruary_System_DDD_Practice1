package com.library.payment.application.command;

import com.library.shared.domain.model.RefundId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProcessRefundCommand {

    @NotNull(message = "Refund ID must not be null")
    private RefundId refundId;

    @NotBlank(message = "External refund ID must not be blank")
    private String externalRefundId;

    public ProcessRefundCommand() {
    }

    public ProcessRefundCommand(RefundId refundId, String externalRefundId) {
        this.refundId = refundId;
        this.externalRefundId = externalRefundId;
    }

    public RefundId getRefundId() { return refundId; }
    public void setRefundId(RefundId refundId) { this.refundId = refundId; }
    public String getExternalRefundId() { return externalRefundId; }
    public void setExternalRefundId(String externalRefundId) { this.externalRefundId = externalRefundId; }
}
