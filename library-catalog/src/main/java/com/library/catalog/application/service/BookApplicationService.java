package com.library.catalog.application.service;

import com.library.catalog.application.command.CreateBookCommand;
import com.library.catalog.application.command.UpdateBookCommand;
import com.library.catalog.application.dto.BookDTO;
import com.library.catalog.application.query.BookSearchCriteria;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.repository.BookRepository;
import com.library.catalog.domain.service.BookManagementService;
import com.library.shared.domain.model.AuthorId;
import com.library.shared.domain.model.BookId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookApplicationService {

    private final BookManagementService bookManagementService;
    private final BookRepository bookRepository;

    public BookApplicationService(BookManagementService bookManagementService, BookRepository bookRepository) {
        this.bookManagementService = bookManagementService;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookDTO createBook(CreateBookCommand command) {
        ISBN isbn = new ISBN(command.isbn());
        Book book = bookManagementService.createBook(
            isbn, command.title(), command.description(),
            command.publicationDate() != null ? LocalDate.parse(command.publicationDate()) : null,
            command.pageCount(), command.language()
        );
        return BookDTO.from(book);
    }

    public BookDTO getBook(String bookId) {
        Book book = bookManagementService.getBook(BookId.of(bookId));
        return BookDTO.from(book);
    }

    public List<BookDTO> getAllBooks() {
        return bookManagementService.getAllBooks().stream()
            .map(BookDTO::from)
            .toList();
    }

    @Transactional
    public BookDTO updateBook(String bookId, UpdateBookCommand command) {
        Book book = bookManagementService.updateBook(
            BookId.of(bookId),
            command.title(), command.description(),
            command.publicationDate() != null ? LocalDate.parse(command.publicationDate()) : null,
            command.pageCount(), command.language()
        );
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO publishBook(String bookId) {
        Book book = bookManagementService.publishBook(BookId.of(bookId));
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO unpublishBook(String bookId) {
        Book book = bookManagementService.unpublishBook(BookId.of(bookId));
        return BookDTO.from(book);
    }

    @Transactional
    public void deleteBook(String bookId) {
        bookManagementService.deleteBook(BookId.of(bookId));
    }

    public Page<BookDTO> searchBooks(BookSearchCriteria criteria, Pageable pageable) {
        Page<Book> books = bookRepository.search(criteria, pageable);
        return books.map(BookDTO::from);
    }

    @Transactional
    public BookDTO addAuthor(String bookId, String authorId, String role) {
        Book book = bookManagementService.addAuthorToBook(
            BookId.of(bookId), authorId, AuthorRole.valueOf(role));
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO removeAuthor(String bookId, String authorId) {
        Book book = bookManagementService.removeAuthorFromBook(BookId.of(bookId), authorId);
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO setPublisher(String bookId, String publisherId) {
        Book book = bookManagementService.setPublisher(BookId.of(bookId), publisherId);
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO addCategory(String bookId, String categoryId) {
        Book book = bookManagementService.addCategory(BookId.of(bookId), categoryId);
        return BookDTO.from(book);
    }

    @Transactional
    public BookDTO removeCategory(String bookId, String categoryId) {
        Book book = bookManagementService.removeCategory(BookId.of(bookId), categoryId);
        return BookDTO.from(book);
    }
}
