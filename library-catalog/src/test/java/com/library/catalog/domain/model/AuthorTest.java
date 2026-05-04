package com.library.catalog.domain.model;

import com.library.shared.domain.model.AuthorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AuthorTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create author with valid data")
        void shouldCreateAuthor() {
            Author author = Author.create("J.K. Rowling", "British author", 
                LocalDate.of(1965, 7, 31), null, "British");
            assertNotNull(author.getId());
            assertEquals("J.K. Rowling", author.getName());
            assertEquals("British author", author.getBiography());
            assertEquals(LocalDate.of(1965, 7, 31), author.getBirthDate());
            assertNull(author.getDeathDate());
            assertEquals("British", author.getNationality());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                Author.create("", "bio", null, null, null));
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThrows(IllegalArgumentException.class, () ->
                Author.create(null, "bio", null, null, null));
        }

        @Test
        @DisplayName("should reject name exceeding 200 chars")
        void shouldRejectLongName() {
            String longName = "a".repeat(201);
            assertThrows(IllegalArgumentException.class, () ->
                Author.create(longName, "bio", null, null, null));
        }

        @Test
        @DisplayName("should accept name at 200 chars")
        void shouldAcceptNameAt200Chars() {
            String name = "a".repeat(200);
            Author author = Author.create(name, null, null, null, null);
            assertEquals(name, author.getName());
        }

        @Test
        @DisplayName("should reject death date before birth date")
        void shouldRejectDeathBeforeBirth() {
            assertThrows(IllegalArgumentException.class, () ->
                Author.create("Author", null,
                    LocalDate.of(2000, 1, 1),
                    LocalDate.of(1999, 1, 1), null));
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("should update biography")
        void shouldUpdateBiography() {
            Author author = Author.create("Author", "old bio", null, null, null);
            author.updateBiography("new bio");
            assertEquals("new bio", author.getBiography());
        }

        @Test
        @DisplayName("should update personal info")
        void shouldUpdatePersonalInfo() {
            Author author = Author.create("Old Name", null, null, null, null);
            author.updatePersonalInfo("New Name", "American",
                LocalDate.of(1990, 5, 15), null);
            assertEquals("New Name", author.getName());
            assertEquals("American", author.getNationality());
            assertEquals(LocalDate.of(1990, 5, 15), author.getBirthDate());
        }

        @Test
        @DisplayName("should preserve existing values when null passed")
        void shouldPreserveExistingOnNull() {
            Author author = Author.create("Name", null, 
                LocalDate.of(1990, 1, 1), null, "French");
            author.updatePersonalInfo(null, null, null, null);
            assertEquals("Name", author.getName());
            assertEquals("French", author.getNationality());
            assertEquals(LocalDate.of(1990, 1, 1), author.getBirthDate());
        }
    }
}
