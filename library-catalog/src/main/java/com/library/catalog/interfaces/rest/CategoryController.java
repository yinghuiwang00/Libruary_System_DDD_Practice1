package com.library.catalog.interfaces.rest;

import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.CategoryDTO;
import com.library.catalog.domain.model.Category;
import com.library.catalog.domain.service.CategoryManagementService;
import com.library.shared.domain.model.CategoryId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/categories")
@Tag(name = "Categories", description = "Category management API")
public class CategoryController {

    private final CategoryManagementService categoryManagementService;

    public CategoryController(CategoryManagementService categoryManagementService) {
        this.categoryManagementService = categoryManagementService;
    }

    @PostMapping
    @Operation(summary = "Create a root category")
    public ResponseEntity<ApiResponse<CategoryDTO>> createRootCategory(@RequestBody CreateCategoryRequest request) {
        Category category = categoryManagementService.createRootCategory(
            request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(CategoryDTO.from(category)));
    }

    @PostMapping("/{parentId}/children")
    @Operation(summary = "Add a child category")
    public ResponseEntity<ApiResponse<CategoryDTO>> addChildCategory(
            @PathVariable String parentId, @RequestBody CreateCategoryRequest request) {
        Category child = categoryManagementService.addChildCategory(
            CategoryId.of(parentId), request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(CategoryDTO.from(child)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable String id, @RequestBody UpdateCategoryRequest request) {
        Category category = categoryManagementService.updateCategory(
            CategoryId.of(id), request.name(), request.description());
        return ResponseEntity.ok(ApiResponse.ok(CategoryDTO.from(category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategory(@PathVariable String id) {
        Category category = categoryManagementService.getCategory(CategoryId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(CategoryDTO.from(category)));
    }

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        List<CategoryDTO> categories = categoryManagementService.getAllCategories().stream()
            .map(CategoryDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getRootCategories() {
        List<CategoryDTO> categories = categoryManagementService.getRootCategories().stream()
            .map(CategoryDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        categoryManagementService.deleteCategory(CategoryId.of(id));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record CreateCategoryRequest(String name, String description) {}

    public record UpdateCategoryRequest(String name, String description) {}
}
