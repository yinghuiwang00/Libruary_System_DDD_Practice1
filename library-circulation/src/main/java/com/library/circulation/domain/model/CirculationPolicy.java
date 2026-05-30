package com.library.circulation.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class CirculationPolicy {

    @Column(name = "loan_period_days")
    private Integer loanPeriodDays;

    @Column(name = "max_renewals_allowed")
    private Integer maxRenewalsAllowed;

    @Column(name = "daily_fine_rate", precision = 5, scale = 2)
    private BigDecimal dailyFineRate;

    @Column(name = "max_fine_amount", precision = 10, scale = 2)
    private BigDecimal maxFineAmount;

    @Column(name = "grace_period_days")
    private Integer gracePeriodDays;

    @Column(name = "hold_expiration_days")
    private Integer holdExpirationDays;

    @Column(name = "hold_pickup_days")
    private Integer holdPickupDays;

    @Column(name = "recall_notice_days")
    private Integer recallNoticeDays;

    @Column(name = "reminder_days_before_due")
    private Integer reminderDaysBeforeDue;

    protected CirculationPolicy() {
    }

    public CirculationPolicy(Integer loanPeriodDays, Integer maxRenewalsAllowed,
                             BigDecimal dailyFineRate, BigDecimal maxFineAmount,
                             Integer gracePeriodDays, Integer holdExpirationDays,
                             Integer holdPickupDays, Integer recallNoticeDays,
                             Integer reminderDaysBeforeDue) {
        this.loanPeriodDays = validatePositive(loanPeriodDays, "Loan period days");
        this.maxRenewalsAllowed = validateNonNegative(maxRenewalsAllowed, "Max renewals allowed");
        this.dailyFineRate = validatePositive(dailyFineRate, "Daily fine rate");
        this.maxFineAmount = validatePositive(maxFineAmount, "Max fine amount");
        this.gracePeriodDays = validateNonNegative(gracePeriodDays, "Grace period days");
        this.holdExpirationDays = validatePositive(holdExpirationDays, "Hold expiration days");
        this.holdPickupDays = validatePositive(holdPickupDays, "Hold pickup days");
        this.recallNoticeDays = validatePositive(recallNoticeDays, "Recall notice days");
        this.reminderDaysBeforeDue = validatePositive(reminderDaysBeforeDue, "Reminder days before due");
    }

    public static CirculationPolicy standard() {
        return new CirculationPolicy(
            30, 2, new BigDecimal("0.50"), new BigDecimal("50.00"),
            3, 7, 5, 7, 3
        );
    }

    public static CirculationPolicy faculty() {
        return new CirculationPolicy(
            90, 3, new BigDecimal("0.25"), new BigDecimal("30.00"),
            7, 14, 7, 14, 7
        );
    }

    public BigDecimal calculateFine(int overdueDays) {
        BigDecimal calculated = dailyFineRate.multiply(new BigDecimal(overdueDays));
        return calculated.min(maxFineAmount);
    }

    public boolean isInGracePeriod(int overdueDays) {
        return overdueDays > 0 && overdueDays <= gracePeriodDays;
    }

    private Integer validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private Integer validateNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private BigDecimal validatePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    public Integer getLoanPeriodDays() { return loanPeriodDays; }
    public Integer getMaxRenewalsAllowed() { return maxRenewalsAllowed; }
    public BigDecimal getDailyFineRate() { return dailyFineRate; }
    public BigDecimal getMaxFineAmount() { return maxFineAmount; }
    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public Integer getHoldExpirationDays() { return holdExpirationDays; }
    public Integer getHoldPickupDays() { return holdPickupDays; }
    public Integer getRecallNoticeDays() { return recallNoticeDays; }
    public Integer getReminderDaysBeforeDue() { return reminderDaysBeforeDue; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CirculationPolicy that = (CirculationPolicy) o;
        return Objects.equals(loanPeriodDays, that.loanPeriodDays)
                && Objects.equals(maxRenewalsAllowed, that.maxRenewalsAllowed)
                && Objects.equals(dailyFineRate, that.dailyFineRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanPeriodDays, maxRenewalsAllowed, dailyFineRate);
    }
}
