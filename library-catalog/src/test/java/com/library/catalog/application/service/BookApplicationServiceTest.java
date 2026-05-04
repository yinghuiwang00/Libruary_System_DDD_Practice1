package com.library.catalog.application.service;

import com.library.catalog.application.command.CreateBookCommand;
import com.library.catalog.application.command.UpdateBookCommand;
import com.library.catalog.application.dto.BookDTO;
import com.library.catalog.application.query.BookSearchCriteria;
import com.library.catalog.domain.exception.BookNotFoundException;
import com.library.catalog.domain.exception.InvalidOperationException;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.BookStatus;
import com.library.catalog.domain.repository.BookRepository;
import com.library.catalog.domain.service.BookManagementService;
import com.library.shared.domain.model.BookId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookApplicationService unit tests")
class BookApplicationServiceTest {

    @Mock
    private BookManagementService bookManagementService;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookApplicationService service;

    private Book createTestBook() {
        return Book.create(
            new ISBN("978-7-111-40701-0"),
            "Domain-Driven Design",
            "Software architecture book",
            LocalDate.of(2003, 9, 30),
            400,
            "zh"
        );
    }

    private Book createTestBookWithAuthorAndPublisher() {
        Book book = createTestBook();
        book.addAuthor("author-1", "Eric Evans", com.library.catalog.domain.model.enums.AuthorRole.AUTHOR);
        book.setPublisher("publisher-1");
        book.addCategory("category-1");
        return book;
    }

    // ---------------------------------------------------------------
    // createBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("createBook")
    class CreateBookTests {

        @Test
        @DisplayName("should create book and return DTO with all fields mapped")
        void shouldCreateBookAndReturnDTO() {
            Book book = createTestBook();
            when(bookManagementService.createBook(
                any(ISBN.class), eq("Domain-Driven Design"), eq("Software architecture book"),
                eq(LocalDate.of(2003, 9, 30)), eq(400), eq("zh")
            )).thenReturn(book);

            CreateBookCommand command = new CreateBookCommand(
                "978-7-111-40701-0", "Domain-Driven Design",
                "Software architecture book", "2003-09-30", 400, "zh"
            );

            BookDTO result = service.createBook(command);

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Domain-Driven Design");
            assertThat(result.isbn()).isEqualTo("9787111407010");
            assertThat(result.description()).isEqualTo("Software architecture book");
            assertThat(result.publicationDate()).isEqualTo(LocalDate.of(2003, 9, 30));
            assertThat(result.pageCount()).isEqualTo(400);
            assertThat(result.language()).isEqualTo("zh");
            assertThat(result.status()).isEqualTo("DRAFT");

            verify(bookManagementService).createBook(
                any(ISBN.class), eq("Domain-Driven Design"), eq("Software architecture book"),
                eq(LocalDate.of(2003, 9, 30)), eq(400), eq("zh")
            );
        }

        @Test
        @DisplayName("should create book with null publication date")
        void shouldCreateBookWithNullPublicationDate() {
            Book book = Book.create(
                new ISBN("978-7-111-40701-0"), "Test Book", "desc", null, 200, "en"
            );
            when(bookManagementService.createBook(
                any(ISBN.class), eq("Test Book"), eq("desc"),
                eq(null), eq(200), eq("en")
            )).thenReturn(book);

            CreateBookCommand command = new CreateBookCommand(
                "978-7-111-40701-0", "Test Book", "desc", null, 200, "en"
            );

            BookDTO result = service.createBook(command);

            assertThat(result).isNotNull();
            assertThat(result.publicationDate()).isNull();
            assertThat(result.title()).isEqualTo("Test Book");
        }

