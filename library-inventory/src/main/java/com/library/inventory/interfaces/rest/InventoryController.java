package com.library.inventory.interfaces.rest;

import com.library.inventory.application.command.AddCopyCommand;
import com.library.inventory.application.command.BatchAddCopiesCommand;
import com.library.inventory.application.command.CreateInventoryCommand;
import com.library.inventory.application.dto.ApiResponse;
import com.library.inventory.application.dto.BookCopyDTO;
import com.library.inventory.application.dto.CopyInventoryDTO;
import com.library.inventory.application.service.InventoryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryApplicationService inventoryService;

    public InventoryController(InventoryApplicationService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inventories")
    @Operation(summary = "Create initial inventory for a book in a library")
    public ResponseEntity<ApiResponse<CopyInventoryDTO>> createInventory(
            @Valid @RequestBody CreateInventoryCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(inventoryService.createInventory(command)));
    }

    @PostMapping("/inventories/{inventoryId}/copies")
    @Operation(summary = "Add a copy to an inventory")
    public ResponseEntity<ApiResponse<BookCopyDTO>> addCopy(
            @PathVariable String inventoryId,
            @Valid @RequestBody AddCopyCommand command) {
        command.setInventoryId(inventoryId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(inventoryService.addCopy(command)));
    }

    @PostMapping("/inventories/{inventoryId}/copies/batch")
    @Operation(summary = "Batch add copies to an inventory")
    public ResponseEntity<ApiResponse<List<BookCopyDTO>>> batchAddCopies(
            @PathVariable String inventoryId,
            @Valid @RequestBody BatchAddCopiesCommand command) {
        command.setInventoryId(inventoryId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(inventoryService.batchAddCopies(command)));
    }

    @PostMapping("/copies/{copyId}/checkout")
    @Operation(summary = "Checkout (borrow) a copy")
    public ResponseEntity<ApiResponse<Void>> checkoutCopy(@PathVariable String copyId) {
        inventoryService.checkoutCopy(copyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/copies/{copyId}/return")
    @Operation(summary = "Return a copy")
    public ResponseEntity<ApiResponse<Void>> returnCopy(@PathVariable String copyId) {
        inventoryService.returnCopy(copyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/copies/{copyId}/damage")
    @Operation(summary = "Report a damaged copy")
    public ResponseEntity<ApiResponse<Void>> reportDamage(
            @PathVariable String copyId,
            @RequestBody DamageReportRequest request) {
        inventoryService.reportDamage(copyId, request.getDescription());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/copies/{copyId}/loss")
    @Operation(summary = "Report a lost copy")
    public ResponseEntity<ApiResponse<Void>> reportLoss(
            @PathVariable String copyId,
            @RequestBody LossReportRequest request) {
        inventoryService.reportLoss(copyId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/inventories/{inventoryId}")
    @Operation(summary = "Get inventory details")
    public ResponseEntity<ApiResponse<CopyInventoryDTO>> getInventory(
            @PathVariable String inventoryId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(inventoryId)));
    }

    @GetMapping("/books/{bookId}/overview")
    @Operation(summary = "Get inventory overview for a book across all libraries")
    public ResponseEntity<ApiResponse<List<CopyInventoryDTO>>> getInventoryOverview(
            @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryOverview(bookId)));
    }

    public static class DamageReportRequest {
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class LossReportRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
