package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.AuthorNotFoundException;
import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.repository.AuthorRepository;
import com.library.shared.domain.model.AuthorId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorManagementServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorManagementService service;

    private AuthorId authorId;

    @BeforeEach
    void setUp() {
        authorId = AuthorId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    // ------------------------------------------------------------------ //
    //  createAuthor
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("createAuthor")
    class CreateAuthorTests {

        @Test
        @DisplayName("should create author and return saved entity")
        void shouldCreateAuthor() {
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = service.createAuthor("Joshua Bloch", "Java expert",
                LocalDate.of(1961, 8, 28), null, "US");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Joshua Bloch");
            assertThat(result.getBiography()).isEqualTo("Java expert");
            assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1961, 8, 28));
            assertThat(result.getNationality()).isEqualTo("US");
            verify(authorRepository).save(any(Author.class));
        }

        @Test
        @DisplayName("should create author with minimal fields")
        void shouldCreateAuthor_minimalFields() {
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = service.createAuthor("Jane Doe", null, null, null, null);

            assertThat(result.getName()).isEqualTo("Jane Doe");
            assertThat(result.getBiography()).isNull();
            assertThat(result.getNationality()).isNull();
        }
    }

    // ------------------------------------------------------------------ //
    //  updateAuthor
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("updateAuthor")
    class UpdateAuthorTests {

        @Test
        @DisplayName("should update author personal info and biography")
        void shouldUpdateAuthor() {
            Author author = Author.create("Old Name", "Old bio",
                LocalDate.of(1960, 1, 1), null, "US");
            when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = service.updateAuthor(authorId, "New Name", "UK",
                LocalDate.of(1970, 6, 15), null, "Updated bio");

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getNationality()).isEqualTo("UK");
            assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1970, 6, 15));
            assertThat(result.getBiography()).isEqualTo("Updated bio");
            verify(authorRepository).save(author);
        }

        @Test
        @DisplayName("should update author without changing biography when null")
        void shouldNotChangeBiography_whenNull() {
            Author author = Author.create("Name", "Original bio", null, null, null);
            when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = service.updateAuthor(authorId, "Updated Name", null, null, null, null);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getBiography()).isEqualTo("Original bio");
        }

        @Test
        @DisplayName("should throw AuthorNotFoundException when author not found")
        void shouldThrow_whenNotFound() {
            when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAuthor(authorId, "Name", null, null, null, null))
                .isInstanceOf(AuthorNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  getAuthor
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAuthor")
    class GetAuthorTests {

        @Test
        @DisplayName("should return author when found")
        void shouldReturnAuthor() {
            Author author = Author.create("Joshua Bloch", "Java guru", null, null, "US");
            when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

            Author result = service.getAuthor(authorId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Joshua Bloch");
        }

        @Test
        @DisplayName("should throw AuthorNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAuthor(authorId))
                .isInstanceOf(AuthorNotFoundException.class)
                .hasMessageContaining("Author not found");
        }
    }

    // ------------------------------------------------------------------ //
    //  searchAuthors
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("searchAuthors")
    class SearchAuthorsTests {

        @Test
        @DisplayName("should return paginated search results")
        void shouldReturnPaginatedResults() {
            Author author1 = Author.create("Joshua Bloch", null, null, null, null);
            Author author2 = Author.create("Joshua Long", null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> page = new PageImpl<>(List.of(author1, author2));
            when(authorRepository.findByNameContaining("Joshua", pageable)).thenReturn(page);

            Page<Author> result = service.searchAuthors("Joshua", pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting(Author::getName)
                .containsExactly("Joshua Bloch", "Joshua Long");
        }
    }

    // ------------------------------------------------------------------ //
    //  getAllAuthors
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAllAuthors")
    class GetAllAuthorsTests {

        @Test
        @DisplayName("should return all authors")
        void shouldReturnAllAuthors() {
            Author author1 = Author.create("Author One", null, null, null, null);
            Author author2 = Author.create("Author Two", null, null, null, null);
            when(authorRepository.findAll()).thenReturn(List.of(author1, author2));

            List<Author> result = service.getAllAuthors();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no authors exist")
        void shouldReturnEmptyList() {
            when(authorRepository.findAll()).thenReturn(Collections.emptyList());

            List<Author> result = service.getAllAuthors();

            assertThat(result).isEmpty();
        }
    }

    // ------------------------------------------------------------------ //
    //  deleteAuthor
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("deleteAuthor")
    class DeleteAuthorTests {

        @Test
        @DisplayName("should delete author when exists")
        void shouldDeleteAuthor() {
            when(authorRepository.existsById(authorId)).thenReturn(true);

            service.deleteAuthor(authorId);

            verify(authorRepository).deleteById(authorId);
        }

        @Test
        @DisplayName("should throw AuthorNotFoundException when author not found")
        void shouldThrow_whenNotFound() {
            when(authorRepository.existsById(authorId)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteAuthor(authorId))
                .isInstanceOf(AuthorNotFoundException.class)
                .hasMessageContaining("Author not found");

            verify(authorRepository, never()).deleteById(any());
        }
    }
}
