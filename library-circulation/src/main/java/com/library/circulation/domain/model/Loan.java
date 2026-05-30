package com.library.circulation.domain.model;

import com.library.circulation.domain.exception.InvalidOperationException;
import com.library.circulation.domain.exception.LoanRenewalException;
import com.library.circulation.domain.model.enums.LoanStatus;
import com.library.shared.domain.model.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "loans", indexes = {
    @Index(name = "idx_loan_patron", columnList = "patron_id"),
    @Index(name = "idx_loan_copy", columnList = "copy_id"),
    @Index(name = "idx_loan_status", columnList = "status"),
    @Index(name = "idx_loan_due_date", columnList = "due_date")
})
public class Loan {

    @EmbeddedId
    private LoanId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "copy_id"))
    private CopyId copyId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "patron_id"))
    private PatronId patronId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "book_id"))
    private BookId bookId;

    @Column(name = "loan_date", nullable = false)
    private LocalDateTime loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Embedded
    private Fine fine;

    @Column(name = "renewal_count")
    private Integer renewalCount;

    @Column(name = "max_renewals_allowed")
    private Integer maxRenewalsAllowed;

    @Column(name = "recall_date")
    private LocalDateTime recallDate;

    @Column(name = "recall_reason", length = 500)
    private String recallReason;

    @Column(name = "reminder_sent")
    private Boolean reminderSent;

    @Column(name = "overdue_notice_sent")
    private Boolean overdueNoticeSent;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private CirculationPolicy circulationPolicy;

    protected Loan() {
    }

    public Loan(LoanId id, CopyId copyId, PatronId patronId, BookId bookId,
                LocalDateTime loanDate, LocalDateTime dueDate, CirculationPolicy policy) {
        this.id = Objects.requireNonNull(id, "Loan ID must not be null");
        this.copyId = Objects.requireNonNull(copyId, "Copy ID must not be null");
        this.patronId = Objects.requireNonNull(patronId, "Patron ID must not be null");
        this.bookId = Objects.requireNonNull(bookId, "Book ID must not be null");
        this.loanDate = Objects.requireNonNull(loanDate, "Loan date must not be null");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date must not be null");
        this.status = LoanStatus.ACTIVE;
        this.renewalCount = 0;
        this.maxRenewalsAllowed = policy.getMaxRenewalsAllowed();
        this.reminderSent = false;
        this.overdueNoticeSent = false;
        this.circulationPolicy = policy;
    }

    public static Loan create(CopyId copyId, PatronId patronId, BookId bookId,
                              LocalDateTime loanDate, CirculationPolicy policy) {
        LocalDateTime dueDate = loanDate.plusDays(policy.getLoanPeriodDays());
        return new Loan(LoanId.generate(), copyId, patronId, bookId, loanDate, dueDate, policy);
    }

    public void returnBook(LocalDateTime returnDate) {
        if (this.status != LoanStatus.ACTIVE && this.status != LoanStatus.OVERDUE && this.status != LoanStatus.RENEWED) {
            throw new InvalidOperationException("Cannot return loan in status: " + this.status);
        }
        this.returnDate = Objects.requireNonNull(returnDate, "Return date must not be null");
        if (returnDate.isAfter(this.dueDate)) {
            calculateFine();
        }
        this.status = LoanStatus.RETURNED;
    }

    public void renew(LocalDateTime renewalDate, CirculationPolicy policy) {
        if (this.status != LoanStatus.ACTIVE && this.status != LoanStatus.RENEWED) {
            throw new LoanRenewalException("Cannot renew loan in status: " + this.status);
        }
        if (isOverdue(renewalDate)) {
            throw new LoanRenewalException("Cannot renew overdue loan");
        }
        if (this.recallDate != null) {
            throw new LoanRenewalException("Cannot renew recalled loan");
        }
        if (this.renewalCount >= this.maxRenewalsAllowed) {
            throw new LoanRenewalException("Maximum renewals reached: " + this.maxRenewalsAllowed);
        }
        this.dueDate = this.dueDate.plusDays(policy.getLoanPeriodDays());
        this.renewalCount++;
        this.status = LoanStatus.RENEWED;
        this.circulationPolicy = policy;
    }

    public void recall(LocalDateTime recallDueDate, String reason) {
        if (this.status != LoanStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot recall non-active loan");
        }
        this.recallDate = LocalDateTime.now();
        this.recallReason = reason;
        if (this.dueDate.isAfter(recallDueDate)) {
            this.dueDate = recallDueDate;
        }
    }

    public void markOverdue() {
        if (this.status != LoanStatus.ACTIVE && this.status != LoanStatus.RENEWED) {
            throw new InvalidOperationException("Can only mark active or renewed loan as overdue");
        }
        this.status = LoanStatus.OVERDUE;
    }

    public void markReminderSent() {
        this.reminderSent = true;
    }

    public void markOverdueNoticeSent() {
        this.overdueNoticeSent = true;
    }

    public void cancel(String reason) {
        if (this.status == LoanStatus.RETURNED) {
            throw new InvalidOperationException("Cannot cancel returned loan");
        }
        this.status = LoanStatus.CANCELLED;
    }

    public boolean isOverdue(LocalDateTime currentDate) {
        return (this.status == LoanStatus.ACTIVE || this.status == LoanStatus.OVERDUE || this.status == LoanStatus.RENEWED)
                && currentDate.isAfter(this.dueDate);
    }

    public boolean isDueSoon(LocalDateTime currentDate, int daysBeforeDue) {
        return (this.status == LoanStatus.ACTIVE || this.status == LoanStatus.RENEWED)
                && !currentDate.isAfter(this.dueDate)
                && currentDate.plusDays(daysBeforeDue).isAfter(this.dueDate);
    }

    public boolean isRecalled() {
        return this.recallDate != null;
    }

    public long getOverdueDays(LocalDateTime currentDate) {
        if (!isOverdue(currentDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(this.dueDate, currentDate);
    }

    public BigDecimal getCurrentFine(LocalDateTime currentDate) {
        if (this.fine != null) {
            return this.fine.getAmount();
        }
        if (isOverdue(currentDate) && this.circulationPolicy != null) {
            long days = getOverdueDays(currentDate);
            return this.circulationPolicy.calculateFine((int) days);
        }
        return BigDecimal.ZERO;
    }

    public boolean canBeRenewed(LocalDateTime currentDate, CirculationPolicy policy) {
        return (this.status == LoanStatus.ACTIVE || this.status == LoanStatus.RENEWED)
                && !isOverdue(currentDate)
                && this.recallDate == null
                && this.renewalCount < this.maxRenewalsAllowed;
    }

    private void calculateFine() {
        if (this.returnDate == null || !this.returnDate.isAfter(this.dueDate)) {
            return;
        }
        long overdueDays = ChronoUnit.DAYS.between(this.dueDate, this.returnDate);
        BigDecimal fineAmount;
        if (this.circulationPolicy != null) {
            fineAmount = this.circulationPolicy.calculateFine((int) overdueDays);
        } else {
            fineAmount = BigDecimal.ZERO;
        }
        this.fine = new Fine(FineId.generate(), fineAmount, (int) overdueDays, LocalDateTime.now());
    }

    public void setCirculationPolicy(CirculationPolicy policy) {
        this.circulationPolicy = policy;
    }

    public LoanId getId() { return id; }
    public CopyId getCopyId() { return copyId; }
    public PatronId getPatronId() { return patronId; }
    public BookId getBookId() { return bookId; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }
    public Fine getFine() { return fine; }
    public Integer getRenewalCount() { return renewalCount; }
    public Integer getMaxRenewalsAllowed() { return maxRenewalsAllowed; }
    public LocalDateTime getRecallDate() { return recallDate; }
    public String getRecallReason() { return recallReason; }
    public Boolean getReminderSent() { return reminderSent; }
    public Boolean getOverdueNoticeSent() { return overdueNoticeSent; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
