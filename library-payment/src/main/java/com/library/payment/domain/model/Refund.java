package com.library.payment.domain.model;

import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.model.enums.RefundStatus;
import com.library.shared.domain.model.RefundId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
    @Index(name = "idx_refund_status", columnList = "status")
})
public class Refund {

    @EmbeddedId
    private RefundId id;

    @Column(name = "payment_id", nullable = false, length = 36)
    private String paymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(length = 500)
    private String reason;

    @Column(name = "external_refund_id", length = 100)
    private String externalRefundId;

    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Refund() {
    }

    private Refund(RefundId id, String paymentId, BigDecimal amount, String reason) {
        this.id = Objects.requireNonNull(id, "Refund ID must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "Payment ID must not be null");
        this.amount = validateAmount(amount);
        this.reason = reason;
        this.status = RefundStatus.PENDING;
        this.requestedDate = LocalDateTime.now();
    }

    public static Refund create(RefundId id, String paymentId, BigDecimal amount, String reason) {
        return new Refund(id, paymentId, amount, reason);
    }

    public void process(String externalRefundId) {
        if (this.status != RefundStatus.PENDING) {
            throw new InvalidOperationException(
                "Cannot process refund in status: " + this.status);
        }
        this.status = RefundStatus.PROCESSING;
        this.externalRefundId = externalRefundId;
    }

    public void complete(String refundMethod) {
        if (this.status != RefundStatus.PROCESSING) {
            throw new InvalidOperationException(
                "Cannot complete refund in status: " + this.status);
        }
        this.status = RefundStatus.COMPLETED;
        this.processedDate = LocalDateTime.now();
        this.refundMethod = refundMethod;
    }

    public void fail() {
        if (this.status != RefundStatus.PROCESSING) {
            throw new InvalidOperationException(
                "Cannot fail refund in status: " + this.status);
        }
        this.status = RefundStatus.FAILED;
    }

    public void cancel() {
        if (this.status != RefundStatus.PENDING) {
            throw new InvalidOperationException(
                "Cannot cancel refund in status: " + this.status);
        }
        this.status = RefundStatus.CANCELLED;
    }

    public boolean isPending() {
        return this.status == RefundStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == RefundStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == RefundStatus.FAILED;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Refund amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        return amount;
    }

    // Getters
    public RefundId getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public RefundStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getExternalRefundId() { return externalRefundId; }
    public LocalDateTime getRequestedDate() { return requestedDate; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public String getRefundMethod() { return refundMethod; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
