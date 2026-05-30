package com.library.payment.application.dto;

import com.library.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {

    private String id;
    private String patronId;
    private String paymentType;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String referenceNumber;
    private LocalDateTime paymentDate;
    private String description;
    private String currency;

    public PaymentDTO() {
    }

    public static PaymentDTO fromDomain(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.id = payment.getId().getValue();
        dto.patronId = payment.getPatronId().getValue();
        dto.paymentType = payment.getPaymentType().name();
        dto.amount = payment.getAmount();
        dto.paymentMethod = payment.getPaymentMethod().name();
        dto.status = payment.getStatus().name();
        dto.referenceNumber = payment.getReferenceNumber();
        dto.paymentDate = payment.getPaymentDate();
        dto.description = payment.getDescription();
        dto.currency = payment.getCurrency();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
