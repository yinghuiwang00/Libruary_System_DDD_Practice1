package com.library.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void shouldCreateFromBigDecimal() {
            Money money = new Money(new BigDecimal("99.99"));
            assertThat(money.getAmount()).isEqualByComparingTo("99.99");
            assertThat(money.getCurrency()).isEqualTo("CNY");
        }

        @Test
        void shouldCreateFromDouble() {
            Money money = Money.of(50.00);
            assertThat(money.getAmount()).isEqualByComparingTo("50.00");
        }

        @Test
        void shouldCreateWithCurrency() {
            Money money = Money.of(100, "USD");
            assertThat(money.getCurrency()).isEqualTo("USD");
        }

        @Test
        void shouldCreateZero() {
            Money zero = Money.zero();
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.getAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> new Money(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount must not be null");
        }

        @Test
        void shouldRejectNullCurrency() {
            assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectInvalidCurrency() {
            assertThatThrownBy(() -> new Money(BigDecimal.TEN, "US"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3-letter ISO code");
        }

        @Test
        void shouldScaleToTwoDecimalPlaces() {
            Money money = new Money(new BigDecimal("99.999"));
            assertThat(money.getAmount()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        void shouldAdd() {
            Money result = Money.of(10).add(Money.of(20));
            assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        }

        @Test
        void shouldSubtract() {
            Money result = Money.of(50).subtract(Money.of(20));
            assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        }

        @Test
        void shouldMultiplyByInt() {
            Money result = Money.of(10).multiply(3);
            assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        }

        @Test
        void shouldMultiplyByBigDecimal() {
            Money result = Money.of(10).multiply(new BigDecimal("1.5"));
            assertThat(result.getAmount()).isEqualByComparingTo("15.00");
        }

        @Test
        void shouldNegate() {
            Money result = Money.of(10).negate();
            assertThat(result.getAmount()).isEqualByComparingTo("-10.00");
        }

        @Test
        void shouldRejectDifferentCurrenciesForAdd() {
            Money cny = Money.of(10);
            Money usd = Money.of(10, "USD");
            assertThatThrownBy(() -> cny.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
        }

        @Test
        void shouldRejectDifferentCurrenciesForSubtract() {
            assertThatThrownBy(() -> Money.of(10).subtract(Money.of(5, "USD")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        void isPositiveShouldReturnTrueForPositive() {
            assertThat(Money.of(1).isPositive()).isTrue();
        }

        @Test
        void isPositiveShouldReturnFalseForZero() {
            assertThat(Money.zero().isPositive()).isFalse();
        }

        @Test
        void isNegativeShouldReturnTrueForNegative() {
            assertThat(Money.of(-1).isNegative()).isTrue();
        }

        @Test
        void isZeroShouldReturnTrueForZero() {
            assertThat(Money.zero().isZero()).isTrue();
        }

        @Test
        void shouldCompareGreaterThan() {
            assertThat(Money.of(10).isGreaterThan(Money.of(5))).isTrue();
            assertThat(Money.of(5).isGreaterThan(Money.of(10))).isFalse();
        }

        @Test
        void shouldCompareLessThan() {
            assertThat(Money.of(5).isLessThan(Money.of(10))).isTrue();
            assertThat(Money.of(10).isLessThan(Money.of(5))).isFalse();
        }

        @Test
        void shouldCompareGreaterThanOrEqual() {
            assertThat(Money.of(10).isGreaterThanOrEqual(Money.of(10))).isTrue();
            assertThat(Money.of(10).isGreaterThanOrEqual(Money.of(5))).isTrue();
            assertThat(Money.of(5).isGreaterThanOrEqual(Money.of(10))).isFalse();
        }

        @Test
        void shouldImplementComparable() {
            assertThat(Money.of(10).compareTo(Money.of(5))).isGreaterThan(0);
            assertThat(Money.of(5).compareTo(Money.of(10))).isLessThan(0);
            assertThat(Money.of(10).compareTo(Money.of(10))).isZero();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void shouldBeEqualWhenSameAmountAndCurrency() {
            assertThat(Money.of(100)).isEqualTo(Money.of(100));
        }

        @Test
        void shouldNotBeEqualWhenDifferentAmount() {
            assertThat(Money.of(100)).isNotEqualTo(Money.of(200));
        }

        @Test
        void shouldNotBeEqualWhenDifferentCurrency() {
            assertThat(Money.of(100)).isNotEqualTo(Money.of(100, "USD"));
        }

        @Test
        void shouldHaveSameHashCodeWhenEqual() {
            Money m1 = Money.of(100);
            Money m2 = Money.of(100);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        void shouldIncludeAmountAndCurrency() {
            assertThat(Money.of(99.99).toString()).isEqualTo("99.99 CNY");
        }

        @Test
        void shouldIncludeUSD() {
            assertThat(Money.of(50, "USD").toString()).isEqualTo("50.00 USD");
        }
    }
}