        @Test
        @DisplayName("should delegate ISBN construction to ISBN value object")
        void shouldPassISBNToManagementService() {
            Book book = createTestBook();
            when(bookManagementService.createBook(
                any(ISBN.class), anyString(), any(), any(), any(), any()
            )).thenReturn(book);

            CreateBookCommand command = new CreateBookCommand(
                "978-7-111-40701-0", "Title", "desc", "2024-01-15", 300, "en"
            );

            service.createBook(command);

            verify(bookManagementService).createBook(
                any(ISBN.class), eq("Title"), eq("desc"),
                eq(LocalDate.of(2024, 1, 15)), eq(300), eq("en")
            );
        }
    }

    // ---------------------------------------------------------------
    // getBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getBook")
    class GetBookTests {

        @Test
        @DisplayName("should return book DTO when book exists")
        void shouldReturnBookDTO() {
            Book book = createTestBook();
            when(bookManagementService.getBook(book.getId())).thenReturn(book);

            BookDTO result = service.getBook(book.getId().getValue());

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(book.getId().getValue());
            assertThat(result.title()).isEqualTo("Domain-Driven Design");
            assertThat(result.isbn()).isEqualTo("9787111407010");

            verify(bookManagementService).getBook(book.getId());
        }

        @Test
        @DisplayName("should throw when book not found")
        void shouldThrowWhenBookNotFound() {
            String unknownId = "nonexistent-id";
            when(bookManagementService.getBook(BookId.of(unknownId)))
                .thenThrow(new BookNotFoundException("Book not found: " + unknownId));

            assertThatThrownBy(() -> service.getBook(unknownId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining(unknownId);

            verify(bookManagementService).getBook(BookId.of(unknownId));
        }

        @Test
        @DisplayName("should convert BookId from string correctly")
        void shouldConvertBookIdFromString() {
            String idValue = "test-book-id-123";
            Book book = createTestBook();
            when(bookManagementService.getBook(BookId.of(idValue))).thenReturn(book);

            service.getBook(idValue);

            verify(bookManagementService).getBook(BookId.of(idValue));
        }
    }

    // ---------------------------------------------------------------
    // getAllBooks
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getAllBooks")
    class GetAllBooksTests {

        @Test
        @DisplayName("should return list of book DTOs")
        void shouldReturnListOfBookDTOs() {
            Book book1 = Book.create(new ISBN("978-7-111-40701-0"), "Book 1", "desc1", null, 100, "zh");
            Book book2 = Book.create(new ISBN("978-0-13-235088-4"), "Book 2", "desc2", null, 200, "en");
            when(bookManagementService.getAllBooks()).thenReturn(List.of(book1, book2));

            List<BookDTO> results = service.getAllBooks();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).title()).isEqualTo("Book 1");
            assertThat(results.get(1).title()).isEqualTo("Book 2");

            verify(bookManagementService).getAllBooks();
        }

        @Test
        @DisplayName("should return empty list when no books exist")
        void shouldReturnEmptyListWhenNoBooks() {
            when(bookManagementService.getAllBooks()).thenReturn(Collections.emptyList());

            List<BookDTO> results = service.getAllBooks();

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return single book DTO")
        void shouldReturnSingleBook() {
            Book book = createTestBook();
            when(bookManagementService.getAllBooks()).thenReturn(List.of(book));

            List<BookDTO> results = service.getAllBooks();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(book.getId().getValue());
        }
    }

    // ---------------------------------------------------------------
    // updateBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("updateBook")
    class UpdateBookTests {

        @Test
        @DisplayName("should update book and return DTO")
        void shouldUpdateBookAndReturnDTO() {
            Book originalBook = createTestBook();
            String bookId = originalBook.getId().getValue();

            // After update, title changes
            Book updatedBook = createTestBook();
            updatedBook.updateBasicInfo("Updated Title", "Updated description",
                LocalDate.of(2024, 6, 15), 500, "en");

            when(bookManagementService.updateBook(
                BookId.of(bookId), "Updated Title", "Updated description",
                LocalDate.of(2024, 6, 15), 500, "en"
            )).thenReturn(updatedBook);

            UpdateBookCommand command = new UpdateBookCommand(
                "Updated Title", "Updated description", "2024-06-15", 500, "en"
            );

            BookDTO result = service.updateBook(bookId, command);

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Updated Title");

            verify(bookManagementService).updateBook(
                BookId.of(bookId), "Updated Title", "Updated description",
                LocalDate.of(2024, 6, 15), 500, "en"
            );
        }

