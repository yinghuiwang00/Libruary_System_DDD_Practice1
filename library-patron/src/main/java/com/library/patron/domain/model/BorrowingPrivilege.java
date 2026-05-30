package com.library.patron.domain.model;

import com.library.patron.domain.model.enums.PatronType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class BorrowingPrivilege {

    @Column(name = "max_loans")
    private Integer maxLoans;

    @Column(name = "loan_period_days")
    private Integer loanPeriodDays;

    @Column(name = "max_renewals")
    private Integer maxRenewals;

    @Column(name = "daily_fine_rate", precision = 5, scale = 2)
    private BigDecimal dailyFineRate;

    @Column(name = "max_fine_amount", precision = 10, scale = 2)
    private BigDecimal maxFineAmount;

    @Column(name = "can_place_holds")
    private Boolean canPlaceHolds;

    @Column(name = "max_holds")
    private Integer maxHolds;

    @Column(name = "can_recall_books")
    private Boolean canRecallBooks;

    protected BorrowingPrivilege() {
    }

    public BorrowingPrivilege(PatronType patronType) {
        Objects.requireNonNull(patronType, "Patron type must not be null");
        applyDefaults(patronType);
    }

    public BorrowingPrivilege(Integer maxLoans, Integer loanPeriodDays, Integer maxRenewals,
                              BigDecimal dailyFineRate, BigDecimal maxFineAmount,
                              Boolean canPlaceHolds, Integer maxHolds, Boolean canRecallBooks) {
        this.maxLoans = validatePositive(maxLoans, "Max loans");
        this.loanPeriodDays = validatePositive(loanPeriodDays, "Loan period days");
        this.maxRenewals = validateNonNegative(maxRenewals, "Max renewals");
        this.dailyFineRate = validatePositive(dailyFineRate, "Daily fine rate");
        this.maxFineAmount = validatePositive(maxFineAmount, "Max fine amount");
        this.canPlaceHolds = canPlaceHolds != null && canPlaceHolds;
        this.maxHolds = validateNonNegative(maxHolds, "Max holds");
        this.canRecallBooks = canRecallBooks != null && canRecallBooks;
    }

    private void applyDefaults(PatronType patronType) {
        switch (patronType) {
            case STUDENT -> {
                this.maxLoans = 5; this.loanPeriodDays = 21; this.maxRenewals = 1;
                this.dailyFineRate = new BigDecimal("0.50"); this.maxFineAmount = new BigDecimal("30.00");
                this.canPlaceHolds = true; this.maxHolds = 3; this.canRecallBooks = false;
            }
            case FACULTY -> {
                this.maxLoans = 20; this.loanPeriodDays = 90; this.maxRenewals = 3;
                this.dailyFineRate = new BigDecimal("0.25"); this.maxFineAmount = new BigDecimal("50.00");
                this.canPlaceHolds = true; this.maxHolds = 10; this.canRecallBooks = true;
            }
            case STAFF -> {
                this.maxLoans = 10; this.loanPeriodDays = 30; this.maxRenewals = 2;
                this.dailyFineRate = new BigDecimal("0.30"); this.maxFineAmount = new BigDecimal("40.00");
                this.canPlaceHolds = true; this.maxHolds = 5; this.canRecallBooks = false;
            }
            case ALUMNI -> {
                this.maxLoans = 3; this.loanPeriodDays = 14; this.maxRenewals = 0;
                this.dailyFineRate = new BigDecimal("0.75"); this.maxFineAmount = new BigDecimal("20.00");
                this.canPlaceHolds = true; this.maxHolds = 2; this.canRecallBooks = false;
            }
            case COMMUNITY -> {
                this.maxLoans = 2; this.loanPeriodDays = 7; this.maxRenewals = 0;
                this.dailyFineRate = new BigDecimal("1.00"); this.maxFineAmount = new BigDecimal("15.00");
                this.canPlaceHolds = false; this.maxHolds = 0; this.canRecallBooks = false;
            }
        }
    }

    public boolean hasRenewalQuota(int currentRenewals) {
        return currentRenewals < this.maxRenewals;
    }

    public boolean hasHoldQuota(int currentHolds) {
        return Boolean.TRUE.equals(this.canPlaceHolds) && currentHolds < this.maxHolds;
    }

    private Integer validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) throw new IllegalArgumentException(fieldName + " must be positive");
        return value;
    }

    private Integer validateNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) throw new IllegalArgumentException(fieldName + " must be non-negative");
        return value;
    }

    private BigDecimal validatePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(fieldName + " must be positive");
        return value;
    }

    public Integer getMaxLoans() { return maxLoans; }
    public Integer getLoanPeriodDays() { return loanPeriodDays; }
    public Integer getMaxRenewals() { return maxRenewals; }
    public BigDecimal getDailyFineRate() { return dailyFineRate; }
    public BigDecimal getMaxFineAmount() { return maxFineAmount; }
    public Boolean getCanPlaceHolds() { return canPlaceHolds; }
    public Integer getMaxHolds() { return maxHolds; }
    public Boolean getCanRecallBooks() { return canRecallBooks; }
}
