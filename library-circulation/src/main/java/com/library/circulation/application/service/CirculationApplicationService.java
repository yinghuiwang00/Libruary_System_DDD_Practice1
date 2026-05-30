package com.library.circulation.application.service;

import com.library.circulation.application.command.*;
import com.library.circulation.application.dto.HoldDTO;
import com.library.circulation.application.dto.LoanDTO;
import com.library.circulation.domain.model.CirculationPolicy;
import com.library.circulation.domain.model.Hold;
import com.library.circulation.domain.model.Loan;
import com.library.circulation.domain.service.CirculationManagementService;
import com.library.shared.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CirculationApplicationService {

    private final CirculationManagementService circulationService;
    private final CirculationPolicy policy;

    public CirculationApplicationService(CirculationManagementService circulationService,
                                         CirculationPolicy policy) {
        this.circulationService = circulationService;
        this.policy = policy;
    }

    @Transactional
    public LoanDTO borrowBook(BorrowBookCommand command) {
        Loan loan = circulationService.borrowBook(
            CopyId.of(command.getCopyId()),
            PatronId.of(command.getPatronId()),
            BookId.of(command.getBookId()),
            policy
        );
        return LoanDTO.fromDomain(loan);
    }

    @Transactional
    public LoanDTO returnBook(ReturnBookCommand command) {
        Loan loan = circulationService.returnBook(LoanId.of(command.getLoanId()));
        return LoanDTO.fromDomain(loan);
    }

    @Transactional
    public LoanDTO renewLoan(RenewLoanCommand command) {
        Loan loan = circulationService.renewLoan(LoanId.of(command.getLoanId()), policy);
        return LoanDTO.fromDomain(loan);
    }

    @Transactional
    public HoldDTO placeHold(PlaceHoldCommand command) {
        Hold hold = circulationService.placeHold(
            BookId.of(command.getBookId()),
            PatronId.of(command.getPatronId()),
            command.getPickupLibraryId(),
            policy
        );
        return HoldDTO.fromDomain(hold);
    }

    @Transactional
    public void cancelHold(CancelHoldCommand command) {
        circulationService.cancelHold(HoldId.of(command.getHoldId()), command.getReason());
    }

    @Transactional
    public LoanDTO recallBook(RecallBookCommand command) {
        circulationService.recallBook(LoanId.of(command.getLoanId()), command.getReason(), policy);
        return circulationService.findLoanById(LoanId.of(command.getLoanId()))
            .map(LoanDTO::fromDomain)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getActiveLoans(String patronId) {
        return circulationService.getActiveLoans(PatronId.of(patronId)).stream()
            .map(LoanDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoanHistory(String patronId) {
        return circulationService.getLoanHistory(PatronId.of(patronId)).stream()
            .map(LoanDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HoldDTO> getPatronHolds(String patronId) {
        return circulationService.getPatronHolds(PatronId.of(patronId)).stream()
            .map(HoldDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanDTO getLoanById(String loanId) {
        return circulationService.findLoanById(LoanId.of(loanId))
            .map(LoanDTO::fromDomain)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public HoldDTO getHoldById(String holdId) {
        return circulationService.findHoldById(HoldId.of(holdId))
            .map(HoldDTO::fromDomain)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<HoldDTO> getBookHoldQueue(String bookId) {
        return circulationService.getBookHoldQueue(BookId.of(bookId)).stream()
            .map(HoldDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getAllLoansByStatus(String status) {
        return circulationService.getAllLoansByStatus(
            com.library.circulation.domain.model.enums.LoanStatus.valueOf(status)
        ).stream()
            .map(LoanDTO::fromDomain)
            .collect(Collectors.toList());
    }
}
