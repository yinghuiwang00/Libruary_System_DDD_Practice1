package com.library.circulation.domain.model;

import com.library.shared.domain.model.FineId;
import com.library.shared.domain.model.LoanId;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class Fine {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "fine_id"))
    private FineId id;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "overdue_days")
    private Integer overdueDays;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "waived_date")
    private LocalDateTime waivedDate;

    @Column(name = "waived_reason", length = 200)
    private String waivedReason;

    protected Fine() {
    }

    public Fine(FineId id, BigDecimal amount, int overdueDays, LocalDateTime calculatedAt) {
        this.id = Objects.requireNonNull(id, "Fine ID must not be null");
        this.amount = validateAmount(amount);
        this.overdueDays = overdueDays;
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "Calculated at must not be null");
    }

    public void pay(BigDecimal paymentAmount, LocalDateTime paymentDate) {
        if (this.paidDate != null) {
            throw new IllegalStateException("Fine has already been paid");
        }
        if (this.waivedDate != null) {
            throw new IllegalStateException("Fine has been waived");
        }
        Objects.requireNonNull(paymentAmount, "Payment amount must not be null");
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (paymentAmount.compareTo(this.amount) < 0) {
            throw new IllegalArgumentException(
                "Payment amount must cover the full fine amount. Required: " + this.amount + ", provided: " + paymentAmount);
        }
        this.paidDate = Objects.requireNonNull(paymentDate, "Payment date must not be null");
    }

    public void waive(String reason, LocalDateTime waivedDate) {
        if (this.paidDate != null) {
            throw new IllegalStateException("Cannot waive paid fine");
        }
        if (this.waivedDate != null) {
            throw new IllegalStateException("Fine has already been waived");
        }
        this.waivedDate = Objects.requireNonNull(waivedDate, "Waived date must not be null");
        this.waivedReason = Objects.requireNonNull(reason, "Waive reason must not be null");
    }

    public boolean isPaid() {
        return this.paidDate != null;
    }

    public boolean isWaived() {
        return this.waivedDate != null;
    }

    public boolean isOutstanding() {
        return !isPaid() && !isWaived();
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Fine amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fine amount must not be negative");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public FineId getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Integer getOverdueDays() { return overdueDays; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public LocalDateTime getPaidDate() { return paidDate; }
    public LocalDateTime getWaivedDate() { return waivedDate; }
    public String getWaivedReason() { return waivedReason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fine fine = (Fine) o;
        return Objects.equals(id, fine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
