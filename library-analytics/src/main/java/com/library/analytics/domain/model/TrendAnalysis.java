package com.library.analytics.domain.model;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class TrendAnalysis {

    private String metricName;
    private BigDecimal currentValue;
    private BigDecimal previousValue;
    private BigDecimal changePercentage;
    private String trendDirection;
    private String trendStrength;

    protected TrendAnalysis() {
    }

    public TrendAnalysis(String metricName, BigDecimal currentValue, BigDecimal previousValue) {
        this.metricName = Objects.requireNonNull(metricName, "Metric name must not be null");
        this.currentValue = Objects.requireNonNull(currentValue, "Current value must not be null");
        this.previousValue = Objects.requireNonNull(previousValue, "Previous value must not be null");
        this.changePercentage = calculateChangePercentage(currentValue, previousValue);
        this.trendDirection = determineTrendDirection(currentValue, previousValue);
        this.trendStrength = determineTrendStrength(this.changePercentage);
    }

    public boolean isUpwardTrend() {
        return "UP".equals(this.trendDirection);
    }

    public boolean isDownwardTrend() {
        return "DOWN".equals(this.trendDirection);
    }

    public boolean isSignificant() {
        return this.changePercentage != null
            && this.changePercentage.abs().compareTo(BigDecimal.valueOf(10)) >= 0;
    }

    private BigDecimal calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return current.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(-100);
        }
        return current.subtract(previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    private String determineTrendDirection(BigDecimal current, BigDecimal previous) {
        int comparison = current.compareTo(previous);
        if (comparison > 0) return "UP";
        if (comparison < 0) return "DOWN";
        return "STABLE";
    }

    private String determineTrendStrength(BigDecimal changePercentage) {
        if (changePercentage == null) return "UNKNOWN";
        BigDecimal absChange = changePercentage.abs();
        if (absChange.compareTo(BigDecimal.valueOf(25)) >= 0) return "STRONG";
        if (absChange.compareTo(BigDecimal.valueOf(10)) >= 0) return "MODERATE";
        if (absChange.compareTo(BigDecimal.ONE) >= 0) return "WEAK";
        return "NEGLIGIBLE";
    }

    public String getMetricName() { return metricName; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public BigDecimal getPreviousValue() { return previousValue; }
    public BigDecimal getChangePercentage() { return changePercentage; }
    public String getTrendDirection() { return trendDirection; }
    public String getTrendStrength() { return trendStrength; }
}
