package com.library.catalog.interfaces.rest;

import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.PublisherDTO;
import com.library.catalog.domain.model.Publisher;
import com.library.catalog.domain.service.PublisherManagementService;
import com.library.shared.domain.model.PublisherId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/publishers")
@Tag(name = "Publishers", description = "Publisher management API")
public class PublisherController {

    private final PublisherManagementService publisherManagementService;

    public PublisherController(PublisherManagementService publisherManagementService) {
        this.publisherManagementService = publisherManagementService;
    }

    @PostMapping
    @Operation(summary = "Create a new publisher")
    public ResponseEntity<ApiResponse<PublisherDTO>> createPublisher(@RequestBody CreatePublisherRequest request) {
        Publisher publisher = publisherManagementService.createPublisher(
            request.name(), request.description(), request.address(),
            request.phone(), request.email(), request.website());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(PublisherDTO.from(publisher)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update publisher")
    public ResponseEntity<ApiResponse<PublisherDTO>> updatePublisher(
            @PathVariable String id, @RequestBody UpdatePublisherRequest request) {
        Publisher publisher = publisherManagementService.updatePublisher(
            PublisherId.of(id), request.name(), request.description(),
            request.address(), request.phone(), request.email(), request.website());
        return ResponseEntity.ok(ApiResponse.ok(PublisherDTO.from(publisher)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get publisher by ID")
    public ResponseEntity<ApiResponse<PublisherDTO>> getPublisher(@PathVariable String id) {
        Publisher publisher = publisherManagementService.getPublisher(PublisherId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(PublisherDTO.from(publisher)));
    }

    @GetMapping
    @Operation(summary = "Get all publishers")
    public ResponseEntity<ApiResponse<List<PublisherDTO>>> getAllPublishers() {
        List<PublisherDTO> publishers = publisherManagementService.getAllPublishers().stream()
            .map(PublisherDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(publishers));
    }

    @GetMapping("/search")
    @Operation(summary = "Search publishers by name")
    public ResponseEntity<ApiResponse<Page<PublisherDTO>>> searchPublishers(
            @RequestParam String name, Pageable pageable) {
        Page<PublisherDTO> results = publisherManagementService.searchPublishers(name, pageable)
            .map(PublisherDTO::from);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete publisher")
    public ResponseEntity<ApiResponse<Void>> deletePublisher(@PathVariable String id) {
        publisherManagementService.deletePublisher(PublisherId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record CreatePublisherRequest(
        String name, String description, String address,
        String phone, String email, String website
    ) {}

    public record UpdatePublisherRequest(
        String name, String description, String address,
        String phone, String email, String website
    ) {}
}
