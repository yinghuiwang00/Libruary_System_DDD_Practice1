package com.library.catalog.interfaces.rest;

import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.AuthorDTO;
import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.service.AuthorManagementService;
import com.library.shared.domain.model.AuthorId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/authors")
@Tag(name = "Authors", description = "Author management API")
public class AuthorController {

    private final AuthorManagementService authorManagementService;

    public AuthorController(AuthorManagementService authorManagementService) {
        this.authorManagementService = authorManagementService;
    }

    @PostMapping
    @Operation(summary = "Create a new author")
    public ResponseEntity<ApiResponse<AuthorDTO>> createAuthor(@RequestBody CreateAuthorRequest request) {
        Author author = authorManagementService.createAuthor(
            request.name(), request.biography(), request.birthDate(),
            request.deathDate(), request.nationality()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(AuthorDTO.from(author)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID")
    public ResponseEntity<ApiResponse<AuthorDTO>> getAuthor(@PathVariable String id) {
        Author author = authorManagementService.getAuthor(AuthorId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(AuthorDTO.from(author)));
    }

    @GetMapping
    @Operation(summary = "Get all authors")
    public ResponseEntity<ApiResponse<List<AuthorDTO>>> getAllAuthors() {
        List<AuthorDTO> authors = authorManagementService.getAllAuthors().stream()
            .map(AuthorDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(authors));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update author")
    public ResponseEntity<ApiResponse<AuthorDTO>> updateAuthor(
            @PathVariable String id, @RequestBody UpdateAuthorRequest request) {
        Author author = authorManagementService.updateAuthor(
            AuthorId.of(id), request.name(), request.nationality(),
            request.birthDate(), request.deathDate(), request.biography()
        );
        return ResponseEntity.ok(ApiResponse.ok(AuthorDTO.from(author)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete author")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable String id) {
        authorManagementService.deleteAuthor(AuthorId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record CreateAuthorRequest(
        String name, String biography, LocalDate birthDate,
        LocalDate deathDate, String nationality
    ) {}

    public record UpdateAuthorRequest(
        String name, String nationality, LocalDate birthDate,
        LocalDate deathDate, String biography
    ) {}
}
