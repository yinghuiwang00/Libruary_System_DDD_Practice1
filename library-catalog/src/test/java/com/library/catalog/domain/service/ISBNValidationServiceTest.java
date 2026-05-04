package com.library.catalog.domain.service;

import com.library.catalog.domain.model.ISBN;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ISBNValidationServiceTest {

    private final ISBNValidationService service = new ISBNValidationService();

    @Nested
    @DisplayName("isValidISBN Tests")
    class IsValidISBNTests {

        @Test
        @DisplayName("should return true for valid ISBN-13")
        void shouldReturnTrueForValidISBN13() {
            assertTrue(service.isValidISBN("978-0-13-468599-1"));
        }

        @Test
        @DisplayName("should return true for valid ISBN-10")
        void shouldReturnTrueForValidISBN10() {
            assertTrue(service.isValidISBN("0-306-40615-2"));
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(service.isValidISBN(null));
        }

        @Test
        @DisplayName("should return false for blank string")
        void shouldReturnFalseForBlank() {
            assertFalse(service.isValidISBN("   "));
        }

        @Test
        @DisplayName("should return false for invalid checksum")
        void shouldReturnFalseForInvalidChecksum() {
            assertFalse(service.isValidISBN("978-0-13-468599-2"));
        }

        @Test
        @DisplayName("should return false for wrong length")
        void shouldReturnFalseForWrongLength() {
            assertFalse(service.isValidISBN("12345"));
        }
    }

    @Nested
    @DisplayName("convertToISBN13 Tests")
    class ConvertToISBN13Tests {

        @Test
        @DisplayName("should convert valid ISBN-10 to ISBN-13")
        void shouldConvertISBN10ToISBN13() {
            ISBN isbn10 = new ISBN("0-306-40615-2");
            ISBN isbn13 = service.convertToISBN13(isbn10);

            assertTrue(isbn13.isISBN13());
            assertTrue(isbn13.getValue().startsWith("978"));
            assertEquals(13, isbn13.getValue().length());
        }

        @Test
        @DisplayName("should produce a valid ISBN-13 after conversion")
        void shouldProduceValidISBN13() {
            ISBN isbn10 = new ISBN("0471958697");
            ISBN isbn13 = service.convertToISBN13(isbn10);

            // The converted ISBN-13 should pass validation (no exception)
            assertDoesNotThrow(() -> new ISBN(isbn13.getValue()));
        }

        @Test
        @DisplayName("should throw when converting non-ISBN-10")
        void shouldThrowWhenConvertingNonISBN10() {
            ISBN isbn13 = new ISBN("978-0-13-468599-1");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.convertToISBN13(isbn13));
            assertTrue(ex.getMessage().contains("Cannot convert non-ISBN-10"));
        }
    }

    @Nested
    @DisplayName("lookupExternalRegistry Tests")
    class LookupExternalRegistryTests {

        @Test
        @DisplayName("should return true for stubbed lookup")
        void shouldReturnTrueForStubbedLookup() {
            ISBN isbn = new ISBN("978-0-13-468599-1");
            assertTrue(service.lookupExternalRegistry(isbn));
        }
    }
}
