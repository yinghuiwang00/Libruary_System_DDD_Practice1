package com.library.catalog.interfaces.rest;

import com.library.catalog.application.command.CreateBookCommand;
import com.library.catalog.application.command.UpdateBookCommand;
import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.BookDTO;
import com.library.catalog.application.query.BookSearchCriteria;
import com.library.catalog.application.service.BookApplicationService;
import com.library.catalog.domain.model.enums.BookStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/books")
@Tag(name = "Books", description = "Book management API")
public class BookController {

    private final BookApplicationService bookApplicationService;

    public BookController(BookApplicationService bookApplicationService) {
        this.bookApplicationService = bookApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    public ResponseEntity<ApiResponse<BookDTO>> createBook(@Valid @RequestBody CreateBookCommand command) {
        BookDTO book = bookApplicationService.createBook(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(book));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookDTO>> getBook(@PathVariable String id) {
        BookDTO book = bookApplicationService.getBook(id);
        return ResponseEntity.ok(ApiResponse.ok(book));
    }

    @GetMapping
    @Operation(summary = "Get all books")
    public ResponseEntity<ApiResponse<List<BookDTO>>> getAllBooks() {
        List<BookDTO> books = bookApplicationService.getAllBooks();
        return ResponseEntity.ok(ApiResponse.ok(books));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update book")
    public ResponseEntity<ApiResponse<BookDTO>> updateBook(
            @PathVariable String id, @Valid @RequestBody UpdateBookCommand command) {
        BookDTO book = bookApplicationService.updateBook(id, command);
        return ResponseEntity.ok(ApiResponse.ok(book));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a book")
    public ResponseEntity<ApiResponse<BookDTO>> publishBook(@PathVariable String id) {
        BookDTO book = bookApplicationService.publishBook(id);
        return ResponseEntity.ok(ApiResponse.ok(book));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a book")
    public ResponseEntity<ApiResponse<BookDTO>> unpublishBook(@PathVariable String id) {
        BookDTO book = bookApplicationService.unpublishBook(id);
        return ResponseEntity.ok(ApiResponse.ok(book));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable String id) {
        bookApplicationService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books with filters and pagination")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String publisherId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String language,
            Pageable pageable) {
        BookSearchCriteria criteria = new BookSearchCriteria(
            title, authorName, status, publisherId, categoryId, language);
        Page<BookDTO> results = bookApplicationService.searchBooks(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
