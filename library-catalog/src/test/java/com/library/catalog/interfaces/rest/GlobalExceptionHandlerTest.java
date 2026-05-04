package com.library.catalog.interfaces.rest;

import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.domain.exception.*;
import com.library.catalog.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ---------------------------------------------------------------
    // DomainException handling - NOT_FOUND -> 404
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("DomainException -> NOT_FOUND (404)")
    class NotFoundTests {

        @Test
        @DisplayName("should return 404 for AuthorNotFoundException (error code contains NOT_FOUND)")
        void shouldReturn404ForAuthorNotFoundException() {
            AuthorNotFoundException ex = new AuthorNotFoundException("Author not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().errorCode()).isEqualTo("AUTHOR_NOT_FOUND");
            assertThat(response.getBody().error()).isEqualTo("Author not found");
        }

        @Test
        @DisplayName("should return 404 for BookNotFoundException")
        void shouldReturn404ForBookNotFoundException() {
            BookNotFoundException ex = new BookNotFoundException("Book not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("BOOK_NOT_FOUND");
        }

        @Test
        @DisplayName("should return 404 for CategoryNotFoundException")
        void shouldReturn404ForCategoryNotFoundException() {
            CategoryNotFoundException ex = new CategoryNotFoundException("Category not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("CATEGORY_NOT_FOUND");
        }

        @Test
        @DisplayName("should return 404 for PublisherNotFoundException")
        void shouldReturn404ForPublisherNotFoundException() {
            PublisherNotFoundException ex = new PublisherNotFoundException("Publisher not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("PUBLISHER_NOT_FOUND");
        }

        @Test
        @DisplayName("should map error code and message correctly for 404 responses")
        void shouldMapErrorCodeAndMessageFor404() {
            AuthorNotFoundException ex = new AuthorNotFoundException("Author with id abc-123 was not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().errorCode()).isEqualTo("AUTHOR_NOT_FOUND");
            assertThat(response.getBody().error()).isEqualTo("Author with id abc-123 was not found");
        }
    }

    // ---------------------------------------------------------------
    // DomainException handling - DUPLICATE -> 409
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("DomainException -> DUPLICATE (409)")
    class DuplicateTests {

        @Test
        @DisplayName("should return 409 for DuplicateISBNException")
        void shouldReturn409ForDuplicateISBNException() {
            DuplicateISBNException ex = new DuplicateISBNException("ISBN 9780132350884 already exists");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_ISBN");
            assertThat(response.getBody().error()).isEqualTo("ISBN 9780132350884 already exists");
        }

        @Test
        @DisplayName("should return 409 for DuplicateAuthorException")
        void shouldReturn409ForDuplicateAuthorException() {
            DuplicateAuthorException ex = new DuplicateAuthorException("Author already exists");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_AUTHOR");
            assertThat(response.getBody().error()).isEqualTo("Author already exists");
        }
    }

    // ---------------------------------------------------------------
    // DomainException handling - default -> 400
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("DomainException -> default (400)")
    class DefaultDomainExceptionTests {

        @Test
        @DisplayName("should return 400 for InvalidOperationException")
        void shouldReturn400ForInvalidOperationException() {
            InvalidOperationException ex = new InvalidOperationException("Cannot delete a published book");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALIDOPERATION");
            assertThat(response.getBody().error()).isEqualTo("Cannot delete a published book");
        }

        @Test
        @DisplayName("should return 400 for InvalidISBNException")
        void shouldReturn400ForInvalidISBNException() {
            InvalidISBNException ex = new InvalidISBNException("Invalid ISBN format");
            ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALIDISBN");
            assertThat(response.getBody().error()).isEqualTo("Invalid ISBN format");
        }
    }

    // ---------------------------------------------------------------
    // MethodArgumentNotValidException handling
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("MethodArgumentNotValidException handling")
    class ValidationExceptionTests {

        @Test
        @DisplayName("should return 400 with single validation error")
        void shouldReturn400WithSingleValidationError() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "title", "must not be blank"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().error()).contains("title");
            assertThat(response.getBody().error()).contains("must not be blank");
        }

        @Test
        @DisplayName("should return 400 with multiple validation errors joined by semicolons")
        void shouldReturn400WithMultipleValidationErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "title", "must not be blank"));
            bindingResult.addError(new FieldError("request", "isbn", "invalid format"));
            bindingResult.addError(new FieldError("request", "pageCount", "must be positive"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            String errorMsg = response.getBody().error();
            assertThat(errorMsg).contains("title: must not be blank");
            assertThat(errorMsg).contains("isbn: invalid format");
            assertThat(errorMsg).contains("pageCount: must be positive");
            assertThat(errorMsg).contains(";");
        }

        @Test
        @DisplayName("should return default message when no field errors present")
        void shouldReturnDefaultMessageWhenNoFieldErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().error()).isEqualTo("Validation failed");
        }
    }

    // ---------------------------------------------------------------
    // IllegalArgumentException handling
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgumentTests {

        @Test
        @DisplayName("should return 400 for IllegalArgumentException")
        void shouldReturn400ForIllegalArgumentException() {
            IllegalArgumentException ex = new IllegalArgumentException("Author name must not be blank");
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_ARGUMENT");
            assertThat(response.getBody().error()).isEqualTo("Author name must not be blank");
        }

        @Test
        @DisplayName("should return 400 for IllegalArgumentException with null message")
        void shouldReturn400ForIllegalArgumentExceptionWithNullMessage() {
            IllegalArgumentException ex = new IllegalArgumentException((String) null);
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_ARGUMENT");
            assertThat(response.getBody().error()).isNull();
        }

        @Test
        @DisplayName("should return 400 for IllegalArgumentException with empty message")
        void shouldReturn400ForIllegalArgumentExceptionWithEmptyMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("");
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEmpty();
        }
    }
}
