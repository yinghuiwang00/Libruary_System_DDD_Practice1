package com.library.circulation.interfaces.rest;

import com.library.circulation.application.command.CancelHoldCommand;
import com.library.circulation.application.command.PlaceHoldCommand;
import com.library.circulation.application.dto.ApiResponse;
import com.library.circulation.application.dto.HoldDTO;
import com.library.circulation.application.service.CirculationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circulation")
@Tag(name = "Hold Management", description = "APIs for managing book holds")
public class HoldController {

    private final CirculationApplicationService circulationService;

    public HoldController(CirculationApplicationService circulationService) {
        this.circulationService = circulationService;
    }

    @PostMapping("/holds")
    @Operation(summary = "Place a hold on a book")
    public ResponseEntity<ApiResponse<HoldDTO>> placeHold(@Valid @RequestBody PlaceHoldCommand command) {
        HoldDTO hold = circulationService.placeHold(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(hold));
    }

    @DeleteMapping("/holds/{holdId}")
    @Operation(summary = "Cancel a hold")
    public ResponseEntity<ApiResponse<Void>> cancelHold(
            @PathVariable String holdId,
            @RequestParam(required = false) String reason) {
        CancelHoldCommand command = new CancelHoldCommand(holdId, reason);
        circulationService.cancelHold(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/holds/{holdId}")
    @Operation(summary = "Get hold details")
    public ResponseEntity<ApiResponse<HoldDTO>> getHold(@PathVariable String holdId) {
        HoldDTO hold = circulationService.getHoldById(holdId);
        if (hold == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(hold));
    }

    @GetMapping("/patrons/{patronId}/holds")
    @Operation(summary = "Get holds for a patron")
    public ResponseEntity<ApiResponse<List<HoldDTO>>> getPatronHolds(@PathVariable String patronId) {
        List<HoldDTO> holds = circulationService.getPatronHolds(patronId);
        return ResponseEntity.ok(ApiResponse.success(holds));
    }

    @GetMapping("/books/{bookId}/holds")
    @Operation(summary = "Get hold queue for a book")
    public ResponseEntity<ApiResponse<List<HoldDTO>>> getBookHoldQueue(@PathVariable String bookId) {
        List<HoldDTO> holds = circulationService.getBookHoldQueue(bookId);
        return ResponseEntity.ok(ApiResponse.success(holds));
    }
}
