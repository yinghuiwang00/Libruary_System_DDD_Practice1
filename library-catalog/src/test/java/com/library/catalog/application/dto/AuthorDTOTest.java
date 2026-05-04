package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.model.BookAuthor;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorDTO tests")
class AuthorDTOTest {

    // ---------------------------------------------------------------
    // AuthorDTO.from(Author) - full field mapping
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("AuthorDTO.from(Author)")
    class FromAuthorTests {

        @Test
        @DisplayName("should map all fields from Author")
        void shouldMapAllFieldsFromAuthor() {
            Author author = Author.create(
                "Eric Evans",
                "Software architect and author",
                LocalDate.of(1963, 1, 1),
                null,
                "American"
            );

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.id()).isEqualTo(author.getId().getValue());
            assertThat(dto.name()).isEqualTo("Eric Evans");
            assertThat(dto.biography()).isEqualTo("Software architect and author");
            assertThat(dto.birthDate()).isEqualTo(LocalDate.of(1963, 1, 1));
            assertThat(dto.deathDate()).isNull();
            assertThat(dto.nationality()).isEqualTo("American");
            assertThat(dto.role()).isNull();
        }

        @Test
        @DisplayName("should map id from AuthorId value")
        void shouldMapAuthorId() {
            Author author = Author.create("Jane Doe", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.id()).isEqualTo(author.getId().getValue());
        }

        @Test
        @DisplayName("should map name correctly")
        void shouldMapName() {
            Author author = Author.create("Martin Fowler", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.name()).isEqualTo("Martin Fowler");
        }

