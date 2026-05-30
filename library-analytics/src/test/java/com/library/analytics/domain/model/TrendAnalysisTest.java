package com.library.analytics.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class TrendAnalysisTest {

    @Nested
    @DisplayName("Upward Trend")
    class UpwardTrend {

        @Test
        @DisplayName("should detect upward trend when current > previous")
        void shouldDetectUpwardTrend() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("120"), new BigDecimal("100"));

            assertThat(trend.isUpwardTrend()).isTrue();
            assertThat(trend.isDownwardTrend()).isFalse();
            assertThat(trend.getTrendDirection()).isEqualTo("UP");
        }

        @Test
        @DisplayName("should calculate positive change percentage for upward trend")
        void shouldCalculatePositiveChangePercentage() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("120"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("should detect upward trend from negative to positive")
        void shouldDetectUpwardTrendFromNegativeToPositive() {
            TrendAnalysis trend = new TrendAnalysis("balance",
                new BigDecimal("10"), new BigDecimal("-10"));

            assertThat(trend.isUpwardTrend()).isTrue();
        }
    }

    @Nested
    @DisplayName("Downward Trend")
    class DownwardTrend {

        @Test
        @DisplayName("should detect downward trend when current < previous")
        void shouldDetectDownwardTrend() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("80"), new BigDecimal("100"));

            assertThat(trend.isDownwardTrend()).isTrue();
            assertThat(trend.isUpwardTrend()).isFalse();
            assertThat(trend.getTrendDirection()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("should calculate negative change percentage for downward trend")
        void shouldCalculateNegativeChangePercentage() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("80"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("-20.00"));
        }
    }

    @Nested
    @DisplayName("Stable Trend")
    class StableTrend {

        @Test
        @DisplayName("should detect stable trend when current equals previous")
        void shouldDetectStableTrend() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("100"), new BigDecimal("100"));

            assertThat(trend.getTrendDirection()).isEqualTo("STABLE");
            assertThat(trend.isUpwardTrend()).isFalse();
            assertThat(trend.isDownwardTrend()).isFalse();
        }

        @Test
        @DisplayName("should have zero change percentage for stable trend")
        void shouldHaveZeroChangePercentage() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("100"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should have NEGLIGIBLE strength for stable trend")
        void shouldHaveNegligibleStrengthForStableTrend() {
            TrendAnalysis trend = new TrendAnalysis("circulation",
                new BigDecimal("100"), new BigDecimal("100"));

            assertThat(trend.getTrendStrength()).isEqualTo("NEGLIGIBLE");
        }
    }

    @Nested
    @DisplayName("Zero Previous Value")
    class ZeroPreviousValue {

        @Test
        @DisplayName("should return 100% change when previous is zero and current is positive")
        void shouldReturn100WhenPreviousIsZeroAndCurrentPositive() {
            TrendAnalysis trend = new TrendAnalysis("new_metric",
                new BigDecimal("50"), BigDecimal.ZERO);

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(trend.getTrendDirection()).isEqualTo("UP");
        }

        @Test
        @DisplayName("should return -100% change when previous is zero and current is negative")
        void shouldReturnNegative100WhenPreviousIsZeroAndCurrentNegative() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("-50"), BigDecimal.ZERO);

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("-100"));
            assertThat(trend.getTrendDirection()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("should return zero change when both are zero")
        void shouldReturnZeroWhenBothAreZero() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                BigDecimal.ZERO, BigDecimal.ZERO);

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(trend.getTrendDirection()).isEqualTo("STABLE");
        }
    }

    @Nested
    @DisplayName("Trend Strength")
    class TrendStrength {

        @Test
        @DisplayName("should be STRONG for change >= 25%")
        void shouldBeStrongForLargeChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("130"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("STRONG");
        }

        @Test
        @DisplayName("should be STRONG for change exactly 25%")
        void shouldBeStrongForExactly25Percent() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("125"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("STRONG");
        }

        @Test
        @DisplayName("should be MODERATE for change between 10% and 25%")
        void shouldBeModerateForMediumChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("115"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("MODERATE");
        }

        @Test
        @DisplayName("should be MODERATE for change exactly 10%")
        void shouldBeModerateForExactly10Percent() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("110"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("MODERATE");
        }

        @Test
        @DisplayName("should be WEAK for change between 1% and 10%")
        void shouldBeWeakForSmallChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("105"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("WEAK");
        }

        @Test
        @DisplayName("should be WEAK for change exactly 1%")
        void shouldBeWeakForExactly1Percent() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("101"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("WEAK");
        }

        @Test
        @DisplayName("should be NEGLIGIBLE for change less than 1%")
        void shouldBeNegligibleForTinyChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("100.50"), new BigDecimal("100"));

            assertThat(trend.getTrendStrength()).isEqualTo("NEGLIGIBLE");
        }

        @Test
        @DisplayName("should be NEGLIGIBLE for zero change")
        void shouldBeNegligibleForZeroChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("100"), new BigDecimal("100"));

            assertThat(trend.getTrendStrength()).isEqualTo("NEGLIGIBLE");
        }

        @Test
        @DisplayName("should be STRONG for large negative change")
        void shouldBeStrongForLargeNegativeChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("70"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("-30.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("STRONG");
        }

        @Test
        @DisplayName("should be MODERATE for negative change between -10% and -25%")
        void shouldBeModerateForMediumNegativeChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("85"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("-15.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("MODERATE");
        }

        @Test
        @DisplayName("should be WEAK for small negative change")
        void shouldBeWeakForSmallNegativeChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("95"), new BigDecimal("100"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("-5.00"));
            assertThat(trend.getTrendStrength()).isEqualTo("WEAK");
        }
    }

    @Nested
    @DisplayName("isSignificant")
    class IsSignificant {

        @Test
        @DisplayName("should be significant for STRONG trend")
        void shouldBeSignificantForStrongTrend() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("130"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isTrue();
        }

        @Test
        @DisplayName("should be significant for MODERATE trend")
        void shouldBeSignificantForModerateTrend() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("115"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isTrue();
        }

        @Test
        @DisplayName("should not be significant for WEAK trend")
        void shouldNotBeSignificantForWeakTrend() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("105"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isFalse();
        }

        @Test
        @DisplayName("should not be significant for NEGLIGIBLE trend")
        void shouldNotBeSignificantForNegligibleTrend() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("100.50"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isFalse();
        }

        @Test
        @DisplayName("should be significant for large negative change")
        void shouldBeSignificantForLargeNegativeChange() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("70"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isTrue();
        }

        @Test
        @DisplayName("should be significant exactly at 10% boundary")
        void shouldBeSignificantAtBoundary() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("110"), new BigDecimal("100"));

            assertThat(trend.isSignificant()).isTrue();
        }
    }

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw when metricName is null")
        void shouldThrowWhenMetricNameIsNull() {
            assertThatThrownBy(() -> new TrendAnalysis(null,
                new BigDecimal("100"), new BigDecimal("100")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Metric name must not be null");
        }

        @Test
        @DisplayName("should throw when currentValue is null")
        void shouldThrowWhenCurrentValueIsNull() {
            assertThatThrownBy(() -> new TrendAnalysis("metric",
                null, new BigDecimal("100")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Current value must not be null");
        }

        @Test
        @DisplayName("should throw when previousValue is null")
        void shouldThrowWhenPreviousValueIsNull() {
            assertThatThrownBy(() -> new TrendAnalysis("metric",
                new BigDecimal("100"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Previous value must not be null");
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("should return metric name")
        void shouldReturnMetricName() {
            TrendAnalysis trend = new TrendAnalysis("circulation_count",
                new BigDecimal("100"), new BigDecimal("80"));

            assertThat(trend.getMetricName()).isEqualTo("circulation_count");
        }

        @Test
        @DisplayName("should return current and previous values")
        void shouldReturnCurrentAndPreviousValues() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("120"), new BigDecimal("100"));

            assertThat(trend.getCurrentValue()).isEqualByComparingTo(new BigDecimal("120"));
            assertThat(trend.getPreviousValue()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    @Nested
    @DisplayName("Percentage Calculation")
    class PercentageCalculation {

        @Test
        @DisplayName("should handle decimal values correctly")
        void shouldHandleDecimalValues() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("123.45"), new BigDecimal("100.00"));

            assertThat(trend.getChangePercentage()).isEqualByComparingTo(new BigDecimal("23.45"));
        }

        @Test
        @DisplayName("should round change percentage to 2 decimal places")
        void shouldRoundToTwoDecimalPlaces() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("100"), new BigDecimal("3"));

            assertThat(trend.getChangePercentage().scale()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should handle very small previous value")
        void shouldHandleVerySmallPreviousValue() {
            TrendAnalysis trend = new TrendAnalysis("metric",
                new BigDecimal("1"), new BigDecimal("0.01"));

            assertThat(trend.getChangePercentage()).isNotNull();
            assertThat(trend.isUpwardTrend()).isTrue();
        }
    }
}
