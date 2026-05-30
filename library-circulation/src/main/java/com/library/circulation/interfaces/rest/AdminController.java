package com.library.circulation.interfaces.rest;

import com.library.circulation.application.dto.ApiResponse;
import com.library.circulation.application.dto.LoanDTO;
import com.library.circulation.application.service.CirculationApplicationService;
import com.library.circulation.domain.model.Loan;
import com.library.circulation.domain.service.CirculationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circulation/admin")
@Tag(name = "Admin Operations", description = "Administrative APIs for circulation management")
public class AdminController {

    private final CirculationManagementService circulationManagementService;
    private final CirculationApplicationService circulationApplicationService;

    public AdminController(CirculationManagementService circulationManagementService,
                           CirculationApplicationService circulationApplicationService) {
        this.circulationManagementService = circulationManagementService;
        this.circulationApplicationService = circulationApplicationService;
    }

    @PostMapping("/process-overdue")
    @Operation(summary = "Process overdue loans")
    public ResponseEntity<ApiResponse<List<LoanDTO>>> processOverdueLoans() {
        circulationManagementService.processOverdueLoans();
        List<LoanDTO> overdueLoans = circulationApplicationService.getAllLoansByStatus("OVERDUE");
        return ResponseEntity.ok(ApiResponse.success(overdueLoans));
    }
}