        @Test
        @DisplayName("should update book with null publication date")
        void shouldUpdateWithNullPublicationDate() {
            Book book = createTestBook();
            String bookId = book.getId().getValue();

            when(bookManagementService.updateBook(
                BookId.of(bookId), "New Title", null, null, null, null
            )).thenReturn(book);

            UpdateBookCommand command = new UpdateBookCommand("New Title", null, null, null, null);

            BookDTO result = service.updateBook(bookId, command);

            assertThat(result).isNotNull();
            verify(bookManagementService).updateBook(
                BookId.of(bookId), "New Title", null, null, null, null
            );
        }

        @Test
        @DisplayName("should throw when updating nonexistent book")
        void shouldThrowWhenUpdatingNonexistentBook() {
            String unknownId = "nonexistent-id";
            when(bookManagementService.updateBook(
                eq(BookId.of(unknownId)), any(), any(), any(), any(), any()
            )).thenThrow(new BookNotFoundException("Book not found: " + unknownId));

            UpdateBookCommand command = new UpdateBookCommand("Title", null, null, null, null);

            assertThatThrownBy(() -> service.updateBook(unknownId, command))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // publishBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("publishBook")
    class PublishBookTests {

        @Test
        @DisplayName("should publish book and return DTO with PUBLISHED status")
        void shouldPublishBookAndReturnDTO() {
            Book book = createTestBookWithAuthorAndPublisher();
            book.publish();
            String bookId = book.getId().getValue();

            when(bookManagementService.publishBook(BookId.of(bookId))).thenReturn(book);

            BookDTO result = service.publishBook(bookId);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("PUBLISHED");

            verify(bookManagementService).publishBook(BookId.of(bookId));
        }

        @Test
        @DisplayName("should throw when publishing already published book")
        void shouldThrowWhenPublishingPublishedBook() {
            String bookId = "some-id";
            when(bookManagementService.publishBook(BookId.of(bookId)))
                .thenThrow(new InvalidOperationException("Can only publish DRAFT or UNPUBLISHED books"));

            assertThatThrownBy(() -> service.publishBook(bookId))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // ---------------------------------------------------------------
    // unpublishBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("unpublishBook")
    class UnpublishBookTests {

        @Test
        @DisplayName("should unpublish book and return DTO with UNPUBLISHED status")
        void shouldUnpublishBookAndReturnDTO() {
            Book book = createTestBookWithAuthorAndPublisher();
            book.publish();
            book.unpublish();
            String bookId = book.getId().getValue();

            when(bookManagementService.unpublishBook(BookId.of(bookId))).thenReturn(book);

            BookDTO result = service.unpublishBook(bookId);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("UNPUBLISHED");

            verify(bookManagementService).unpublishBook(BookId.of(bookId));
        }

        @Test
        @DisplayName("should throw when unpublishing non-published book")
        void shouldThrowWhenUnpublishingNonPublishedBook() {
            String bookId = "some-id";
            when(bookManagementService.unpublishBook(BookId.of(bookId)))
                .thenThrow(new InvalidOperationException("Can only unpublish PUBLISHED books"));

            assertThatThrownBy(() -> service.unpublishBook(bookId))
                .isInstanceOf(InvalidOperationException.class);
        }
    }

    // ---------------------------------------------------------------
    // deleteBook
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("deleteBook")
    class DeleteBookTests {

        @Test
        @DisplayName("should delete book successfully")
        void shouldDeleteBook() {
            String bookId = "book-to-delete";

            doNothing().when(bookManagementService).deleteBook(BookId.of(bookId));

            service.deleteBook(bookId);

            verify(bookManagementService).deleteBook(BookId.of(bookId));
        }

        @Test
        @DisplayName("should throw when deleting nonexistent book")
        void shouldThrowWhenDeletingNonexistentBook() {
            String unknownId = "nonexistent-id";
            doThrow(new BookNotFoundException("Book not found: " + unknownId))
                .when(bookManagementService).deleteBook(BookId.of(unknownId));

            assertThatThrownBy(() -> service.deleteBook(unknownId))
                .isInstanceOf(BookNotFoundException.class);

            verify(bookManagementService).deleteBook(BookId.of(unknownId));
        }

        @Test
        @DisplayName("should throw when deleting published book")
        void shouldThrowWhenDeletingPublishedBook() {
            String bookId = "published-book";
            doThrow(new InvalidOperationException("Cannot delete a published book. Unpublish first."))
                .when(bookManagementService).deleteBook(BookId.of(bookId));

            assertThatThrownBy(() -> service.deleteBook(bookId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot delete a published book");
        }
    }

    // ---------------------------------------------------------------
    // searchBooks
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("searchBooks")
    class SearchBooksTests {

        @Test
        @DisplayName("should return paginated book DTOs")
        void shouldReturnPaginatedBookDTOs() {
            Book book1 = Book.create(new ISBN("978-7-111-40701-0"), "DDD Book", "desc", null, 400, "zh");
            Book book2 = Book.create(new ISBN("978-0-13-235088-4"), "Clean Code", "desc", null, 300, "en");

            Page<Book> bookPage = new PageImpl<>(List.of(book1, book2));
            BookSearchCriteria criteria = new BookSearchCriteria("DDD", null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(bookRepository.search(criteria, pageable)).thenReturn(bookPage);

            Page<BookDTO> result = service.searchBooks(criteria, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).title()).isEqualTo("DDD Book");
            assertThat(result.getContent().get(1).title()).isEqualTo("Clean Code");

            verify(bookRepository).search(criteria, pageable);
        }

        @Test
        @DisplayName("should return empty page when no results")
        void shouldReturnEmptyPage() {
            BookSearchCriteria criteria = new BookSearchCriteria(null, null, BookStatus.DRAFT, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(Collections.emptyList());

            when(bookRepository.search(criteria, pageable)).thenReturn(emptyPage);

            Page<BookDTO> result = service.searchBooks(criteria, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should return correct pagination metadata")
        void shouldReturnCorrectPaginationMetadata() {
            Book book = createTestBook();
            Page<Book> bookPage = new PageImpl<>(List.of(book), PageRequest.of(0, 5), 25L);

            BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 5);

            when(bookRepository.search(criteria, pageable)).thenReturn(bookPage);

            Page<BookDTO> result = service.searchBooks(criteria, pageable);

            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(5);
            assertThat(result.getNumber()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("should delegate to book repository not management service")
        void shouldUseRepositoryDirectly() {
            BookSearchCriteria criteria = new BookSearchCriteria("test", null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(Collections.emptyList());

            when(bookRepository.search(criteria, pageable)).thenReturn(emptyPage);

            service.searchBooks(criteria, pageable);

            verify(bookRepository).search(criteria, pageable);
            verifyNoInteractions(bookManagementService);
        }

        @Test
        @DisplayName("should handle search with all criteria fields populated")
        void shouldHandleFullCriteriaSearch() {
            Book book = createTestBook();
            Page<Book> bookPage = new PageImpl<>(List.of(book));

            BookSearchCriteria criteria = new BookSearchCriteria(
                "Title", "Author", BookStatus.PUBLISHED, "pub-1", "cat-1", "en"
            );
            Pageable pageable = PageRequest.of(0, 20);

            when(bookRepository.search(criteria, pageable)).thenReturn(bookPage);

            Page<BookDTO> result = service.searchBooks(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
