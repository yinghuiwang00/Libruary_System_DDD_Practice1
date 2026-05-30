package com.library.circulation.domain.service;

import com.library.circulation.domain.event.*;
import com.library.circulation.domain.exception.*;
import com.library.circulation.domain.model.*;
import com.library.circulation.domain.model.enums.HoldStatus;
import com.library.circulation.domain.model.enums.LoanStatus;
import com.library.circulation.domain.repository.HoldRepository;
import com.library.circulation.domain.repository.LoanRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CirculationManagementService {

    private final LoanRepository loanRepository;
    private final HoldRepository holdRepository;
    private final DomainEventPublisher eventPublisher;

    public CirculationManagementService(LoanRepository loanRepository,
                                        HoldRepository holdRepository,
                                        DomainEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.holdRepository = holdRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Loan borrowBook(CopyId copyId, PatronId patronId, BookId bookId,
                           CirculationPolicy policy) {
        LocalDateTime now = LocalDateTime.now();

        Loan loan = Loan.create(copyId, patronId, bookId, now, policy);
        Loan savedLoan = loanRepository.save(loan);

        eventPublisher.publish(new BookBorrowedEvent(
            savedLoan.getId(), copyId, patronId, bookId, now, savedLoan.getDueDate()
        ));

        return savedLoan;
    }

    @Transactional
    public Loan returnBook(LoanId loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new InvalidOperationException("Loan already returned: " + loanId);
        }

        LocalDateTime returnDate = LocalDateTime.now();
        loan.returnBook(returnDate);
        Loan savedLoan = loanRepository.save(loan);

        eventPublisher.publish(new BookReturnedEvent(
            savedLoan.getId(), savedLoan.getCopyId(), savedLoan.getPatronId(),
            savedLoan.getBookId(), returnDate,
            savedLoan.getFine() != null ? savedLoan.getFine().getAmount() : null
        ));

        if (savedLoan.getFine() != null) {
            eventPublisher.publish(new FineIncurredEvent(
                savedLoan.getFine().getId(), savedLoan.getId(), savedLoan.getPatronId(),
                savedLoan.getFine().getAmount(), savedLoan.getFine().getOverdueDays(), returnDate
            ));
        }

        processNextHold(savedLoan.getBookId());

        return savedLoan;
    }

    @Transactional
    public Loan renewLoan(LoanId loanId, CirculationPolicy policy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        boolean hasWaitingHolds = holdRepository.existsByBookIdAndPatronIdAndStatusIn(
            loan.getBookId(), null, List.of(HoldStatus.WAITING)
        );

        // Check if there are any waiting holds for this book
        long waitingHoldCount = holdRepository.countByBookIdAndStatus(loan.getBookId(), HoldStatus.WAITING);
        if (waitingHoldCount > 0) {
            throw new LoanRenewalException("Cannot renew loan with pending holds on the book");
        }

        LocalDateTime oldDueDate = loan.getDueDate();
        LocalDateTime renewalDate = LocalDateTime.now();
        loan.renew(renewalDate, policy);
        Loan savedLoan = loanRepository.save(loan);

        eventPublisher.publish(new LoanRenewedEvent(
            savedLoan.getId(), oldDueDate, savedLoan.getDueDate(),
            savedLoan.getRenewalCount(), renewalDate
        ));

        return savedLoan;
    }

    @Transactional
    public Hold placeHold(BookId bookId, PatronId patronId,
                          String pickupLibraryId, CirculationPolicy policy) {
        boolean hasExistingHold = holdRepository.existsByBookIdAndPatronIdAndStatusIn(
            bookId, patronId, List.of(HoldStatus.WAITING, HoldStatus.READY_FOR_PICKUP)
        );
        if (hasExistingHold) {
            throw new DuplicateHoldException(bookId, patronId);
        }

        long waitingCount = holdRepository.countByBookIdAndStatus(bookId, HoldStatus.WAITING);
        int queuePosition = (int) waitingCount + 1;

        LocalDateTime now = LocalDateTime.now();
        Hold hold = Hold.create(bookId, patronId, now, queuePosition, policy.getHoldExpirationDays());
        hold.setPickupLibraryId(pickupLibraryId);
        Hold savedHold = holdRepository.save(hold);

        eventPublisher.publish(new HoldPlacedEvent(
            savedHold.getId(), bookId, patronId, queuePosition, now
        ));

        return savedHold;
    }

    @Transactional
    public void cancelHold(HoldId holdId, String reason) {
        Hold hold = holdRepository.findById(holdId)
            .orElseThrow(() -> new HoldNotFoundException(holdId));

        hold.cancel(reason);
        holdRepository.save(hold);

        updateQueuePositions(hold.getBookId());

        eventPublisher.publish(new HoldCancelledEvent(
            holdId, hold.getBookId(), hold.getPatronId(), reason, LocalDateTime.now()
        ));
    }

    @Transactional
    public void recallBook(LoanId loanId, String reason, CirculationPolicy policy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        LocalDateTime recallDueDate = LocalDateTime.now().plusDays(policy.getRecallNoticeDays());
        loan.recall(recallDueDate, reason);
        loanRepository.save(loan);

        eventPublisher.publish(new LoanRecalledEvent(
            loanId, recallDueDate, reason, LocalDateTime.now()
        ));
    }

    @Transactional
    public void cancelLoan(LoanId loanId, String reason) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        loan.cancel(reason);
        Loan savedLoan = loanRepository.save(loan);

        eventPublisher.publish(new LoanCancelledEvent(
            savedLoan.getId(), savedLoan.getPatronId(), savedLoan.getCopyId(), reason
        ));
    }

    @Transactional
    public void fulfillHold(HoldId holdId, CopyId copyId, CirculationPolicy policy) {
        Hold hold = holdRepository.findById(holdId)
            .orElseThrow(() -> new HoldNotFoundException(holdId));

        LocalDateTime now = LocalDateTime.now();
        hold.fulfill(copyId, now, policy.getHoldPickupDays());
        holdRepository.save(hold);

        eventPublisher.publish(new HoldFulfilledEvent(
            holdId, hold.getBookId(), hold.getPatronId(), copyId,
            hold.getAvailableUntilDate(), now
        ));
    }

    @Transactional
    public void pickupHold(HoldId holdId, CopyId copyId) {
        Hold hold = holdRepository.findById(holdId)
            .orElseThrow(() -> new HoldNotFoundException(holdId));

        hold.markAsPickedUp(LocalDateTime.now());
        holdRepository.save(hold);

        eventPublisher.publish(new HoldPickedUpEvent(
            holdId, hold.getPatronId(), hold.getBookId(), copyId
        ));
    }

    @Transactional
    public void processExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();

        List<Hold> expiredWaiting = holdRepository.findByStatusAndExpirationDateBefore(
            HoldStatus.WAITING, now
        );
        for (Hold hold : expiredWaiting) {
            hold.markAsExpired();
            holdRepository.save(hold);
            updateQueuePositions(hold.getBookId());
            eventPublisher.publish(new HoldExpiredEvent(
                hold.getId(), hold.getPatronId(), hold.getBookId()
            ));
        }

        List<Hold> expiredPickups = holdRepository.findByStatusAndAvailableUntilDateBefore(
            HoldStatus.READY_FOR_PICKUP, now
        );
        for (Hold hold : expiredPickups) {
            hold.markAsExpiredNotPickedUp();
            holdRepository.save(hold);
            processNextHold(hold.getBookId());
            eventPublisher.publish(new HoldExpiredNotPickedUpEvent(
                hold.getId(), hold.getPatronId(), hold.getBookId()
            ));
        }
    }

    @Transactional
    public void processOverdueLoans() {
        LocalDateTime now = LocalDateTime.now();
        List<Loan> overdueLoans = loanRepository.findByDueDateBeforeAndStatusIn(
            now, List.of(LoanStatus.ACTIVE, LoanStatus.RENEWED));
        for (Loan loan : overdueLoans) {
            loan.markOverdue();
            loanRepository.save(loan);
            long daysOverdue = loan.getOverdueDays(now);
            eventPublisher.publish(new OverdueNoticeEvent(
                loan.getId(), loan.getPatronId(), loan.getCopyId(), daysOverdue
            ));
        }
    }

    @Transactional
    public void sendDueDateReminders(int daysBeforeDue) {
        LocalDateTime now = LocalDateTime.now();
        List<Loan> activeLoans = loanRepository.findByDueDateBeforeAndStatusIn(
            now.plusDays(daysBeforeDue + 1), List.of(LoanStatus.ACTIVE, LoanStatus.RENEWED));
        for (Loan loan : activeLoans) {
            if (loan.isDueSoon(now, daysBeforeDue) && !Boolean.TRUE.equals(loan.getReminderSent())) {
                eventPublisher.publish(new DueDateReminderEvent(
                    loan.getId(), loan.getPatronId(), loan.getCopyId(), loan.getDueDate()
                ));
                loan.markReminderSent();
                loanRepository.save(loan);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Loan> getActiveLoans(PatronId patronId) {
        return loanRepository.findByPatronIdAndStatus(patronId, LoanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoanHistory(PatronId patronId) {
        return loanRepository.findByPatronId(patronId);
    }

    @Transactional(readOnly = true)
    public List<Hold> getPatronHolds(PatronId patronId) {
        return holdRepository.findByPatronId(patronId);
    }

    @Transactional(readOnly = true)
    public Optional<Loan> findLoanById(LoanId loanId) {
        return loanRepository.findById(loanId);
    }

    @Transactional(readOnly = true)
    public Optional<Hold> findHoldById(HoldId holdId) {
        return holdRepository.findById(holdId);
    }

    @Transactional(readOnly = true)
    public List<Hold> getBookHoldQueue(BookId bookId) {
        return holdRepository.findByBookIdAndStatus(bookId, HoldStatus.WAITING);
    }

    @Transactional(readOnly = true)
    public List<Loan> getAllLoansByStatus(LoanStatus status) {
        return loanRepository.findByStatus(status);
    }

    private void processNextHold(BookId bookId) {
        Optional<Hold> nextHold = holdRepository.findFirstByBookIdAndStatusOrderByQueuePositionAsc(
            bookId, HoldStatus.WAITING
        );
        // The actual copy fulfillment is done by the caller who has access to inventory
        // This just publishes the event for notification purposes
    }

    private void updateQueuePositions(BookId bookId) {
        List<Hold> waitingHolds = holdRepository.findByBookIdAndStatus(bookId, HoldStatus.WAITING);
        for (int i = 0; i < waitingHolds.size(); i++) {
            Hold hold = waitingHolds.get(i);
            if (hold.getQueuePosition() != (i + 1)) {
                hold.updateQueuePosition(i + 1);
                holdRepository.save(hold);
            }
        }
    }
}
