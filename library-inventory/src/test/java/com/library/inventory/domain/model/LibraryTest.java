package com.library.inventory.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LibraryTest {

    @Test
    void shouldCreateLibrary() {
        Library library = Library.create("LIB01", "Central Library");

        assertThat(library.getId()).isNotNull();
        assertThat(library.getCode()).isEqualTo("LIB01");
        assertThat(library.getName()).isEqualTo("Central Library");
        assertThat(library.isActive()).isTrue();
    }

    @Test
    void shouldUppercaseCode() {
        Library library = Library.create("lib01", "Test Library");
        assertThat(library.getCode()).isEqualTo("LIB01");
    }

    @Test
    void shouldRejectBlankCode() {
        assertThatThrownBy(() -> Library.create("", "Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Library code must not be blank");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> Library.create("LIB01", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Library name must not be blank");
    }

    @Test
    void shouldRejectLongCode() {
        assertThatThrownBy(() -> Library.create("A".repeat(21), "Test"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLongName() {
        assertThatThrownBy(() -> Library.create("LIB01", "A".repeat(201)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUpdateContactInfo() {
        Library library = Library.create("LIB01", "Central Library");
        library.updateContactInfo("123 Main St", "Springfield", "IL", "62701", "555-1234", "lib@test.com");

        assertThat(library.getAddress()).isEqualTo("123 Main St");
        assertThat(library.getCity()).isEqualTo("Springfield");
        assertThat(library.getProvince()).isEqualTo("IL");
        assertThat(library.getPostalCode()).isEqualTo("62701");
        assertThat(library.getPhone()).isEqualTo("555-1234");
        assertThat(library.getEmail()).isEqualTo("lib@test.com");
    }

    @Test
    void shouldUpdateOperatingInfo() {
        Library library = Library.create("LIB01", "Central Library");
        library.updateOperatingInfo("9am-5pm", 3);

        assertThat(library.getOpeningHours()).isEqualTo("9am-5pm");
        assertThat(library.getTotalFloors()).isEqualTo(3);
    }

    @Test
    void shouldActivateAndDeactivate() {
        Library library = Library.create("LIB01", "Test");
        assertThat(library.isActive()).isTrue();

        library.deactivate();
        assertThat(library.isActive()).isFalse();

        library.activate();
        assertThat(library.isActive()).isTrue();
    }

    @Test
    void shouldTrimName() {
        Library library = Library.create("LIB01", "  Central Library  ");
        assertThat(library.getName()).isEqualTo("Central Library");
    }
}
