package com.library.catalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ISBNTest {

    @Nested
    @DisplayName("ISBN-13 Tests")
    class ISBN13Tests {

        @Test
        @DisplayName("should accept valid ISBN-13")
        void shouldAcceptValidISBN13() {
            assertDoesNotThrow(() -> new ISBN("978-0-13-468599-1"));
        }

        @Test
        @DisplayName("should accept ISBN-13 without hyphens")
        void shouldAcceptISBN13WithoutHyphens() {
            assertDoesNotThrow(() -> new ISBN("9780134685991"));
        }

        @Test
        @DisplayName("should accept ISBN-13 with spaces")
        void shouldAcceptISBN13WithSpaces() {
            assertDoesNotThrow(() -> new ISBN("978 0 13 468599 1"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "978-3-16-148410-0",
            "978-1-56619-909-4",
            "978-0-306-40615-7"
        })
        @DisplayName("should accept various valid ISBN-13 values")
        void shouldAcceptVariousValidISBN13(String value) {
            assertDoesNotThrow(() -> new ISBN(value));
        }
    }

    @Nested
    @DisplayName("ISBN-10 Tests")
    class ISBN10Tests {

        @Test
        @DisplayName("should accept valid ISBN-10")
        void shouldAcceptValidISBN10() {
            assertDoesNotThrow(() -> new ISBN("0-306-40615-2"));
        }

        @Test
        @DisplayName("should accept ISBN-10 with X check digit")
        void shouldAcceptISBN10WithX() {
            assertDoesNotThrow(() -> new ISBN("0-8044-2957-X"));
        }

        @Test
        @DisplayName("should accept ISBN-10 with lowercase x")
        void shouldAcceptISBN10WithLowercaseX() {
            assertDoesNotThrow(() -> new ISBN("0-8044-2957-x"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "0471958697",
            "0-13-149505-4"
        })
        @DisplayName("should accept various valid ISBN-10 values")
        void shouldAcceptVariousValidISBN10(String value) {
            assertDoesNotThrow(() -> new ISBN(value));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should reject null ISBN")
        void shouldRejectNullISBN() {
            assertThrows(NullPointerException.class, () -> new ISBN(null));
        }

        @Test
        @DisplayName("should reject empty ISBN")
        void shouldRejectEmptyISBN() {
            assertThrows(IllegalArgumentException.class, () -> new ISBN(""));
        }

        @Test
        @DisplayName("should reject ISBN with invalid checksum")
        void shouldRejectInvalidChecksum() {
            assertThrows(IllegalArgumentException.class, () -> new ISBN("978-0-13-468599-2"));
        }

        @Test
        @DisplayName("should reject too short value")
        void shouldRejectTooShort() {
            assertThrows(IllegalArgumentException.class, () -> new ISBN("123"));
        }

        @Test
        @DisplayName("should reject value with letters in middle")
        void shouldRejectLettersInMiddle() {
            assertThrows(IllegalArgumentException.class, () -> new ISBN("978-0-AB-468599-1"));
        }
    }

    @Nested
    @DisplayName("Formatting Tests")
    class FormattingTests {

        @Test
        @DisplayName("should format ISBN-13 with hyphens")
        void shouldFormatISBN13() {
            ISBN isbn = new ISBN("9780134685991");
            String formatted = isbn.getFormattedValue();
            assertTrue(formatted.contains("-"));
        }

        @Test
        @DisplayName("should return cleaned value")
        void shouldReturnCleanedValue() {
            ISBN isbn = new ISBN("978-0-13-468599-1");
            assertEquals("9780134685991", isbn.getValue());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("should be equal for same ISBN value")
        void shouldBeEqualForSameValue() {
            ISBN isbn1 = new ISBN("978-0-13-468599-1");
            ISBN isbn2 = new ISBN("9780134685991");
            assertEquals(isbn1, isbn2);
            assertEquals(isbn1.hashCode(), isbn2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different ISBN")
        void shouldNotBeEqualForDifferentValue() {
            ISBN isbn1 = new ISBN("978-0-13-468599-1");
            ISBN isbn2 = new ISBN("978-3-16-148410-0");
            assertNotEquals(isbn1, isbn2);
        }
    }

    @Nested
    @DisplayName("Type Detection Tests")
    class TypeDetectionTests {

        @Test
        @DisplayName("should detect ISBN-13")
        void shouldDetectISBN13() {
            ISBN isbn = new ISBN("978-0-13-468599-1");
            assertTrue(isbn.isISBN13());
            assertFalse(isbn.isISBN10());
        }

        @Test
        @DisplayName("should detect ISBN-10")
        void shouldDetectISBN10() {
            ISBN isbn = new ISBN("0-306-40615-2");
            assertTrue(isbn.isISBN10());
            assertFalse(isbn.isISBN13());
        }
    }
}