        @Test
        @DisplayName("should map biography when provided")
        void shouldMapBiography() {
            Author author = Author.create(
                "Author Name",
                "A prolific writer of technical books",
                null, null, null
            );

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.biography()).isEqualTo("A prolific writer of technical books");
        }

        @Test
        @DisplayName("should map null biography")
        void shouldMapNullBiography() {
            Author author = Author.create("Author Name", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.biography()).isNull();
        }

        @Test
        @DisplayName("should map birthDate correctly")
        void shouldMapBirthDate() {
            LocalDate birthDate = LocalDate.of(1970, 6, 15);
            Author author = Author.create("Author Name", null, birthDate, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.birthDate()).isEqualTo(birthDate);
        }

        @Test
        @DisplayName("should map null birthDate")
        void shouldMapNullBirthDate() {
            Author author = Author.create("Author Name", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.birthDate()).isNull();
        }

        @Test
        @DisplayName("should map deathDate when provided")
        void shouldMapDeathDate() {
            LocalDate birthDate = LocalDate.of(1900, 1, 1);
            LocalDate deathDate = LocalDate.of(1980, 12, 31);
            Author author = Author.create("Author Name", null, birthDate, deathDate, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.deathDate()).isEqualTo(deathDate);
        }

        @Test
        @DisplayName("should map null deathDate")
        void shouldMapNullDeathDate() {
            Author author = Author.create("Author Name", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.deathDate()).isNull();
        }

        @Test
        @DisplayName("should map nationality when provided")
        void shouldMapNationality() {
            Author author = Author.create("Author Name", null, null, null, "British");

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.nationality()).isEqualTo("British");
        }

        @Test
        @DisplayName("should map null nationality")
        void shouldMapNullNationality() {
            Author author = Author.create("Author Name", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.nationality()).isNull();
        }

        @Test
        @DisplayName("should always set role to null from Author")
        void shouldSetRoleToNullFromAuthor() {
            Author author = Author.create("Author Name", "bio", null, null, "French");

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.role()).isNull();
        }

        @Test
        @DisplayName("should map JPA-managed fields as null when not persisted")
        void shouldMapNullVersionAndTimestampsForNewEntity() {
            Author author = Author.create("Author Name", null, null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            assertThat(dto.version()).isNull();
            assertThat(dto.createdAt()).isNull();
            assertThat(dto.updatedAt()).isNull();
        }

        @Test
        @DisplayName("should produce equal DTOs from same Author")
        void shouldProduceEqualDTOsFromSameAuthor() {
            Author author = Author.create("Author Name", "bio", null, null, null);

            AuthorDTO dto1 = AuthorDTO.from(author);
            AuthorDTO dto2 = AuthorDTO.from(author);

            assertThat(dto1).isEqualTo(dto2);
        }

        @Test
        @DisplayName("should produce independent DTO snapshot")
        void shouldProduceIndependentSnapshot() {
            Author author = Author.create("Original Name", "Original Bio", null, null, null);

            AuthorDTO dto = AuthorDTO.from(author);

            // Mutate the author after DTO creation
            author.updateBiography("Updated Bio");
            author.updatePersonalInfo("Updated Name", "Japanese", null, null);

            // DTO should still hold original values
            assertThat(dto.name()).isEqualTo("Original Name");
            assertThat(dto.biography()).isEqualTo("Original Bio");
        }
    }

    // ---------------------------------------------------------------
    // AuthorDTO.from(BookAuthor) - BookAuthor field mapping
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("AuthorDTO.from(BookAuthor)")
    class FromBookAuthorTests {

        @Test
        @DisplayName("should map role and identity fields from BookAuthor")
        void shouldMapRoleAndIdentityFields() {
            BookId bookId = BookId.generate();
            AuthorId authorId = AuthorId.generate();
            BookAuthor bookAuthor = BookAuthor.of(
                bookId, authorId, "Eric Evans", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.id()).isEqualTo(authorId.getValue());
            assertThat(dto.name()).isEqualTo("Eric Evans");
            assertThat(dto.role()).isEqualTo("AUTHOR");
        }

        @Test
        @DisplayName("should map authorId correctly")
        void shouldMapAuthorId() {
            AuthorId authorId = AuthorId.generate();
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), authorId, "Author", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.id()).isEqualTo(authorId.getValue());
        }

        @Test
        @DisplayName("should map authorName correctly")
        void shouldMapAuthorName() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Martin Fowler", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.name()).isEqualTo("Martin Fowler");
        }

        @Test
        @DisplayName("should map CO_AUTHOR role")
        void shouldMapCoAuthorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Co-Author", AuthorRole.CO_AUTHOR, 2
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.role()).isEqualTo("CO_AUTHOR");
        }

        @Test
        @DisplayName("should map EDITOR role")
        void shouldMapEditorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Editor", AuthorRole.EDITOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.role()).isEqualTo("EDITOR");
        }

        @Test
        @DisplayName("should map TRANSLATOR role")
        void shouldMapTranslatorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Translator", AuthorRole.TRANSLATOR, 3
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.role()).isEqualTo("TRANSLATOR");
        }

        @Test
        @DisplayName("should map CONTRIBUTOR role")
        void shouldMapContributorRole() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Contributor", AuthorRole.CONTRIBUTOR, 4
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.role()).isEqualTo("CONTRIBUTOR");
        }

        @Test
        @DisplayName("should set biography fields to null from BookAuthor")
        void shouldSetBiographyFieldsToNull() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Author", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.biography()).isNull();
            assertThat(dto.birthDate()).isNull();
            assertThat(dto.deathDate()).isNull();
            assertThat(dto.nationality()).isNull();
        }

        @Test
        @DisplayName("should set version and timestamps to null from BookAuthor")
        void shouldSetVersionAndTimestampsToNull() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Author", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto = AuthorDTO.from(bookAuthor);

            assertThat(dto.version()).isNull();
            assertThat(dto.createdAt()).isNull();
            assertThat(dto.updatedAt()).isNull();
        }

        @Test
        @DisplayName("should produce equal DTOs from same BookAuthor")
        void shouldProduceEqualDTOsFromSameBookAuthor() {
            BookAuthor bookAuthor = BookAuthor.of(
                BookId.generate(), AuthorId.generate(),
                "Author", AuthorRole.AUTHOR, 1
            );

            AuthorDTO dto1 = AuthorDTO.from(bookAuthor);
            AuthorDTO dto2 = AuthorDTO.from(bookAuthor);

            assertThat(dto1).isEqualTo(dto2);
        }
    }
}
