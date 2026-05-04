package com.library.inventory.domain.model;

import com.library.inventory.domain.exception.InvalidOperationException;
import com.library.inventory.domain.model.enums.CopyCondition;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.shared.domain.model.CopyId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @EmbeddedId
    private CopyId id;

    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Embedded
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopyStatus status;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_method", length = 50)
    private String acquisitionMethod;

    @Column(name = "acquisition_cost", precision = 10, scale = 2)
    private BigDecimal acquisitionCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 20)
    private CopyCondition condition;

    @Column(name = "last_borrowed_date")
    private LocalDate lastBorrowedDate;

    @Column(name = "borrow_count")
    private Integer borrowCount;

    @Column(name = "damaged_date")
    private LocalDate damagedDate;

    @Column(name = "damage_description", length = 500)
    private String damageDescription;

    @Column(name = "lost_date")
    private LocalDate lostDate;

    @Column(name = "lost_reason", length = 200)
    private String lostReason;

    @Column(name = "removed_date")
    private LocalDate removedDate;

    @Column(name = "removal_reason", length = 200)
    private String removalReason;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected BookCopy() {
    }

    public BookCopy(CopyId id, String inventoryId, String barcode,
                    Location location, String acquisitionMethod) {
        this.id = Objects.requireNonNull(id, "Copy ID must not be null");
        this.inventoryId = Objects.requireNonNull(inventoryId, "Inventory ID must not be null");
        this.barcode = validateBarcode(barcode);
        this.location = Objects.requireNonNull(location, "Location must not be null");
        this.status = CopyStatus.AVAILABLE;
        this.acquisitionDate = LocalDate.now();
        this.acquisitionMethod = acquisitionMethod;
        this.condition = CopyCondition.NEW;
        this.borrowCount = 0;
    }

    public static BookCopy create(String inventoryId, String barcode,
                                  Location location, String acquisitionMethod) {
        return new BookCopy(CopyId.generate(), inventoryId, barcode, location, acquisitionMethod);
    }

    public void markAsBorrowed() {
        if (this.status != CopyStatus.AVAILABLE) {
            throw new InvalidOperationException("Copy is not available for borrowing, current status: " + this.status);
        }
        this.status = CopyStatus.BORROWED;
        this.lastBorrowedDate = LocalDate.now();
        this.borrowCount++;
    }

    public void markAsReturned() {
        if (this.status != CopyStatus.BORROWED) {
            throw new InvalidOperationException("Copy is not borrowed, current status: " + this.status);
        }
        this.status = CopyStatus.AVAILABLE;
    }

    public void markAsReserved() {
        if (this.status != CopyStatus.AVAILABLE) {
            throw new InvalidOperationException("Copy is not available for reservation, current status: " + this.status);
        }
        this.status = CopyStatus.RESERVED;
    }

    public void releaseReservation() {
        if (this.status != CopyStatus.RESERVED) {
            throw new InvalidOperationException("Copy is not reserved, current status: " + this.status);
        }
        this.status = CopyStatus.AVAILABLE;
    }

    public void markAsDamaged(String damageDescription) {
        if (this.status == CopyStatus.REMOVED) {
            throw new InvalidOperationException("Cannot mark removed copy as damaged");
        }
        this.status = CopyStatus.DAMAGED;
        this.damagedDate = LocalDate.now();
        this.damageDescription = damageDescription;
        this.condition = CopyCondition.DAMAGED;
    }

    public void markAsLost(String reason) {
        if (this.status == CopyStatus.REMOVED) {
            throw new InvalidOperationException("Cannot mark removed copy as lost");
        }
        this.status = CopyStatus.LOST;
        this.lostDate = LocalDate.now();
        this.lostReason = reason;
    }

    public void markAsUnderRepair() {
        if (this.status != CopyStatus.DAMAGED) {
            throw new InvalidOperationException("Only damaged copies can be marked as under repair, current: " + this.status);
        }
        this.status = CopyStatus.UNDER_REPAIR;
    }

    public void markAsRepaired() {
        if (this.status != CopyStatus.UNDER_REPAIR) {
            throw new InvalidOperationException("Copy is not under repair, current: " + this.status);
        }
        this.status = CopyStatus.AVAILABLE;
        this.condition = CopyCondition.GOOD;
    }

    public void markAsRemoved(String reason) {
        if (this.status == CopyStatus.BORROWED) {
            throw new InvalidOperationException("Cannot remove borrowed copy");
        }
        if (this.status == CopyStatus.REMOVED) {
            throw new InvalidOperationException("Copy is already removed");
        }
        this.status = CopyStatus.REMOVED;
        this.removedDate = LocalDate.now();
        this.removalReason = reason;
    }

    public void updateLocation(Location newLocation) {
        this.location = Objects.requireNonNull(newLocation, "Location must not be null");
    }

    public void updateCondition(CopyCondition newCondition) {
        this.condition = Objects.requireNonNull(newCondition, "Condition must not be null");
    }

    public void setAcquisitionCost(BigDecimal cost) {
        this.acquisitionCost = cost;
    }

    public boolean isAvailable() {
        return this.status == CopyStatus.AVAILABLE;
    }

    public boolean isBorrowed() {
        return this.status == CopyStatus.BORROWED;
    }

    public boolean canBeRemoved() {
        return this.status != CopyStatus.BORROWED && this.status != CopyStatus.REMOVED;
    }

    public boolean canBeTransferred() {
        return this.status != CopyStatus.BORROWED && this.status != CopyStatus.REMOVED;
    }

    public boolean isDamagedOrLost() {
        return this.status == CopyStatus.DAMAGED ||
               this.status == CopyStatus.LOST ||
               this.status == CopyStatus.UNDER_REPAIR;
    }

    private String validateBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("Barcode must not be blank");
        }
        if (barcode.length() > 50) {
            throw new IllegalArgumentException("Barcode must not exceed 50 characters");
        }
        return barcode.trim();
    }

    public CopyId getId() { return id; }
    public String getInventoryId() { return inventoryId; }
    public String getBarcode() { return barcode; }
    public Location getLocation() { return location; }
    public CopyStatus getStatus() { return status; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public String getAcquisitionMethod() { return acquisitionMethod; }
    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public CopyCondition getCondition() { return condition; }
    public LocalDate getLastBorrowedDate() { return lastBorrowedDate; }
    public Integer getBorrowCount() { return borrowCount; }
    public String getDamageDescription() { return damageDescription; }
    public String getLostReason() { return lostReason; }
    public String getRemovalReason() { return removalReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
