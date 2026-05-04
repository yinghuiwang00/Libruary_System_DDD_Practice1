package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.*;
import com.library.catalog.domain.model.*;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.model.enums.BookStatus;
import com.library.catalog.domain.repository.AuthorRepository;
import com.library.catalog.domain.repository.BookRepository;
import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookManagementServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookManagementService service;

    private ISBN validISBN;
    private BookId bookId;

    @BeforeEach
    void setUp() {
        validISBN = new ISBN("978-7-111-40701-0");
        bookId = BookId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    private Book createDraftBook() {
        Book book = Book.create(validISBN, "Effective Java",
            "A comprehensive guide to Java programming",
            LocalDate.of(2008, 5, 8), 416, "en");
        return book;
    }

    private Book createPublishableBook() {
        Book book = createDraftBook();
        book.addAuthor("author-1", "Joshua Bloch", AuthorRole.AUTHOR);
        book.setPublisher("publisher-1");
        book.addCategory("category-1");
        return book;
    }

    // ------------------------------------------------------------------ //
    //  createBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("createBook")
    class CreateBookTests {

        @Test
        @DisplayName("should create book when ISBN is unique")
        void shouldCreateBook_whenIsbnUnique() {
            when(bookRepository.existsByIsbn(validISBN)).thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.createBook(validISBN, "Effective Java",
                "A comprehensive guide", LocalDate.of(2008, 5, 8), 416, "en");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Effective Java");
            assertThat(result.getStatus()).isEqualTo(BookStatus.DRAFT);
            verify(bookRepository).existsByIsbn(validISBN);
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @DisplayName("should throw DuplicateISBNException when ISBN already exists")
        void shouldThrow_whenIsbnDuplicate() {
            when(bookRepository.existsByIsbn(validISBN)).thenReturn(true);

            assertThatThrownBy(() -> service.createBook(validISBN, "Duplicate Book",
                    "desc", null, null, null))
                .isInstanceOf(DuplicateISBNException.class)
                .hasMessageContaining("already exists");

            verify(bookRepository).existsByIsbn(validISBN);
            verify(bookRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  addAuthorToBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("addAuthorToBook")
    class AddAuthorToBookTests {

        @Test
        @DisplayName("should add author to book")
        void shouldAddAuthor() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            Author author = Author.create("Joshua Bloch", null, null, null, "US");
            when(authorRepository.findById(AuthorId.of("author-1"))).thenReturn(Optional.of(author));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.addAuthorToBook(bookId, "author-1", AuthorRole.AUTHOR);

            assertThat(result.getAuthors()).hasSize(1);
            assertThat(result.getAuthors().get(0).getAuthorName()).isEqualTo("Joshua Bloch");
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAuthorToBook(bookId, "author-1", AuthorRole.AUTHOR))
                .isInstanceOf(BookNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when author not found")
        void shouldThrow_whenAuthorNotFound() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(authorRepository.findById(AuthorId.of("author-999")))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAuthorToBook(bookId, "author-999", AuthorRole.AUTHOR))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("Author not found");
        }
    }

    // ------------------------------------------------------------------ //
    //  removeAuthorFromBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("removeAuthorFromBook")
    class RemoveAuthorFromBookTests {

        @Test
        @DisplayName("should remove author from book")
        void shouldRemoveAuthor() {
            Book book = createDraftBook();
            book.addAuthor("author-1", "Joshua Bloch", AuthorRole.AUTHOR);
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.removeAuthorFromBook(bookId, "author-1");

            assertThat(result.getAuthors()).isEmpty();
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeAuthorFromBook(bookId, "author-1"))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  setPublisher
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("setPublisher")
    class SetPublisherTests {

        @Test
        @DisplayName("should set publisher on book")
        void shouldSetPublisher() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.setPublisher(bookId, "publisher-1");

            assertThat(result.getPublisherId()).isEqualTo("publisher-1");
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setPublisher(bookId, "publisher-1"))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  addCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("addCategory")
    class AddCategoryTests {

        @Test
        @DisplayName("should add category to book")
        void shouldAddCategory() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.addCategory(bookId, "cat-1");

            assertThat(result.getCategoryIds()).containsExactly("cat-1");
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addCategory(bookId, "cat-1"))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  removeCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("removeCategory")
    class RemoveCategoryTests {

        @Test
        @DisplayName("should remove category from book")
        void shouldRemoveCategory() {
            Book book = createDraftBook();
            book.addCategory("cat-1");
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.removeCategory(bookId, "cat-1");

            assertThat(result.getCategoryIds()).isEmpty();
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeCategory(bookId, "cat-1"))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  publishBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("publishBook")
    class PublishBookTests {

        @Test
        @DisplayName("should publish a draft book with author, publisher, and category")
        void shouldPublishDraftBook() {
            Book book = createPublishableBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.publishBook(bookId);

            assertThat(result.getStatus()).isEqualTo(BookStatus.PUBLISHED);
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishBook(bookId))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  unpublishBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("unpublishBook")
    class UnpublishBookTests {

        @Test
        @DisplayName("should unpublish a published book")
        void shouldUnpublishBook() {
            Book book = createPublishableBook();
            book.publish();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.unpublishBook(bookId);

            assertThat(result.getStatus()).isEqualTo(BookStatus.UNPUBLISHED);
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.unpublishBook(bookId))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  updateBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("updateBook")
    class UpdateBookTests {

        @Test
        @DisplayName("should update basic info of a draft book")
        void shouldUpdateBasicInfo() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.updateBook(bookId, "New Title", "New Description",
                LocalDate.of(2020, 1, 1), 500, "zh");

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getDescription()).isEqualTo("New Description");
            assertThat(result.getPublicationDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(result.getPageCount()).isEqualTo(500);
            assertThat(result.getLanguage()).isEqualTo("zh");
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBook(bookId, "Title", "Desc",
                    LocalDate.now(), 100, "en"))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  deleteBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("deleteBook")
    class DeleteBookTests {

        @Test
        @DisplayName("should soft delete a draft book")
        void shouldSoftDelete() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

            service.deleteBook(bookId);

            assertThat(book.getStatus()).isEqualTo(BookStatus.DELETED);
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("should throw BookNotFoundException when book not found")
        void shouldThrow_whenBookNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteBook(bookId))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  getBook
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getBook")
    class GetBookTests {

        @Test
        @DisplayName("should return book when found")
        void shouldReturnBook() {
            Book book = createDraftBook();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

            Book result = service.getBook(bookId);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Effective Java");
        }

        @Test
        @DisplayName("should throw BookNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBook(bookId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("Book not found");
        }
    }

    // ------------------------------------------------------------------ //
    //  getAllBooks
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAllBooks")
    class GetAllBooksTests {

        @Test
        @DisplayName("should return all books")
        void shouldReturnAllBooks() {
            Book book1 = createDraftBook();
            Book book2 = Book.create(new ISBN("978-0-13-468599-1"), "Clean Code",
                "A handbook of agile software craftsmanship",
                LocalDate.of(2008, 8, 1), 464, "en");
            when(bookRepository.findAll()).thenReturn(List.of(book1, book2));

            List<Book> result = service.getAllBooks();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Book::getTitle)
                .containsExactly("Effective Java", "Clean Code");
        }

        @Test
        @DisplayName("should return empty list when no books exist")
        void shouldReturnEmptyList() {
            when(bookRepository.findAll()).thenReturn(Collections.emptyList());

            List<Book> result = service.getAllBooks();

            assertThat(result).isEmpty();
        }
    }
}
