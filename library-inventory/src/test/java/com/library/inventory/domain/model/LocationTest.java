package com.library.inventory.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LocationTest {

    @Test
    void shouldCreateLocationWithAllFields() {
        Location location = Location.of("LIB01", 1, "A", "01", "B", "001");

        assertThat(location.getLibraryCode()).isEqualTo("LIB01");
        assertThat(location.getFloor()).isEqualTo(1);
        assertThat(location.getZone()).isEqualTo("A");
        assertThat(location.getAisle()).isEqualTo("01");
        assertThat(location.getShelf()).isEqualTo("B");
        assertThat(location.getPosition()).isEqualTo("001");
        assertThat(location.getLocationCode()).isNotBlank();
    }

    @Test
    void shouldUppercaseLibraryCode() {
        Location location = Location.of("lib01", 1, "A", "01", "B", "001");
        assertThat(location.getLibraryCode()).isEqualTo("LIB01");
    }

    @Test
    void shouldRejectBlankLibraryCode() {
        assertThatThrownBy(() -> Location.of("", 1, "A", "01", "B", "001"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Library code must not be blank");
    }

    @Test
    void shouldRejectNullLibraryCode() {
        assertThatThrownBy(() -> Location.of(null, 1, "A", "01", "B", "001"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateSimpleLocation() {
        Location location = Location.simple("LIB01", "A123-B01-001");

        assertThat(location.getLibraryCode()).isEqualTo("LIB01");
        assertThat(location.getLocationCode()).isEqualTo("A123-B01-001");
        assertThat(location.getZone()).isEqualTo("A");
        assertThat(location.getAisle()).isEqualTo("123");
        assertThat(location.getShelf()).isEqualTo("B01");
        assertThat(location.getPosition()).isEqualTo("001");
    }

    @Test
    void shouldGenerateFullDescription() {
        Location location = Location.of("LIB01", 2, "A", "03", "B", "005");
        String desc = location.getFullDescription();

        assertThat(desc).contains("LIB01");
        assertThat(desc).contains("2");
        assertThat(desc).contains("A");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Location loc1 = Location.of("LIB01", 1, "A", "01", "B", "001");
        Location loc2 = Location.of("LIB01", 1, "A", "01", "B", "001");

        assertThat(loc1).isEqualTo(loc2);
        assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());

        Location loc3 = Location.of("LIB01", 2, "C", "03", "D", "005");
        assertThat(loc1).isNotEqualTo(loc3);
    }
}
