package com.library.circulation.interfaces.rest;

import com.library.circulation.application.command.BorrowBookCommand;
import com.library.circulation.application.command.RecallBookCommand;
import com.library.circulation.application.command.RenewLoanCommand;
import com.library.circulation.application.command.ReturnBookCommand;
import com.library.circulation.application.dto.ApiResponse;
import com.library.circulation.application.dto.LoanDTO;
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
@Tag(name = "Loan Management", description = "APIs for managing book loans")
public class LoanController {

    private final CirculationApplicationService circulationService;

    public LoanController(CirculationApplicationService circulationService) {
        this.circulationService = circulationService;
    }

    @PostMapping("/loans")
    @Operation(summary = "Borrow a book")
    public ResponseEntity<ApiResponse<LoanDTO>> borrowBook(@Valid @RequestBody BorrowBookCommand command) {
        LoanDTO loan = circulationService.borrowBook(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(loan));
    }

    @PostMapping("/loans/{loanId}/return")
    @Operation(summary = "Return a book")
    public ResponseEntity<ApiResponse<LoanDTO>> returnBook(@PathVariable String loanId) {
        ReturnBookCommand command = new ReturnBookCommand(loanId);
        LoanDTO loan = circulationService.returnBook(command);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @PostMapping("/loans/{loanId}/renew")
    @Operation(summary = "Renew a loan")
    public ResponseEntity<ApiResponse<LoanDTO>> renewLoan(@PathVariable String loanId) {
        RenewLoanCommand command = new RenewLoanCommand(loanId);
        LoanDTO loan = circulationService.renewLoan(command);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @PostMapping("/loans/{loanId}/recall")
    @Operation(summary = "Recall a book")
    public ResponseEntity<ApiResponse<LoanDTO>> recallBook(
            @PathVariable String loanId,
            @RequestParam(required = false) String reason) {
        RecallBookCommand command = new RecallBookCommand(loanId, reason);
        LoanDTO loan = circulationService.recallBook(command);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @GetMapping("/loans/{loanId}")
    @Operation(summary = "Get loan details")
    public ResponseEntity<ApiResponse<LoanDTO>> getLoan(@PathVariable String loanId) {
        LoanDTO loan = circulationService.getLoanById(loanId);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @GetMapping("/patrons/{patronId}/loans")
    @Operation(summary = "Get active loans for a patron")
    public ResponseEntity<ApiResponse<List<LoanDTO>>> getActiveLoans(@PathVariable String patronId) {
        List<LoanDTO> loans = circulationService.getActiveLoans(patronId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/patrons/{patronId}/loans/history")
    @Operation(summary = "Get loan history for a patron")
    public ResponseEntity<ApiResponse<List<LoanDTO>>> getLoanHistory(@PathVariable String patronId) {
        List<LoanDTO> loans = circulationService.getLoanHistory(patronId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }
}
