package com.library.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregateIdTest {

    @Nested
    @DisplayName("BookId Tests")
    class BookIdTests {

        @Test
        @DisplayName("should generate unique BookId")
        void shouldGenerateUniqueBookId() {
            BookId id1 = BookId.generate();
            BookId id2 = BookId.generate();
            assertNotNull(id1.getValue());
            assertNotNull(id2.getValue());
            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("should create BookId from existing value")
        void shouldCreateFromExistingValue() {
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            BookId id = BookId.of(uuid);
            assertEquals(uuid, id.getValue());
        }

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            BookId id1 = BookId.of(uuid);
            BookId id2 = BookId.of(uuid);
            BookId id3 = BookId.of("different-uuid");

            assertEquals(id1, id2);
            assertEquals(id1.hashCode(), id2.hashCode());
            assertNotEquals(id1, id3);
        }

        @Test
        @DisplayName("should throw on null value")
        void shouldThrowOnNullValue() {
            assertThrows(NullPointerException.class, () -> BookId.of(null));
        }

        @Test
        @DisplayName("should return value as toString")
        void shouldReturnValueAsToString() {
            BookId id = BookId.of("test-uuid");
            assertEquals("test-uuid", id.toString());
        }

        @Test
        @DisplayName("should not equal different ID types")
        void shouldNotEqualDifferentIdTypes() {
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            BookId bookId = BookId.of(uuid);
            AuthorId authorId = AuthorId.of(uuid);
            assertNotEquals(bookId, authorId);
        }
    }

    @Nested
    @DisplayName("AuthorId Tests")
    class AuthorIdTests {

        @Test
        @DisplayName("should generate unique AuthorId")
        void shouldGenerateUnique() {
            AuthorId id1 = AuthorId.generate();
            AuthorId id2 = AuthorId.generate();
            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("should create from string")
        void shouldCreateFromString() {
            AuthorId id = AuthorId.of("author-123");
            assertEquals("author-123", id.getValue());
        }
    }

    @Nested
    @DisplayName("PublisherId Tests")
    class PublisherIdTests {

        @Test
        @DisplayName("should generate unique PublisherId")
        void shouldGenerateUnique() {
            PublisherId id1 = PublisherId.generate();
            PublisherId id2 = PublisherId.generate();
            assertNotEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("CategoryId Tests")
    class CategoryIdTests {

        @Test
        @DisplayName("should generate unique CategoryId")
        void shouldGenerateUnique() {
            CategoryId id1 = CategoryId.generate();
            CategoryId id2 = CategoryId.generate();
            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("should support compareTo")
        void shouldSupportCompareTo() {
            CategoryId id1 = CategoryId.of("aaa");
            CategoryId id2 = CategoryId.of("bbb");
            assertTrue(id1.compareTo(id2) < 0);
        }
    }
}
