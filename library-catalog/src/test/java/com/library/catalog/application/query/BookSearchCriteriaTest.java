package com.library.catalog.application.query;

import com.library.catalog.domain.model.enums.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookSearchCriteriaTest {

    @Nested
    @DisplayName("hasAnyFilter Tests")
    class HasAnyFilterTests {

        @Test
        @DisplayName("should return false when all fields are null")
        void shouldReturnFalseWhenAllFieldsNull() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null);
            assertFalse(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return false when all fields are blank strings")
        void shouldReturnFalseWhenAllFieldsBlank() {
            BookSearchCriteria criteria = new BookSearchCriteria("  ", "  ", null, "  ", "  ", "  ");
            assertFalse(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when title is set")
        void shouldReturnTrueWhenTitleSet() {
            BookSearchCriteria criteria = new BookSearchCriteria("Java", null, null, null, null, null);
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when authorName is set")
        void shouldReturnTrueWhenAuthorNameSet() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, "Smith", null, null, null, null);
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when status is set")
        void shouldReturnTrueWhenStatusSet() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, BookStatus.PUBLISHED, null, null, null);
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when publisherId is set")
        void shouldReturnTrueWhenPublisherIdSet() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, "pub-123", null, null);
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when categoryId is set")
        void shouldReturnTrueWhenCategoryIdSet() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, "cat-456", null);
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when language is set")
        void shouldReturnTrueWhenLanguageSet() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, "en");
            assertTrue(criteria.hasAnyFilter());
        }

        @Test
        @DisplayName("should return true when multiple fields are set")
        void shouldReturnTrueWhenMultipleFieldsSet() {
            BookSearchCriteria criteria = new BookSearchCriteria("Java", "Smith", BookStatus.DRAFT, null, null, null);
            assertTrue(criteria.hasAnyFilter());
        }
    }
}
