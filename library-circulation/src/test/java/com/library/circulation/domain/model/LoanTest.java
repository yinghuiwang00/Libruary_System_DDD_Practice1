package com.library.circulation.domain.model;

import com.library.circulation.domain.exception.InvalidOperationException;
import com.library.circulation.domain.exception.LoanRenewalException;
import com.library.circulation.domain.model.enums.LoanStatus;
import com.library.shared.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LoanTest {

    private final CirculationPolicy policy = CirculationPolicy.standard();

    private Loan createActiveLoan() {
        LocalDateTime now = LocalDateTime.now();
        return new Loan(LoanId.generate(), CopyId.generate(), PatronId.generate(),
                BookId.generate(), now, now.plusDays(30), policy);
    }

    @Test
    void shouldCreateLoanWithActiveStatus() {
        Loan loan = createActiveLoan();
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(0, loan.getRenewalCount());
        assertFalse(loan.isOverdue(LocalDateTime.now()));
        assertFalse(loan.isRecalled());
    }

    @Test
    void shouldReturnBookOnTime() {
        Loan loan = createActiveLoan();
        LocalDateTime returnDate = loan.getDueDate().minusDays(1);
        loan.returnBook(returnDate);
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertEquals(returnDate, loan.getReturnDate());
        assertNull(loan.getFine());
    }

    @Test
    void shouldCalculateFineWhenOverdue() {
        Loan loan = createActiveLoan();
        LocalDateTime returnDate = loan.getDueDate().plusDays(5);
        loan.returnBook(returnDate);
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertNotNull(loan.getFine());
        assertTrue(loan.getFine().getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(5, loan.getFine().getOverdueDays());
    }

    @Test
    void shouldRespectMaxFineCap() {
        Loan loan = createActiveLoan();
        LocalDateTime returnDate = loan.getDueDate().plusDays(200);
        loan.returnBook(returnDate);
        assertNotNull(loan.getFine());
        assertTrue(loan.getFine().getAmount().compareTo(policy.getMaxFineAmount()) <= 0);
    }

    @Test
    void shouldRenewLoanSuccessfully() {
        Loan loan = createActiveLoan();
        LocalDateTime oldDueDate = loan.getDueDate();
        loan.renew(LocalDateTime.now(), policy);
        assertEquals(1, loan.getRenewalCount());
        assertTrue(loan.getDueDate().isAfter(oldDueDate));
        assertEquals(LoanStatus.RENEWED, loan.getStatus());
    }

    @Test
    void shouldThrowWhenRenewingOverdueLoan() {
        Loan loan = createActiveLoan();
        assertThrows(LoanRenewalException.class,
                () -> loan.renew(loan.getDueDate().plusDays(1), policy));
    }

    @Test
    void shouldThrowWhenMaxRenewalsReached() {
        Loan loan = createActiveLoan();
        loan.renew(LocalDateTime.now(), policy);
        loan.renew(LocalDateTime.now(), policy);
        assertThrows(LoanRenewalException.class,
                () -> loan.renew(LocalDateTime.now(), policy));
    }

    @Test
    void shouldThrowWhenRenewingRecalledLoan() {
        Loan loan = createActiveLoan();
        loan.recall(LocalDateTime.now().plusDays(7), "needed");
        assertThrows(LoanRenewalException.class,
                () -> loan.renew(LocalDateTime.now(), policy));
    }

    @Test
    void shouldRecallActiveLoan() {
        Loan loan = createActiveLoan();
        LocalDateTime recallDue = LocalDateTime.now().plusDays(7);
        loan.recall(recallDue, "Another patron needs it");
        assertTrue(loan.isRecalled());
        assertTrue(loan.getDueDate().isBefore(recallDue) || loan.getDueDate().equals(recallDue));
    }

    @Test
    void shouldThrowWhenRecallingNonActiveLoan() {
        Loan loan = createActiveLoan();
        loan.returnBook(LocalDateTime.now());
        assertThrows(InvalidOperationException.class,
                () -> loan.recall(LocalDateTime.now().plusDays(7), "reason"));
    }

    @Test
    void shouldMarkOverdue() {
        Loan loan = createActiveLoan();
        loan.markOverdue();
        assertEquals(LoanStatus.OVERDUE, loan.getStatus());
    }

    @Test
    void shouldDetectOverdue() {
        Loan loan = createActiveLoan();
        assertFalse(loan.isOverdue(LocalDateTime.now()));
        assertTrue(loan.isOverdue(loan.getDueDate().plusDays(1)));
    }

    @Test
    void shouldDetectDueSoon() {
        Loan loan = createActiveLoan();
        assertFalse(loan.isDueSoon(LocalDateTime.now(), 3));
        assertTrue(loan.isDueSoon(loan.getDueDate().minusDays(2), 3));
    }

    @Test
    void shouldCalculateOverdueDays() {
        Loan loan = createActiveLoan();
        assertEquals(0, loan.getOverdueDays(LocalDateTime.now()));
        assertEquals(5, loan.getOverdueDays(loan.getDueDate().plusDays(5)));
    }

    @Test
    void shouldReturnBookFromOverdueStatus() {
        Loan loan = createActiveLoan();
        loan.markOverdue();
        loan.returnBook(loan.getDueDate().plusDays(3));
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertNotNull(loan.getFine());
    }

    @Test
    void shouldThrowWhenReturningAlreadyReturned() {
        Loan loan = createActiveLoan();
        loan.returnBook(LocalDateTime.now());
        assertThrows(InvalidOperationException.class,
                () -> loan.returnBook(LocalDateTime.now()));
    }

    @Test
    void shouldCancelLoan() {
        Loan loan = createActiveLoan();
        loan.cancel("mistake");
        assertEquals(LoanStatus.CANCELLED, loan.getStatus());
    }

    @Test
    void shouldThrowWhenCancellingReturnedLoan() {
        Loan loan = createActiveLoan();
        loan.returnBook(LocalDateTime.now());
        assertThrows(InvalidOperationException.class, () -> loan.cancel("mistake"));
    }

    @Test
    void shouldCheckCanBeRenewed() {
        Loan loan = createActiveLoan();
        assertTrue(loan.canBeRenewed(LocalDateTime.now(), policy));
    }

    @Test
    void shouldCreateLoanViaFactoryMethod() {
        Loan loan = Loan.create(CopyId.generate(), PatronId.generate(), BookId.generate(),
                LocalDateTime.now(), policy);
        assertNotNull(loan.getId());
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
    }
}
