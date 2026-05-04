package com.library.catalog.interfaces.rest;

import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        HttpStatus status = mapToHttpStatus(ex);
        return ResponseEntity.status(status)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("INVALID_ARGUMENT", ex.getMessage()));
    }

    private HttpStatus mapToHttpStatus(DomainException ex) {
        String code = ex.getErrorCode();
        if (code != null) {
            if (code.contains("NOT_FOUND")) return HttpStatus.NOT_FOUND;
            if (code.contains("DUPLICATE")) return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
