package com.library.circulation.domain.model;

import com.library.circulation.domain.exception.InvalidOperationException;
import com.library.circulation.domain.model.enums.HoldStatus;
import com.library.shared.domain.model.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "holds", indexes = {
    @Index(name = "idx_hold_book", columnList = "book_id"),
    @Index(name = "idx_hold_patron", columnList = "patron_id"),
    @Index(name = "idx_hold_status", columnList = "status"),
    @Index(name = "idx_hold_expiration", columnList = "expiration_date")
})
public class Hold {

    @EmbeddedId
    private HoldId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "book_id"))
    private BookId bookId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "patron_id"))
    private PatronId patronId;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HoldStatus status;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "fulfilled_copy_id"))
    private CopyId fulfilledCopyId;

    @Column(name = "fulfillment_date")
    private LocalDateTime fulfillmentDate;

    @Column(name = "available_until_date")
    private LocalDateTime availableUntilDate;

    @Column(name = "notification_sent")
    private Boolean notificationSent;

    @Column(name = "pickup_library_id", length = 36)
    private String pickupLibraryId;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Hold() {
    }

    public Hold(HoldId id, BookId bookId, PatronId patronId,
                LocalDateTime requestDate, LocalDateTime expirationDate,
                Integer queuePosition) {
        this.id = Objects.requireNonNull(id, "Hold ID must not be null");
        this.bookId = Objects.requireNonNull(bookId, "Book ID must not be null");
        this.patronId = Objects.requireNonNull(patronId, "Patron ID must not be null");
        this.requestDate = Objects.requireNonNull(requestDate, "Request date must not be null");
        this.expirationDate = Objects.requireNonNull(expirationDate, "Expiration date must not be null");
        this.queuePosition = queuePosition;
        this.status = HoldStatus.WAITING;
        this.notificationSent = false;
    }

    public static Hold create(BookId bookId, PatronId patronId,
                              LocalDateTime requestDate, int queuePosition,
                              int holdExpirationDays) {
        return new Hold(
            HoldId.generate(), bookId, patronId,
            requestDate, requestDate.plusDays(holdExpirationDays),
            queuePosition
        );
    }

    public void fulfill(CopyId copyId, LocalDateTime fulfillmentDate, int availableDays) {
        if (this.status != HoldStatus.WAITING) {
            throw new InvalidOperationException("Cannot fulfill hold in status: " + this.status);
        }
        this.fulfilledCopyId = Objects.requireNonNull(copyId, "Copy ID must not be null");
        this.fulfillmentDate = Objects.requireNonNull(fulfillmentDate, "Fulfillment date must not be null");
        this.availableUntilDate = fulfillmentDate.plusDays(availableDays);
        this.status = HoldStatus.READY_FOR_PICKUP;
    }

    public void cancel(String reason) {
        if (this.status == HoldStatus.FULFILLED || this.status == HoldStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot cancel hold in status: " + this.status);
        }
        this.status = HoldStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void markAsPickedUp(LocalDateTime pickupDate) {
        if (this.status != HoldStatus.READY_FOR_PICKUP) {
            throw new InvalidOperationException("Hold is not ready for pickup");
        }
        if (pickupDate.isAfter(this.availableUntilDate)) {
            throw new InvalidOperationException("Hold has expired for pickup");
        }
        this.status = HoldStatus.FULFILLED;
    }

    public void markAsExpiredNotPickedUp() {
        if (this.status != HoldStatus.READY_FOR_PICKUP) {
            throw new InvalidOperationException("Hold is not ready for pickup");
        }
        this.status = HoldStatus.EXPIRED_NOT_PICKED_UP;
    }

    public void markAsExpired() {
        if (this.status != HoldStatus.WAITING) {
            throw new InvalidOperationException("Can only expire waiting holds");
        }
        this.status = HoldStatus.EXPIRED;
    }

    public void updateQueuePosition(int newPosition) {
        if (newPosition < 1) {
            throw new IllegalArgumentException("Queue position must be positive");
        }
        this.queuePosition = newPosition;
    }

    public void extendExpiration(int additionalDays) {
        if (this.status != HoldStatus.WAITING) {
            throw new InvalidOperationException("Can only extend waiting holds");
        }
        this.expirationDate = this.expirationDate.plusDays(additionalDays);
    }

    public void markNotificationSent() {
        this.notificationSent = true;
    }

    public void setPickupLibraryId(String pickupLibraryId) {
        this.pickupLibraryId = pickupLibraryId;
    }

    public boolean isExpired(LocalDateTime currentDate) {
        return currentDate.isAfter(this.expirationDate);
    }

    public boolean isReadyForPickup() {
        return this.status == HoldStatus.READY_FOR_PICKUP;
    }

    public boolean isPickupExpired(LocalDateTime currentDate) {
        return this.status == HoldStatus.READY_FOR_PICKUP
                && currentDate.isAfter(this.availableUntilDate);
    }

    public int getDaysUntilExpiration(LocalDateTime currentDate) {
        if (isExpired(currentDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(currentDate, this.expirationDate);
    }

    public HoldId getId() { return id; }
    public BookId getBookId() { return bookId; }
    public PatronId getPatronId() { return patronId; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public HoldStatus getStatus() { return status; }
    public Integer getQueuePosition() { return queuePosition; }
    public CopyId getFulfilledCopyId() { return fulfilledCopyId; }
    public LocalDateTime getFulfillmentDate() { return fulfillmentDate; }
    public LocalDateTime getAvailableUntilDate() { return availableUntilDate; }
    public Boolean getNotificationSent() { return notificationSent; }
    public String getPickupLibraryId() { return pickupLibraryId; }
    public String getCancelReason() { return cancelReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
