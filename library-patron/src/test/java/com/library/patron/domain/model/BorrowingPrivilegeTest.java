package com.library.patron.domain.model;

import com.library.patron.domain.model.enums.PatronType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class BorrowingPrivilegeTest {

    // ---------------------------------------------------------------------------
    // Constructor with PatronType - defaults
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor with PatronType defaults")
    class PatronTypeDefaults {

        @Test
        @DisplayName("should reject null patron type")
        void shouldRejectNull() {
            assertThatThrownBy(() -> new BorrowingPrivilege((PatronType) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Patron type");
        }

        @Test
        @DisplayName("STUDENT should have correct defaults")
        void studentDefaults() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT);

            assertThat(bp.getMaxLoans()).isEqualTo(5);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(21);
            assertThat(bp.getMaxRenewals()).isEqualTo(1);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("0.50"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(bp.getCanPlaceHolds()).isTrue();
            assertThat(bp.getMaxHolds()).isEqualTo(3);
            assertThat(bp.getCanRecallBooks()).isFalse();
        }

        @Test
        @DisplayName("FACULTY should have correct defaults")
        void facultyDefaults() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.FACULTY);

            assertThat(bp.getMaxLoans()).isEqualTo(20);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(90);
            assertThat(bp.getMaxRenewals()).isEqualTo(3);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("0.25"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(bp.getCanPlaceHolds()).isTrue();
            assertThat(bp.getMaxHolds()).isEqualTo(10);
            assertThat(bp.getCanRecallBooks()).isTrue();
        }

        @Test
        @DisplayName("STAFF should have correct defaults")
        void staffDefaults() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STAFF);

            assertThat(bp.getMaxLoans()).isEqualTo(10);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(30);
            assertThat(bp.getMaxRenewals()).isEqualTo(2);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("0.30"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
            assertThat(bp.getCanPlaceHolds()).isTrue();
            assertThat(bp.getMaxHolds()).isEqualTo(5);
            assertThat(bp.getCanRecallBooks()).isFalse();
        }

        @Test
        @DisplayName("ALUMNI should have correct defaults")
        void alumniDefaults() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.ALUMNI);

            assertThat(bp.getMaxLoans()).isEqualTo(3);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(14);
            assertThat(bp.getMaxRenewals()).isEqualTo(0);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("0.75"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(bp.getCanPlaceHolds()).isTrue();
            assertThat(bp.getMaxHolds()).isEqualTo(2);
            assertThat(bp.getCanRecallBooks()).isFalse();
        }

        @Test
        @DisplayName("COMMUNITY should have correct defaults")
        void communityDefaults() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.COMMUNITY);

            assertThat(bp.getMaxLoans()).isEqualTo(2);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(7);
            assertThat(bp.getMaxRenewals()).isEqualTo(0);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(bp.getCanPlaceHolds()).isFalse();
            assertThat(bp.getMaxHolds()).isEqualTo(0);
            assertThat(bp.getCanRecallBooks()).isFalse();
        }
    }

    // ---------------------------------------------------------------------------
    // Custom constructor
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Custom constructor")
    class CustomConstructor {

        @Test
        @DisplayName("should create with valid parameters")
        void shouldCreateValid() {
            BorrowingPrivilege bp = new BorrowingPrivilege(
                15, 30, 2,
                new BigDecimal("0.40"), new BigDecimal("25.00"),
                true, 5, false
            );

            assertThat(bp.getMaxLoans()).isEqualTo(15);
            assertThat(bp.getLoanPeriodDays()).isEqualTo(30);
            assertThat(bp.getMaxRenewals()).isEqualTo(2);
            assertThat(bp.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("0.40"));
            assertThat(bp.getMaxFineAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(bp.getCanPlaceHolds()).isTrue();
            assertThat(bp.getMaxHolds()).isEqualTo(5);
            assertThat(bp.getCanRecallBooks()).isFalse();
        }

        @Test
        @DisplayName("should treat null canPlaceHolds as false")
        void nullCanPlaceHoldsIsFalse() {
            BorrowingPrivilege bp = new BorrowingPrivilege(
                5, 14, 0,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                null, 0, null
            );

            assertThat(bp.getCanPlaceHolds()).isFalse();
            assertThat(bp.getCanRecallBooks()).isFalse();
        }

        @Test
        @DisplayName("should allow zero maxRenewals and maxHolds")
        void shouldAllowZeroNonNegativeFields() {
            BorrowingPrivilege bp = new BorrowingPrivilege(
                5, 14, 0,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, 0, false
            );

            assertThat(bp.getMaxRenewals()).isEqualTo(0);
            assertThat(bp.getMaxHolds()).isEqualTo(0);
        }

        @ParameterizedTest(name = "should reject zero/negative {0}")
        @CsvSource({
            "maxLoans, 0, Max loans",
            "maxLoans, -1, Max loans",
            "loanPeriodDays, 0, Loan period days",
            "loanPeriodDays, -5, Loan period days",
            "dailyFineRate, 0, Daily fine rate",
            "maxFineAmount, -1, Max fine amount",
        })
        void shouldRejectZeroOrNegative(String label, int value, String expectedField) {
            BigDecimal bdValue = BigDecimal.valueOf(value);
            assertThatThrownBy(() -> new BorrowingPrivilege(
                label.equals("maxLoans") ? value : 5,
                label.equals("loanPeriodDays") ? value : 14,
                1,
                label.equals("dailyFineRate") ? bdValue : new BigDecimal("0.50"),
                label.equals("maxFineAmount") ? bdValue : new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedField);
        }

        @Test
        @DisplayName("should reject negative maxRenewals")
        void shouldRejectNegativeMaxRenewals() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, -1,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max renewals");
        }

        @Test
        @DisplayName("should reject negative maxHolds")
        void shouldRejectNegativeMaxHolds() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, 1,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, -2, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max holds");
        }

        @Test
        @DisplayName("should reject null maxLoans")
        void shouldRejectNullMaxLoans() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                null, 14, 1,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max loans");
        }

        @Test
        @DisplayName("should reject null dailyFineRate")
        void shouldRejectNullDailyFineRate() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, 1,
                null, new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily fine rate");
        }

        @Test
        @DisplayName("should reject null maxFineAmount")
        void shouldRejectNullMaxFineAmount() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, 1,
                new BigDecimal("0.50"), null,
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max fine amount");
        }

        @Test
        @DisplayName("should reject null loanPeriodDays")
        void shouldRejectNullLoanPeriodDays() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, null, 1,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan period days");
        }

        @Test
        @DisplayName("should reject null maxRenewals")
        void shouldRejectNullMaxRenewals() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, null,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, 3, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max renewals");
        }

        @Test
        @DisplayName("should reject null maxHolds")
        void shouldRejectNullMaxHolds() {
            assertThatThrownBy(() -> new BorrowingPrivilege(
                5, 14, 1,
                new BigDecimal("0.50"), new BigDecimal("10.00"),
                true, null, false
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max holds");
        }
    }

    // ---------------------------------------------------------------------------
    // hasRenewalQuota
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("hasRenewalQuota")
    class HasRenewalQuota {

        @Test
        @DisplayName("should return true when under renewal limit")
        void underLimit() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT); // maxRenewals = 1

            assertThat(bp.hasRenewalQuota(0)).isTrue();
        }

        @Test
        @DisplayName("should return false when at renewal limit")
        void atLimit() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT); // maxRenewals = 1

            assertThat(bp.hasRenewalQuota(1)).isFalse();
        }

        @Test
        @DisplayName("should return false when over renewal limit")
        void overLimit() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT); // maxRenewals = 1

            assertThat(bp.hasRenewalQuota(2)).isFalse();
        }

        @Test
        @DisplayName("ALUMNI with zero maxRenewals should always return false")
        void alumniNoRenewals() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.ALUMNI); // maxRenewals = 0

            assertThat(bp.hasRenewalQuota(0)).isFalse();
        }
    }

    // ---------------------------------------------------------------------------
    // hasHoldQuota
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("hasHoldQuota")
    class HasHoldQuota {

        @Test
        @DisplayName("should return true when can place holds and under limit")
        void canPlaceAndUnderLimit() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT); // canPlaceHolds=true, maxHolds=3

            assertThat(bp.hasHoldQuota(0)).isTrue();
            assertThat(bp.hasHoldQuota(2)).isTrue();
        }

        @Test
        @DisplayName("should return false when at hold limit")
        void atLimit() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.STUDENT); // maxHolds=3

            assertThat(bp.hasHoldQuota(3)).isFalse();
        }

        @Test
        @DisplayName("should return false when cannot place holds")
        void cannotPlaceHolds() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.COMMUNITY); // canPlaceHolds=false

            assertThat(bp.hasHoldQuota(0)).isFalse();
        }

        @Test
        @DisplayName("COMMUNITY cannot place holds regardless of count")
        void communityNoHolds() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.COMMUNITY); // canPlaceHolds=false, maxHolds=0

            assertThat(bp.hasHoldQuota(0)).isFalse();
            assertThat(bp.hasHoldQuota(1)).isFalse();
        }

        @Test
        @DisplayName("FACULTY should have high hold quota")
        void facultyHighQuota() {
            BorrowingPrivilege bp = new BorrowingPrivilege(PatronType.FACULTY); // maxHolds=10

            assertThat(bp.hasHoldQuota(9)).isTrue();
            assertThat(bp.hasHoldQuota(10)).isFalse();
        }
    }
}
