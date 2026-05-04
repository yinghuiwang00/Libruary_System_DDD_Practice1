package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.BookAuthor;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.model.enums.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookDTO tests")
class BookDTOTest {

    // ---------------------------------------------------------------
    // BookDTO.from(Book) - basic fields
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO.from() basic field mapping")
    class BasicFieldMappingTests {

        @Test
        @DisplayName("should map all basic fields from Book")
        void shouldMapAllBasicFields() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"),
                "Domain-Driven Design",
                "A comprehensive guide to DDD",
                LocalDate.of(2003, 9, 30),
                400,
                "zh"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.id()).isEqualTo(book.getId().getValue());
            assertThat(dto.isbn()).isEqualTo("9787111407010");
            assertThat(dto.title()).isEqualTo("Domain-Driven Design");
            assertThat(dto.description()).isEqualTo("A comprehensive guide to DDD");
            assertThat(dto.publicationDate()).isEqualTo(LocalDate.of(2003, 9, 30));
            assertThat(dto.pageCount()).isEqualTo(400);
            assertThat(dto.language()).isEqualTo("zh");
            assertThat(dto.status()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("should map status as string name")
        void shouldMapStatusAsString() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.status()).isEqualTo(BookStatus.DRAFT.name());
        }

        @Test
        @DisplayName("should map nullable fields as null when not set")
        void shouldMapNullFieldsAsNull() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", null, null, null, null
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.description()).isNull();
            assertThat(dto.publicationDate()).isNull();
            assertThat(dto.pageCount()).isNull();
            assertThat(dto.language()).isNull();
            assertThat(dto.publisherId()).isNull();
        }

        @Test
        @DisplayName("should map id from BookId value")
        void shouldMapBookId() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.id()).isEqualTo(book.getId().getValue());
        }

        @Test
        @DisplayName("should map ISBN cleaned value")
        void shouldMapISBNCleanedValue() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.isbn()).isEqualTo("9787111407010");
        }
    }

    // ---------------------------------------------------------------
    // BookDTO.from(Book) - authors
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO.from() author mapping")
    class AuthorMappingTests {

        @Test
        @DisplayName("should map empty authors list")
        void shouldMapEmptyAuthorsList() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.authors()).isEmpty();
        }

        @Test
        @DisplayName("should map single author")
        void shouldMapSingleAuthor() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.addAuthor("author-1", "Eric Evans", AuthorRole.AUTHOR);

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.authors()).hasSize(1);
            AuthorDTO authorDTO = dto.authors().get(0);
            assertThat(authorDTO.id()).isEqualTo("author-1");
            assertThat(authorDTO.name()).isEqualTo("Eric Evans");
            assertThat(authorDTO.role()).isEqualTo("AUTHOR");
        }

        @Test
        @DisplayName("should map multiple authors in order")
        void shouldMapMultipleAuthorsInOrder() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.addAuthor("a1", "First Author", AuthorRole.AUTHOR);
            book.addAuthor("a2", "Second Author", AuthorRole.CO_AUTHOR);
            book.addAuthor("a3", "Translator Person", AuthorRole.TRANSLATOR);

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.authors()).hasSize(3);
            assertThat(dto.authors().get(0).name()).isEqualTo("First Author");
            assertThat(dto.authors().get(0).role()).isEqualTo("AUTHOR");
            assertThat(dto.authors().get(1).name()).isEqualTo("Second Author");
            assertThat(dto.authors().get(1).role()).isEqualTo("CO_AUTHOR");
            assertThat(dto.authors().get(2).name()).isEqualTo("Translator Person");
            assertThat(dto.authors().get(2).role()).isEqualTo("TRANSLATOR");
        }

        @Test
        @DisplayName("should map AuthorDTO from BookAuthor with null biography fields")
        void shouldMapAuthorDTOWithNullBiographyFields() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.addAuthor("a1", "Author Name", AuthorRole.EDITOR);

            BookDTO dto = BookDTO.from(book);

            AuthorDTO authorDTO = dto.authors().get(0);
            assertThat(authorDTO.biography()).isNull();
            assertThat(authorDTO.birthDate()).isNull();
            assertThat(authorDTO.deathDate()).isNull();
            assertThat(authorDTO.nationality()).isNull();
            assertThat(authorDTO.version()).isNull();
            assertThat(authorDTO.createdAt()).isNull();
            assertThat(authorDTO.updatedAt()).isNull();
        }
    }

    // ---------------------------------------------------------------
    // BookDTO.from(Book) - category IDs
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO.from() category mapping")
    class CategoryMappingTests {

        @Test
        @DisplayName("should map empty category IDs list")
        void shouldMapEmptyCategoryIds() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.categoryIds()).isEmpty();
        }

        @Test
        @DisplayName("should map single category ID")
        void shouldMapSingleCategoryId() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.addCategory("cat-1");

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.categoryIds()).containsExactly("cat-1");
        }

        @Test
        @DisplayName("should map multiple category IDs in order")
        void shouldMapMultipleCategoryIds() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.addCategory("software-engineering");
            book.addCategory("architecture");
            book.addCategory("ddd");

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.categoryIds())
                .containsExactly("software-engineering", "architecture", "ddd");
        }
    }

    // ---------------------------------------------------------------
    // BookDTO.from(Book) - publisher
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO.from() publisher mapping")
    class PublisherMappingTests {

        @Test
        @DisplayName("should map null publisher ID when not set")
        void shouldMapNullPublisherId() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.publisherId()).isNull();
        }

        @Test
        @DisplayName("should map publisher ID when set")
        void shouldMapPublisherId() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );
            book.setPublisher("publisher-42");

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.publisherId()).isEqualTo("publisher-42");
        }
    }

    // ---------------------------------------------------------------
    // BookDTO.from(Book) - complete book scenario
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO.from() complete scenario")
    class CompleteScenarioTests {

        @Test
        @DisplayName("should map fully populated book correctly")
        void shouldMapFullyPopulatedBook() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"),
                "Domain-Driven Design: Tackling Complexity in the Heart of Software",
                "This book provides a broad framework for making design decisions",
                LocalDate.of(2003, 9, 30),
                560,
                "en"
            );
            book.addAuthor("author-evans", "Eric Evans", AuthorRole.AUTHOR);
            book.addAuthor("author-millett", "Scott Millett", AuthorRole.CONTRIBUTOR);
            book.setPublisher("publisher-addison-wesley");
            book.addCategory("software-engineering");
            book.addCategory("architecture");
            book.addCategory("domain-driven-design");

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.id()).isEqualTo(book.getId().getValue());
            assertThat(dto.isbn()).isEqualTo("9787111407010");
            assertThat(dto.title()).isEqualTo("Domain-Driven Design: Tackling Complexity in the Heart of Software");
            assertThat(dto.description()).isEqualTo("This book provides a broad framework for making design decisions");
            assertThat(dto.publicationDate()).isEqualTo(LocalDate.of(2003, 9, 30));
            assertThat(dto.pageCount()).isEqualTo(560);
            assertThat(dto.language()).isEqualTo("en");
            assertThat(dto.status()).isEqualTo("DRAFT");
            assertThat(dto.publisherId()).isEqualTo("publisher-addison-wesley");
            assertThat(dto.authors()).hasSize(2);
            assertThat(dto.categoryIds()).hasSize(3);
        }

        @Test
        @DisplayName("should map minimal book correctly")
        void shouldMapMinimalBook() {
            Book book = Book.create(
                new ISBN("978-0-13-235088-4"),
                "Clean Code",
                null,
                null,
                null,
                null
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.isbn()).isEqualTo("9780132350884");
            assertThat(dto.title()).isEqualTo("Clean Code");
            assertThat(dto.description()).isNull();
            assertThat(dto.publicationDate()).isNull();
            assertThat(dto.pageCount()).isNull();
            assertThat(dto.language()).isNull();
            assertThat(dto.status()).isEqualTo("DRAFT");
            assertThat(dto.publisherId()).isNull();
            assertThat(dto.authors()).isEmpty();
            assertThat(dto.categoryIds()).isEmpty();
        }

        @Test
        @DisplayName("should produce independent DTO from Book changes")
        void shouldProduceIndependentDTO() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Original Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            // Mutating the book after DTO creation should not affect the DTO
            book.updateBasicInfo("Updated Title", "updated desc",
                LocalDate.of(2024, 1, 1), 999, "fr");

            assertThat(dto.title()).isEqualTo("Original Title");
            assertThat(dto.description()).isEqualTo("desc");
            assertThat(dto.pageCount()).isEqualTo(100);
        }
    }

    // ---------------------------------------------------------------
    // BookDTO record behavior
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("BookDTO record behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("should have correct equals based on all fields")
        void shouldHaveCorrectEquals() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto1 = BookDTO.from(book);
            BookDTO dto2 = BookDTO.from(book);

            assertThat(dto1).isEqualTo(dto2);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Title", "desc", null, 100, "en"
            );

            BookDTO dto = BookDTO.from(book);

            assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
        }

        @Test
        @DisplayName("should produce two different DTOs from two different books")
        void shouldProduceDifferentDTOsFromDifferentBooks() {
            Book book1 = Book.create(
                new ISBN("978-7-111-40701-0"), "Book One", "desc1", null, 100, "en"
            );
            Book book2 = Book.create(
                new ISBN("978-0-13-235088-4"), "Book Two", "desc2", null, 200, "fr"
            );

            BookDTO dto1 = BookDTO.from(book1);
            BookDTO dto2 = BookDTO.from(book2);

            assertThat(dto1).isNotEqualTo(dto2);
            assertThat(dto1.title()).isEqualTo("Book One");
            assertThat(dto2.title()).isEqualTo("Book Two");
        }
    }
}
