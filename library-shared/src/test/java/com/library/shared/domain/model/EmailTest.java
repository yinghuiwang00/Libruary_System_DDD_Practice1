package com.library.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EmailTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void shouldCreateValidEmail() {
            Email email = Email.of("user@example.com");
            assertThat(email.getValue()).isEqualTo("user@example.com");
        }

        @Test
        void shouldNormalizeToLowercase() {
            Email email = Email.of("User@Example.COM");
            assertThat(email.getValue()).isEqualTo("user@example.com");
        }

        @Test
        void shouldTrimWhitespace() {
            Email email = Email.of("  user@example.com  ");
            assertThat(email.getValue()).isEqualTo("user@example.com");
        }

        @Test
        void shouldRejectNull() {
            assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectInvalidEmail() {
            assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }

        @Test
        void shouldRejectEmptyString() {
            assertThatThrownBy(() -> Email.of(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAcceptComplexEmail() {
            Email email = Email.of("user.name+tag@sub.domain.co.uk");
            assertThat(email.getValue()).isEqualTo("user.name+tag@sub.domain.co.uk");
        }
    }

    @Nested
    @DisplayName("Domain and LocalPart")
    class DomainAndLocalPart {

        @Test
        void shouldExtractDomain() {
            Email email = Email.of("user@example.com");
            assertThat(email.getDomain()).isEqualTo("example.com");
        }

        @Test
        void shouldExtractLocalPart() {
            Email email = Email.of("user@example.com");
            assertThat(email.getLocalPart()).isEqualTo("user");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void shouldBeEqualWhenSameNormalizedValue() {
            Email e1 = Email.of("User@Example.COM");
            Email e2 = Email.of("user@example.com");
            assertThat(e1).isEqualTo(e2);
        }

        @Test
        void shouldNotBeEqualWhenDifferentEmail() {
            assertThat(Email.of("a@example.com")).isNotEqualTo(Email.of("b@example.com"));
        }

        @Test
        void shouldHaveSameHashCodeWhenEqual() {
            Email e1 = Email.of("User@Example.COM");
            Email e2 = Email.of("user@example.com");
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        void shouldReturnValue() {
            assertThat(Email.of("user@example.com").toString()).isEqualTo("user@example.com");
        }
    }
}
