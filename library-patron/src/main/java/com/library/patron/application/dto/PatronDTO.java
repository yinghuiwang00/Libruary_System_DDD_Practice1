package com.library.patron.application.dto;

import com.library.patron.domain.model.Patron;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatronDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String patronType;
    private String status;
    private LocalDate memberSince;
    private LocalDate membershipExpiry;
    private Integer currentLoans;
    private BigDecimal outstandingFines;
    private Integer totalBorrowed;
    private Integer maxLoans;
    private Integer loanPeriodDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PatronDTO() {
    }

    public static PatronDTO fromDomain(Patron patron) {
        PatronDTO dto = new PatronDTO();
        dto.id = patron.getId().getValue();
        dto.firstName = patron.getFirstName();
        dto.lastName = patron.getLastName();
        dto.email = patron.getEmail();
        dto.phone = patron.getPhone();
        dto.address = patron.getAddress();
        dto.city = patron.getCity();
        dto.postalCode = patron.getPostalCode();
        dto.patronType = patron.getPatronType().name();
        dto.status = patron.getStatus().name();
        dto.memberSince = patron.getMemberSince();
        dto.membershipExpiry = patron.getMembershipExpiry();
        dto.currentLoans = patron.getCurrentLoans();
        dto.outstandingFines = patron.getOutstandingFines();
        dto.totalBorrowed = patron.getTotalBorrowed();
        dto.maxLoans = patron.getBorrowingPrivilege() != null ? patron.getBorrowingPrivilege().getMaxLoans() : null;
        dto.loanPeriodDays = patron.getBorrowingPrivilege() != null ? patron.getBorrowingPrivilege().getLoanPeriodDays() : null;
        dto.createdAt = patron.getCreatedAt();
        dto.updatedAt = patron.getUpdatedAt();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getPatronType() { return patronType; }
    public void setPatronType(String patronType) { this.patronType = patronType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getMemberSince() { return memberSince; }
    public void setMemberSince(LocalDate memberSince) { this.memberSince = memberSince; }
    public LocalDate getMembershipExpiry() { return membershipExpiry; }
    public void setMembershipExpiry(LocalDate membershipExpiry) { this.membershipExpiry = membershipExpiry; }
    public Integer getCurrentLoans() { return currentLoans; }
    public void setCurrentLoans(Integer currentLoans) { this.currentLoans = currentLoans; }
    public BigDecimal getOutstandingFines() { return outstandingFines; }
    public void setOutstandingFines(BigDecimal outstandingFines) { this.outstandingFines = outstandingFines; }
    public Integer getTotalBorrowed() { return totalBorrowed; }
    public void setTotalBorrowed(Integer totalBorrowed) { this.totalBorrowed = totalBorrowed; }
    public Integer getMaxLoans() { return maxLoans; }
    public void setMaxLoans(Integer maxLoans) { this.maxLoans = maxLoans; }
    public Integer getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(Integer loanPeriodDays) { this.loanPeriodDays = loanPeriodDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
