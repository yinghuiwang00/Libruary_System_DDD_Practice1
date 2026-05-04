package com.library.catalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublisherTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create publisher with valid data")
        void shouldCreatePublisher() {
            Publisher p = Publisher.create("O'Reilly", "Tech publisher",
                "123 Main St", "555-1234", "info@oreilly.com", "oreilly.com");
            assertNotNull(p.getId());
            assertEquals("O'Reilly", p.getName());
            assertEquals("Tech publisher", p.getDescription());
            assertEquals("123 Main St", p.getAddress());
            assertEquals("555-1234", p.getPhone());
            assertEquals("info@oreilly.com", p.getEmail());
            assertEquals("oreilly.com", p.getWebsite());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                Publisher.create("  ", null, null, null, null, null));
        }

        @Test
        @DisplayName("should reject name over 200 chars")
        void shouldRejectLongName() {
            assertThrows(IllegalArgumentException.class, () ->
                Publisher.create("a".repeat(201), null, null, null, null, null));
        }

        @Test
        @DisplayName("should create with minimal data")
        void shouldCreateMinimal() {
            Publisher p = Publisher.create("Publisher", null, null, null, null, null);
            assertEquals("Publisher", p.getName());
            assertNull(p.getDescription());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("should update info")
        void shouldUpdateInfo() {
            Publisher p = Publisher.create("Old", null, null, null, null, null);
            p.updateInfo("New", "desc", "addr", "phone", "email", "web");
            assertEquals("New", p.getName());
            assertEquals("desc", p.getDescription());
        }

        @Test
        @DisplayName("should preserve values when null passed")
        void shouldPreserveOnNull() {
            Publisher p = Publisher.create("Name", "desc", "addr", null, null, null);
            p.updateInfo(null, null, null, null, null, null);
            assertEquals("Name", p.getName());
            assertEquals("desc", p.getDescription());
        }
    }
}
