package com.library.inventory.domain.model;

import com.library.inventory.domain.exception.*;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.shared.domain.model.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "copy_inventories")
public class CopyInventory {

    private static final int MAX_COPIES_PER_INVENTORY = 1000;
    private static final int LOW_STOCK_THRESHOLD = 2;

    @EmbeddedId
    private CopyInventoryId id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "library_id", nullable = false, length = 36)
    private String libraryId;

    @Column(name = "library_code", nullable = false, length = 20)
    private String libraryCode;

    @Column(name = "total_copies")
    private Integer totalCopies;

    @Column(name = "available_copies")
    private Integer availableCopies;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_id")
    private List<BookCopy> copies = new ArrayList<>();

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    protected CopyInventory() {
    }

    public CopyInventory(CopyInventoryId id, String bookId, String libraryId, String libraryCode, String createdBy) {
        this.id = Objects.requireNonNull(id, "Inventory ID must not be null");
        this.bookId = Objects.requireNonNull(bookId, "Book ID must not be null");
        this.libraryId = Objects.requireNonNull(libraryId, "Library ID must not be null");
        this.libraryCode = Objects.requireNonNull(libraryCode, "Library code must not be null");
        this.totalCopies = 0;
        this.availableCopies = 0;
        this.createdBy = createdBy;
    }

    public static CopyInventory create(String bookId, String libraryId, String libraryCode, String createdBy) {
        return new CopyInventory(CopyInventoryId.generate(), bookId, libraryId, libraryCode, createdBy);
    }

    public BookCopy addCopy(String barcode, Location location, String acquisitionMethod) {
        validateCopyAddition();
        BookCopy copy = BookCopy.create(id.getValue(), barcode, location, acquisitionMethod);
        this.copies.add(copy);
        this.totalCopies++;
        this.availableCopies++;
        return copy;
    }

    public void removeCopy(String copyId, String reason) {
        BookCopy copy = findCopy(copyId);
        if (!copy.canBeRemoved()) {
            throw new InvalidOperationException("Copy cannot be removed in current state: " + copy.getStatus());
        }
        boolean wasAvailable = copy.isAvailable();
        copy.markAsRemoved(reason);
        this.copies.remove(copy);
        this.totalCopies--;
        if (wasAvailable) {
            this.availableCopies--;
        }
    }

    public BookCopy getAvailableCopy() {
        return this.copies.stream()
            .filter(BookCopy::isAvailable)
            .findFirst()
            .orElseThrow(() -> new NoAvailableCopyException(BookId.of(bookId)));
    }

    public boolean hasAvailableCopies() {
        return this.availableCopies > 0;
    }

    public void onCopyStatusChanged(CopyStatus oldStatus, CopyStatus newStatus) {
        if (oldStatus == CopyStatus.AVAILABLE && newStatus != CopyStatus.AVAILABLE) {
            this.availableCopies--;
        } else if (oldStatus != CopyStatus.AVAILABLE && newStatus == CopyStatus.AVAILABLE) {
            this.availableCopies++;
        }
    }

    public List<BookCopy> addCopies(int count, Location location, String acquisitionMethod) {
        if (count <= 0) {
            throw new IllegalArgumentException("Copy count must be positive");
        }
        List<BookCopy> newCopies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String barcode = generateBarcode(this.totalCopies + i + 1);
            BookCopy copy = addCopy(barcode, location, acquisitionMethod);
            newCopies.add(copy);
        }
        return newCopies;
    }

    public double getAvailabilityRate() {
        if (this.totalCopies == 0) return 0.0;
        return (double) this.availableCopies / this.totalCopies;
    }

    public boolean isLowStock() {
        return this.availableCopies <= LOW_STOCK_THRESHOLD;
    }

    private void validateCopyAddition() {
        if (this.totalCopies >= MAX_COPIES_PER_INVENTORY) {
            throw new InvalidOperationException("Maximum copies per inventory reached: " + MAX_COPIES_PER_INVENTORY);
        }
    }

    private BookCopy findCopy(String copyId) {
        return this.copies.stream()
            .filter(copy -> copy.getId().getValue().equals(copyId))
            .findFirst()
            .orElseThrow(() -> new CopyNotFoundException(CopyId.of(copyId)));
    }

    private String generateBarcode(int sequence) {
        return String.format("%s-%s-%06d",
            libraryCode,
            bookId.substring(0, Math.min(8, bookId.length())),
            sequence);
    }

    public CopyInventoryId getId() { return id; }
    public String getBookId() { return bookId; }
    public String getLibraryId() { return libraryId; }
    public String getLibraryCode() { return libraryCode; }
    public Integer getTotalCopies() { return totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public List<BookCopy> getCopies() { return Collections.unmodifiableList(copies); }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
}
