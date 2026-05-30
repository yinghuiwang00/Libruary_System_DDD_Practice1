package com.library.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AddressTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void shouldCreateWithStreetCityPostalCode() {
            Address addr = Address.of("123 Main St", "Beijing", "100000");
            assertThat(addr.getStreet()).isEqualTo("123 Main St");
            assertThat(addr.getCity()).isEqualTo("Beijing");
            assertThat(addr.getPostalCode()).isEqualTo("100000");
            assertThat(addr.getCountry()).isEqualTo("China");
        }

        @Test
        void shouldCreateWithAllFields() {
            Address addr = Address.of("123 Main St", "Beijing", "100000", "Beijing", "China");
            assertThat(addr.getState()).isEqualTo("Beijing");
            assertThat(addr.getCountry()).isEqualTo("China");
        }

        @Test
        void shouldRejectNullCity() {
            assertThatThrownBy(() -> new Address("street", null, "12345"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("City must not be null");
        }

        @Test
        void shouldTrimFields() {
            Address addr = Address.of("  123 Main St  ", "  Beijing  ", "  100000  ");
            assertThat(addr.getStreet()).isEqualTo("123 Main St");
            assertThat(addr.getCity()).isEqualTo("Beijing");
            assertThat(addr.getPostalCode()).isEqualTo("100000");
        }

        @Test
        void shouldAllowNullOptionalFields() {
            Address addr = Address.of(null, "Beijing", null);
            assertThat(addr.getStreet()).isNull();
            assertThat(addr.getPostalCode()).isNull();
        }
    }

    @Nested
    @DisplayName("FullAddress")
    class FullAddress {

        @Test
        void shouldBuildFullAddress() {
            Address addr = Address.of("123 Main St", "Beijing", "100000", "Beijing", "China");
            assertThat(addr.getFullAddress()).isEqualTo("123 Main St, Beijing, Beijing 100000, China");
        }

        @Test
        void shouldOmitNullFields() {
            Address addr = Address.of(null, "Beijing", null);
            assertThat(addr.getFullAddress()).isEqualTo("Beijing, China");
        }

        @Test
        void shouldIncludePostalCode() {
            Address addr = Address.of("Main St", "Beijing", "100000");
            assertThat(addr.getFullAddress()).contains("100000");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void shouldBeEqualWhenSameFields() {
            Address a1 = Address.of("123 Main St", "Beijing", "100000");
            Address a2 = Address.of("123 Main St", "Beijing", "100000");
            assertThat(a1).isEqualTo(a2);
        }

        @Test
        void shouldNotBeEqualWhenDifferentCity() {
            Address a1 = Address.of("123 Main St", "Beijing", "100000");
            Address a2 = Address.of("123 Main St", "Shanghai", "200000");
            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        void shouldHaveSameHashCodeWhenEqual() {
            Address a1 = Address.of("123 Main St", "Beijing", "100000");
            Address a2 = Address.of("123 Main St", "Beijing", "100000");
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        void shouldReturnFullAddress() {
            Address addr = Address.of("123 Main St", "Beijing", "100000");
            assertThat(addr.toString()).isEqualTo(addr.getFullAddress());
        }
    }
}
