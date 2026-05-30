package com.library.payment.application.dto;

import com.library.payment.domain.model.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundDTO {

    private String id;
    private String paymentId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private LocalDateTime requestedDate;
    private LocalDateTime processedDate;

    public RefundDTO() {
    }

    public static RefundDTO fromDomain(Refund refund) {
        RefundDTO dto = new RefundDTO();
        dto.id = refund.getId().getValue();
        dto.paymentId = refund.getPaymentId();
        dto.amount = refund.getAmount();
        dto.status = refund.getStatus().name();
        dto.reason = refund.getReason();
        dto.requestedDate = refund.getRequestedDate();
        dto.processedDate = refund.getProcessedDate();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDateTime requestedDate) { this.requestedDate = requestedDate; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }
}
