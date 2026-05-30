package com.library.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PhoneNumberTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void shouldCreateValidPhone() {
            PhoneNumber phone = PhoneNumber.of("13800138000");
            assertThat(phone.getValue()).isEqualTo("13800138000");
        }

        @Test
        void shouldCreateInternationalPhone() {
            PhoneNumber phone = PhoneNumber.of("+8613800138000");
            assertThat(phone.getValue()).isEqualTo("+8613800138000");
        }

        @Test
        void shouldStripFormatting() {
            PhoneNumber phone = PhoneNumber.of("138-0013-8000");
            assertThat(phone.getValue()).isEqualTo("13800138000");
        }

        @Test
        void shouldStripParentheses() {
            PhoneNumber phone = PhoneNumber.of("(010) 12345678");
            assertThat(phone.getValue()).isEqualTo("01012345678");
        }

        @Test
        void shouldRejectNull() {
            assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectTooShort() {
            assertThatThrownBy(() -> PhoneNumber.of("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number");
        }

        @Test
        void shouldRejectTooLong() {
            assertThatThrownBy(() -> PhoneNumber.of("1234567890123456"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectLetters() {
            assertThatThrownBy(() -> PhoneNumber.of("abc1234567"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("International check")
    class International {

        @Test
        void shouldBeInternationalWhenStartsWithPlus() {
            assertThat(PhoneNumber.of("+8613800138000").isInternational()).isTrue();
        }

        @Test
        void shouldNotBeInternationalWhenNoPlus() {
            assertThat(PhoneNumber.of("13800138000").isInternational()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void shouldBeEqualWhenSameValue() {
            assertThat(PhoneNumber.of("13800138000")).isEqualTo(PhoneNumber.of("13800138000"));
        }

        @Test
        void shouldNotBeEqualWhenDifferentValue() {
            assertThat(PhoneNumber.of("13800138000")).isNotEqualTo(PhoneNumber.of("13900139000"));
        }

        @Test
        void shouldHaveSameHashCodeWhenEqual() {
            PhoneNumber p1 = PhoneNumber.of("13800138000");
            PhoneNumber p2 = PhoneNumber.of("13800138000");
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        void shouldReturnValue() {
            assertThat(PhoneNumber.of("13800138000").toString()).isEqualTo("13800138000");
        }
    }
}
