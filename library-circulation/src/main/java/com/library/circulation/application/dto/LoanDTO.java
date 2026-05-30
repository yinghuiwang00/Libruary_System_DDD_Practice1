package com.library.circulation.application.dto;

import com.library.circulation.domain.model.Loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanDTO {

    private String id;
    private String copyId;
    private String patronId;
    private String bookId;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;
    private Integer renewalCount;
    private BigDecimal fineAmount;
    private Boolean isRecalled;
    private LocalDateTime createdAt;

    public LoanDTO() {
    }

    public static LoanDTO fromDomain(Loan loan) {
        LoanDTO dto = new LoanDTO();
        dto.id = loan.getId().getValue();
        dto.copyId = loan.getCopyId().getValue();
        dto.patronId = loan.getPatronId().getValue();
        dto.bookId = loan.getBookId().getValue();
        dto.loanDate = loan.getLoanDate();
        dto.dueDate = loan.getDueDate();
        dto.returnDate = loan.getReturnDate();
        dto.status = loan.getStatus().name();
        dto.renewalCount = loan.getRenewalCount();
        dto.fineAmount = loan.getFine() != null ? loan.getFine().getAmount() : null;
        dto.isRecalled = loan.isRecalled();
        dto.createdAt = loan.getCreatedAt();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCopyId() { return copyId; }
    public void setCopyId(String copyId) { this.copyId = copyId; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRenewalCount() { return renewalCount; }
    public void setRenewalCount(Integer renewalCount) { this.renewalCount = renewalCount; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public Boolean getIsRecalled() { return isRecalled; }
    public void setIsRecalled(Boolean recalled) { isRecalled = recalled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
