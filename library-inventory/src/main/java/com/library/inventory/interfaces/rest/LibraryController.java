package com.library.inventory.interfaces.rest;

import com.library.inventory.application.command.CreateLibraryCommand;
import com.library.inventory.application.dto.ApiResponse;
import com.library.inventory.application.dto.LibraryDTO;
import com.library.inventory.application.service.LibraryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/libraries")
@Tag(name = "Library", description = "Library management APIs")
public class LibraryController {

    private final LibraryApplicationService libraryService;

    public LibraryController(LibraryApplicationService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    @Operation(summary = "Create a new library")
    public ResponseEntity<ApiResponse<LibraryDTO>> createLibrary(
            @Valid @RequestBody CreateLibraryCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(libraryService.createLibrary(command)));
    }

    @GetMapping
    @Operation(summary = "Get all libraries")
    public ResponseEntity<ApiResponse<List<LibraryDTO>>> getAllLibraries() {
        return ResponseEntity.ok(ApiResponse.success(libraryService.getAllLibraries()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active libraries")
    public ResponseEntity<ApiResponse<List<LibraryDTO>>> getActiveLibraries() {
        return ResponseEntity.ok(ApiResponse.success(libraryService.getActiveLibraries()));
    }

    @GetMapping("/{libraryId}")
    @Operation(summary = "Get library by ID")
    public ResponseEntity<ApiResponse<LibraryDTO>> getLibrary(@PathVariable String libraryId) {
        return ResponseEntity.ok(ApiResponse.success(libraryService.getLibrary(libraryId)));
    }

    @PutMapping("/{libraryId}")
    @Operation(summary = "Update library")
    public ResponseEntity<ApiResponse<LibraryDTO>> updateLibrary(
            @PathVariable String libraryId,
            @Valid @RequestBody CreateLibraryCommand command) {
        return ResponseEntity.ok(ApiResponse.success(libraryService.updateLibrary(libraryId, command)));
    }

    @PostMapping("/{libraryId}/deactivate")
    @Operation(summary = "Deactivate a library")
    public ResponseEntity<ApiResponse<Void>> deactivateLibrary(@PathVariable String libraryId) {
        libraryService.deactivateLibrary(libraryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{libraryId}/activate")
    @Operation(summary = "Activate a library")
    public ResponseEntity<ApiResponse<Void>> activateLibrary(@PathVariable String libraryId) {
        libraryService.activateLibrary(libraryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
