package com.library.catalog.domain.model;

import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BookAuthor value object tests")
class BookAuthorTest {

    // ---------------------------------------------------------------
    // Static factory: BookAuthor.of()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookAuthor.of() factory method")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create BookAuthor with all fields")
        void shouldCreateBookAuthorWithAllFields() {
            BookId bookId = BookId.generate();
            AuthorId authorId = AuthorId.generate();

            BookAuthor bookAuthor = BookAuthor.of(bookId, authorId, "Eric Evans", AuthorRole.AUTHOR, 0);

            assertThat(bookAuthor.getBookId()).isEqualTo(bookId.getValue());
            assertThat(bookAuthor.getAuthorId()).isEqualTo(authorId.getValue());
            assertThat(bookAuthor.getAuthorName()).isEqualTo("Eric Evans");
            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.AUTHOR);
            assertThat(bookAuthor.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create BookAuthor with CO_AUTHOR role")
        void shouldCreateWithCoAuthorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Jane Doe", AuthorRole.CO_AUTHOR, 1
            );

            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.CO_AUTHOR);
            assertThat(bookAuthor.getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create BookAuthor with EDITOR role")
        void shouldCreateWithEditorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Editor Person", AuthorRole.EDITOR, 2
            );

            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.EDITOR);
        }

        @Test
        @DisplayName("should create BookAuthor with TRANSLATOR role")
        void shouldCreateWithTranslatorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Translator Name", AuthorRole.TRANSLATOR, 3
            );

            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.TRANSLATOR);
        }

        @Test
        @DisplayName("should create BookAuthor with CONTRIBUTOR role")
        void shouldCreateWithContributorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Contributor Name", AuthorRole.CONTRIBUTOR, 4
            );

            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.CONTRIBUTOR);
        }

        @Test
        @DisplayName("should extract string values from typed IDs")
        void shouldExtractStringValuesFromTypedIDs() {
            String bookIdStr = "book-abc-123";
            String authorIdStr = "author-xyz-456";

            BookAuthor bookAuthor = BookAuthor.of(
                BookId.of(bookIdStr), AuthorId.of(authorIdStr), "Name", AuthorRole.AUTHOR, 0
            );

            assertThat(bookAuthor.getBookId()).isEqualTo(bookIdStr);
            assertThat(bookAuthor.getAuthorId()).isEqualTo(authorIdStr);
        }

        @Test
        @DisplayName("should throw NullPointerException when bookId is null")
        void shouldThrowWhenBookIdIsNull() {
            assertThatThrownBy(() -> BookAuthor.of(null, AuthorId.generate(), "Name", AuthorRole.AUTHOR, 0))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw NullPointerException when authorId is null")
        void shouldThrowWhenAuthorIdIsNull() {
            assertThatThrownBy(() -> BookAuthor.of(BookId.generate(), null, "Name", AuthorRole.AUTHOR, 0))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw NullPointerException when authorName is null")
        void shouldThrowWhenAuthorNameIsNull() {
            assertThatThrownBy(() -> BookAuthor.of(BookId.generate(), AuthorId.generate(), null, AuthorRole.AUTHOR, 0))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw NullPointerException when role is null")
        void shouldThrowWhenRoleIsNull() {
            assertThatThrownBy(() -> BookAuthor.of(BookId.generate(), AuthorId.generate(), "Name", null, 0))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // ---------------------------------------------------------------
    // Getter methods
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Getter methods")
    class GetterTests {

        @Test
        @DisplayName("getBookId should return the book ID string")
        void shouldReturnBookId() {
            BookId bookId = BookId.generate();
            BookAuthor bookAuthor = BookAuthor.of(bookId, AuthorId.generate(), "Author", AuthorRole.AUTHOR, 0);

            assertThat(bookAuthor.getBookId()).isEqualTo(bookId.getValue());
        }

        @Test
        @DisplayName("getAuthorId should return the author ID string")
        void shouldReturnAuthorId() {
            AuthorId authorId = AuthorId.generate();
            BookAuthor bookAuthor = BookAuthor.of(BookId.generate(), authorId, "Author", AuthorRole.AUTHOR, 0);

            assertThat(bookAuthor.getAuthorId()).isEqualTo(authorId.getValue());
        }

        @Test
        @DisplayName("getAuthorName should return the author name")
        void shouldReturnAuthorName() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Robert C. Martin", AuthorRole.AUTHOR, 0
            );

            assertThat(bookAuthor.getAuthorName()).isEqualTo("Robert C. Martin");
        }

        @Test
        @DisplayName("getRole should return the author role")
        void shouldReturnRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Name", AuthorRole.EDITOR, 0
            );

            assertThat(bookAuthor.getRole()).isEqualTo(AuthorRole.EDITOR);
        }

        @Test
        @DisplayName("getSortOrder should return the sort position")
        void shouldReturnSortOrder() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Name", AuthorRole.AUTHOR, 5
            );

            assertThat(bookAuthor.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("sort order zero is valid")
        void shouldAllowSortOrderZero() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(), "Name", AuthorRole.AUTHOR, 0
            );

            assertThat(bookAuthor.getSortOrder()).isZero();
        }
    }

    // ---------------------------------------------------------------
    // BookAuthorId composite key
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookAuthorId composite key")
    class BookAuthorIdTests {

        @Test
        @DisplayName("equal IDs should be equal")
        void shouldBeEqualWhenSameValues() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId("book-1", "author-1");
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("different book IDs should not be equal")
        void shouldNotBeEqualWhenDifferentBookId() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId("book-1", "author-1");
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId("book-2", "author-1");

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("different author IDs should not be equal")
        void shouldNotBeEqualWhenDifferentAuthorId() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId("book-1", "author-1");
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId("book-1", "author-2");

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            BookAuthor.BookAuthorId id = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            BookAuthor.BookAuthorId id = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id).isNotEqualTo("book-1:author-1");
            assertThat(id).isNotEqualTo(42);
        }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            BookAuthor.BookAuthorId id = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id).isEqualTo(id);
        }

        @Test
        @DisplayName("hashCode should be consistent across calls")
        void shouldHaveConsistentHashCode() {
            BookAuthor.BookAuthorId id = new BookAuthor.BookAuthorId("book-1", "author-1");

            int hash1 = id.hashCode();
            int hash2 = id.hashCode();

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("default constructor should create instance")
        void shouldCreateWithDefaultConstructor() {
            BookAuthor.BookAuthorId id = new BookAuthor.BookAuthorId();

            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("IDs with null fields - equals with same nulls")
        void shouldHandleNullFields() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId(null, null);
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId(null, null);

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("IDs with null bookId vs non-null bookId should not be equal")
        void shouldHandleNullVsNonNullBookId() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId(null, "author-1");
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("IDs with null authorId vs non-null authorId should not be equal")
        void shouldHandleNullVsNonNullAuthorId() {
            BookAuthor.BookAuthorId id1 = new BookAuthor.BookAuthorId("book-1", null);
            BookAuthor.BookAuthorId id2 = new BookAuthor.BookAuthorId("book-1", "author-1");

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    // ---------------------------------------------------------------
    // Integration with Book aggregate
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Integration with Book aggregate")
    class BookIntegrationTests {

        @Test
        @DisplayName("BookAuthor created via Book.addAuthor should have correct fields")
        void shouldHaveCorrectFieldsWhenCreatedViaBook() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Test Book", "desc", null, 100, "zh"
            );

            book.addAuthor("author-1", "Eric Evans", AuthorRole.AUTHOR);

            assertThat(book.getAuthors()).hasSize(1);
            BookAuthor ba = book.getAuthors().get(0);
            assertThat(ba.getBookId()).isEqualTo(book.getId().getValue());
            assertThat(ba.getAuthorId()).isEqualTo("author-1");
            assertThat(ba.getAuthorName()).isEqualTo("Eric Evans");
            assertThat(ba.getRole()).isEqualTo(AuthorRole.AUTHOR);
            assertThat(ba.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("multiple authors should have incrementing sort orders")
        void shouldHaveIncrementingSortOrders() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Test Book", "desc", null, 100, "zh"
            );

            book.addAuthor("a1", "First Author", AuthorRole.AUTHOR);
            book.addAuthor("a2", "Second Author", AuthorRole.CO_AUTHOR);
            book.addAuthor("a3", "Third Author", AuthorRole.EDITOR);

            assertThat(book.getAuthors()).hasSize(3);
            assertThat(book.getAuthors().get(0).getSortOrder()).isEqualTo(0);
            assertThat(book.getAuthors().get(1).getSortOrder()).isEqualTo(1);
            assertThat(book.getAuthors().get(2).getSortOrder()).isEqualTo(2);
        }
    }
}
