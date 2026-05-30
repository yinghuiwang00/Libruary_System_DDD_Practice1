package com.library.catalog.domain.service;

import com.library.catalog.domain.event.BookCreatedEvent;
import com.library.catalog.domain.event.BookDeletedEvent;
import com.library.catalog.domain.event.BookPublishedEvent;
import com.library.catalog.domain.event.BookUpdatedEvent;
import com.library.catalog.domain.exception.BookNotFoundException;
import com.library.catalog.domain.exception.DuplicateISBNException;
import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.repository.AuthorRepository;
import com.library.catalog.domain.repository.BookRepository;
import com.library.catalog.infrastructure.messaging.CatalogDomainEventPublisher;
import com.library.shared.domain.model.BookId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookManagementService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CatalogDomainEventPublisher eventPublisher;

    public BookManagementService(BookRepository bookRepository,
                                 AuthorRepository authorRepository,
                                 CatalogDomainEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Book createBook(ISBN isbn, String title, String description,
                           LocalDate publicationDate, Integer pageCount, String language) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateISBNException("Book with ISBN " + isbn.getValue() + " already exists");
        }
        Book book = Book.create(isbn, title, description, publicationDate, pageCount, language);
        Book saved = bookRepository.save(book);
        eventPublisher.publish(new BookCreatedEvent(
            saved.getId().getValue(), saved.getIsbn().getValue(), saved.getTitle()));
        return saved;
    }

    @Transactional
    public Book addAuthorToBook(BookId bookId, String authorId, AuthorRole role) {
        Book book = findBookOrThrow(bookId);
        Author author = authorRepository.findById(com.library.shared.domain.model.AuthorId.of(authorId))
            .orElseThrow(() -> new BookNotFoundException("Author not found: " + authorId));
        book.addAuthor(authorId, author.getName(), role);
        return bookRepository.save(book);
    }

    @Transactional
    public Book removeAuthorFromBook(BookId bookId, String authorId) {
        Book book = findBookOrThrow(bookId);
        book.removeAuthor(authorId);
        return bookRepository.save(book);
    }

    @Transactional
    public Book setPublisher(BookId bookId, String publisherId) {
        Book book = findBookOrThrow(bookId);
        book.setPublisher(publisherId);
        return bookRepository.save(book);
    }

    @Transactional
    public Book addCategory(BookId bookId, String categoryId) {
        Book book = findBookOrThrow(bookId);
        book.addCategory(categoryId);
        return bookRepository.save(book);
    }

    @Transactional
    public Book removeCategory(BookId bookId, String categoryId) {
        Book book = findBookOrThrow(bookId);
        book.removeCategory(categoryId);
        return bookRepository.save(book);
    }

    @Transactional
    public Book publishBook(BookId bookId) {
        Book book = findBookOrThrow(bookId);
        book.publish();
        Book saved = bookRepository.save(book);
        eventPublisher.publish(new BookPublishedEvent(
            saved.getId().getValue(), saved.getIsbn().getValue(), saved.getTitle()));
        return saved;
    }

    @Transactional
    public Book unpublishBook(BookId bookId) {
        Book book = findBookOrThrow(bookId);
        book.unpublish();
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(BookId bookId, String title, String description,
                           LocalDate publicationDate, Integer pageCount, String language) {
        Book book = findBookOrThrow(bookId);
        book.updateBasicInfo(title, description, publicationDate, pageCount, language);
        Book saved = bookRepository.save(book);
        eventPublisher.publish(new BookUpdatedEvent(
            saved.getId().getValue(), saved.getTitle()));
        return saved;
    }

    @Transactional
    public void deleteBook(BookId bookId) {
        Book book = findBookOrThrow(bookId);
        book.delete();
        bookRepository.save(book);
        eventPublisher.publish(new BookDeletedEvent(
            book.getId().getValue(), book.getTitle()));
    }

    public Book getBook(BookId bookId) {
        return findBookOrThrow(bookId);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    private Book findBookOrThrow(BookId bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId.getValue()));
    }
}
