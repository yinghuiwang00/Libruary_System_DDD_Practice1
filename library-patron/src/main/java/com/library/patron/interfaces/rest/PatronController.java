package com.library.patron.interfaces.rest;

import com.library.patron.application.command.*;
import com.library.patron.application.dto.ApiResponse;
import com.library.patron.application.dto.PatronDTO;
import com.library.patron.application.service.PatronApplicationService;
import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.model.PatronId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patrons")
@Tag(name = "Patron Management", description = "APIs for managing library patrons")
public class PatronController {

    private final PatronApplicationService patronService;

    public PatronController(PatronApplicationService patronService) {
        this.patronService = patronService;
    }

    @PostMapping
    @Operation(summary = "Register a new patron")
    public ResponseEntity<ApiResponse<PatronDTO>> registerPatron(@Valid @RequestBody RegisterPatronCommand command) {
        PatronDTO patron = patronService.registerPatron(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(patron));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patron by ID")
    public ResponseEntity<ApiResponse<PatronDTO>> getPatron(@PathVariable String id) {
        PatronDTO patron = patronService.getPatron(PatronId.of(id));
        return ResponseEntity.ok(ApiResponse.success(patron));
    }

    @GetMapping
    @Operation(summary = "List all patrons")
    public ResponseEntity<ApiResponse<List<PatronDTO>>> getAllPatrons() {
        List<PatronDTO> patrons = patronService.getAllPatrons();
        return ResponseEntity.ok(ApiResponse.success(patrons));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patron information")
    public ResponseEntity<ApiResponse<PatronDTO>> updatePatron(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        UpdatePatronCommand command = new UpdatePatronCommand(
            PatronId.of(id),
            body.get("firstName"),
            body.get("lastName"),
            body.get("email"),
            body.get("phone"),
            body.get("address"),
            body.get("city"),
            body.get("postalCode")
        );
        PatronDTO patron = patronService.updatePatron(command);
        return ResponseEntity.ok(ApiResponse.success(patron));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a patron")
    public ResponseEntity<ApiResponse<Void>> suspendPatron(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        SuspendPatronCommand command = new SuspendPatronCommand(PatronId.of(id), reason);
        patronService.suspendPatron(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a suspended patron")
    public ResponseEntity<ApiResponse<Void>> reactivatePatron(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        ReactivatePatronCommand command = new ReactivatePatronCommand(PatronId.of(id), reason);
        patronService.reactivatePatron(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate a patron membership")
    public ResponseEntity<ApiResponse<Void>> terminatePatron(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        TerminatePatronCommand command = new TerminatePatronCommand(PatronId.of(id), reason);
        patronService.terminatePatron(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/extend-membership")
    @Operation(summary = "Extend patron membership")
    public ResponseEntity<ApiResponse<Void>> extendMembership(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        int months = ((Number) body.get("months")).intValue();
        String reason = (String) body.get("reason");
        ExtendMembershipCommand command = new ExtendMembershipCommand(PatronId.of(id), months, reason);
        patronService.extendMembership(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/fines")
    @Operation(summary = "Add a fine to a patron")
    public ResponseEntity<ApiResponse<Void>> addFine(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = (String) body.get("reason");
        AddFineCommand command = new AddFineCommand(PatronId.of(id), amount, reason);
        patronService.addFine(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/fines/pay")
    @Operation(summary = "Pay a patron fine")
    public ResponseEntity<ApiResponse<Void>> payFine(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        PayFineCommand command = new PayFineCommand(PatronId.of(id), amount);
        patronService.payFine(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/fines/waive")
    @Operation(summary = "Waive a patron fine")
    public ResponseEntity<ApiResponse<Void>> waiveFine(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = (String) body.get("reason");
        WaiveFineCommand command = new WaiveFineCommand(PatronId.of(id), amount, reason);
        patronService.waiveFine(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/type")
    @Operation(summary = "Change patron type")
    public ResponseEntity<ApiResponse<Void>> changePatronType(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        PatronType newType = PatronType.valueOf(body.get("patronType"));
        ChangePatronTypeCommand command = new ChangePatronTypeCommand(PatronId.of(id), newType);
        patronService.changePatronType(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
