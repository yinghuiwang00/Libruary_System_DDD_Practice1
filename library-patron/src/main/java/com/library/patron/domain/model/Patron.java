package com.library.patron.domain.model;

import com.library.patron.domain.exception.InvalidOperationException;
import com.library.patron.domain.exception.PatronCannotBorrowException;
import com.library.patron.domain.model.enums.MembershipStatus;
import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.model.PatronId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "patrons", indexes = {
    @Index(name = "idx_patron_email", columnList = "email"),
    @Index(name = "idx_patron_status", columnList = "status"),
    @Index(name = "idx_patron_type", columnList = "patron_type")
})
public class Patron {

    private static final BigDecimal MAX_ALLOWED_FINE = new BigDecimal("50.00");

    @EmbeddedId
    private PatronId id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatronType patronType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "member_since", nullable = false)
    private LocalDate memberSince;

    @Column(name = "membership_expiry")
    private LocalDate membershipExpiry;

    @Column(name = "current_loans", nullable = false)
    private Integer currentLoans;

    @Column(name = "outstanding_fines", precision = 10, scale = 2, nullable = false)
    private BigDecimal outstandingFines;

    @Column(name = "total_borrowed")
    private Integer totalBorrowed;

    @Column(name = "last_borrow_date")
    private LocalDate lastBorrowDate;

    @Embedded
    private BorrowingPrivilege borrowingPrivilege;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Patron() {
    }

    public Patron(PatronId id, String firstName, String lastName, String email, PatronType patronType) {
        this.id = Objects.requireNonNull(id, "Patron ID must not be null");
        this.firstName = validateName(firstName, "First name");
        this.lastName = validateName(lastName, "Last name");
        this.email = validateEmail(email);
        this.patronType = Objects.requireNonNull(patronType, "Patron type must not be null");
        this.status = MembershipStatus.ACTIVE;
        this.memberSince = LocalDate.now();
        this.currentLoans = 0;
        this.outstandingFines = BigDecimal.ZERO;
        this.totalBorrowed = 0;
        this.borrowingPrivilege = new BorrowingPrivilege(patronType);
    }

    public static Patron create(String firstName, String lastName, String email, PatronType patronType) {
        return new Patron(PatronId.generate(), firstName, lastName, email, patronType);
    }

    public void updatePersonalInfo(String firstName, String lastName, String email,
                                   String phone, String address, String city, String postalCode) {
        if (this.status == MembershipStatus.TERMINATED) {
            throw new InvalidOperationException("Cannot update terminated patron");
        }
        this.firstName = validateName(firstName, "First name");
        this.lastName = validateName(lastName, "Last name");
        if (email != null && !email.equals(this.email)) {
            this.email = validateEmail(email);
        }
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
    }

    public void updatePatronType(PatronType newType) {
        Objects.requireNonNull(newType, "Patron type must not be null");
        this.patronType = newType;
        this.borrowingPrivilege = new BorrowingPrivilege(newType);
    }

    public boolean canBorrow() {
        if (this.status != MembershipStatus.ACTIVE) return false;
        if (this.currentLoans >= this.borrowingPrivilege.getMaxLoans()) return false;
        if (this.outstandingFines.compareTo(MAX_ALLOWED_FINE) >= 0) return false;
        if (this.membershipExpiry != null && this.membershipExpiry.isBefore(LocalDate.now())) return false;
        return true;
    }

    public void recordLoan() {
        if (!canBorrow()) {
            throw new PatronCannotBorrowException(this.status, this.currentLoans, this.outstandingFines);
        }
        this.currentLoans++;
        this.totalBorrowed++;
        this.lastBorrowDate = LocalDate.now();
    }

    public void recordReturn() {
        if (this.currentLoans <= 0) {
            throw new InvalidOperationException("No active loans to return");
        }
        this.currentLoans--;
    }

    public void addFine(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fine amount must be positive");
        }
        this.outstandingFines = this.outstandingFines.add(amount);
        if (this.outstandingFines.compareTo(MAX_ALLOWED_FINE) >= 0 && this.status == MembershipStatus.ACTIVE) {
            this.status = MembershipStatus.SUSPENDED;
        }
    }

    public void payFine(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (amount.compareTo(this.outstandingFines) > 0) {
            throw new IllegalArgumentException("Payment exceeds outstanding fines");
        }
        this.outstandingFines = this.outstandingFines.subtract(amount);
        if (this.status == MembershipStatus.SUSPENDED && this.outstandingFines.compareTo(MAX_ALLOWED_FINE) < 0) {
            this.status = MembershipStatus.ACTIVE;
        }
    }

    public void waiveFine(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Waive amount must be positive");
        }
        if (amount.compareTo(this.outstandingFines) > 0) {
            throw new IllegalArgumentException("Waive amount exceeds outstanding fines");
        }
        this.outstandingFines = this.outstandingFines.subtract(amount);
        if (this.status == MembershipStatus.SUSPENDED && this.outstandingFines.compareTo(MAX_ALLOWED_FINE) < 0) {
            this.status = MembershipStatus.ACTIVE;
        }
    }

    public void suspend(String reason) {
        if (this.status == MembershipStatus.TERMINATED) {
            throw new InvalidOperationException("Cannot suspend terminated patron");
        }
        if (this.status == MembershipStatus.SUSPENDED) {
            throw new InvalidOperationException("Patron is already suspended");
        }
        this.status = MembershipStatus.SUSPENDED;
    }

    public void reactivate(String reason) {
        if (this.status != MembershipStatus.SUSPENDED) {
            throw new InvalidOperationException("Can only reactivate suspended patron");
        }
        if (this.outstandingFines.compareTo(MAX_ALLOWED_FINE) >= 0) {
            throw new InvalidOperationException("Cannot reactivate with excessive fines");
        }
        this.status = MembershipStatus.ACTIVE;
    }

    public void terminate(String reason) {
        if (this.status == MembershipStatus.TERMINATED) {
            throw new InvalidOperationException("Patron is already terminated");
        }
        if (this.currentLoans > 0) {
            throw new InvalidOperationException("Cannot terminate patron with active loans");
        }
        if (this.outstandingFines.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException("Cannot terminate patron with outstanding fines");
        }
        this.status = MembershipStatus.TERMINATED;
        this.membershipExpiry = LocalDate.now();
    }

    public void extendMembership(int months) {
        if (this.status == MembershipStatus.TERMINATED) {
            throw new InvalidOperationException("Cannot extend terminated membership");
        }
        LocalDate currentExpiry = this.membershipExpiry != null ? this.membershipExpiry : LocalDate.now();
        this.membershipExpiry = currentExpiry.plusMonths(months);
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    public boolean isActive() { return this.status == MembershipStatus.ACTIVE; }

    public boolean isSuspended() { return this.status == MembershipStatus.SUSPENDED; }

    public boolean isTerminated() { return this.status == MembershipStatus.TERMINATED; }

    public boolean hasOutstandingFines() { return this.outstandingFines.compareTo(BigDecimal.ZERO) > 0; }

    public boolean isMembershipValid() {
        return this.membershipExpiry == null || !this.membershipExpiry.isBefore(LocalDate.now());
    }

    public int getRemainingLoanQuota() {
        return Math.max(0, this.borrowingPrivilege.getMaxLoans() - this.currentLoans);
    }

    private String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException(fieldName + " must not be empty");
        if (name.length() > 100)
            throw new IllegalArgumentException(fieldName + " must not exceed 100 characters");
        return name.trim();
    }

    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Email must not be empty");
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new IllegalArgumentException("Invalid email format");
        if (email.length() > 150)
            throw new IllegalArgumentException("Email must not exceed 150 characters");
        return email.toLowerCase().trim();
    }

    // Getters
    public PatronId getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public PatronType getPatronType() { return patronType; }
    public MembershipStatus getStatus() { return status; }
    public LocalDate getMemberSince() { return memberSince; }
    public LocalDate getMembershipExpiry() { return membershipExpiry; }
    public Integer getCurrentLoans() { return currentLoans; }
    public BigDecimal getOutstandingFines() { return outstandingFines; }
    public Integer getTotalBorrowed() { return totalBorrowed; }
    public LocalDate getLastBorrowDate() { return lastBorrowDate; }
    public BorrowingPrivilege getBorrowingPrivilege() { return borrowingPrivilege; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
