package com.library.circulation.application.dto;

import com.library.circulation.domain.model.Hold;

import java.time.LocalDateTime;

public class HoldDTO {

    private String id;
    private String bookId;
    private String patronId;
    private LocalDateTime requestDate;
    private LocalDateTime expirationDate;
    private String status;
    private Integer queuePosition;
    private String fulfilledCopyId;
    private LocalDateTime availableUntilDate;
    private String pickupLibraryId;
    private LocalDateTime createdAt;

    public HoldDTO() {
    }

    public static HoldDTO fromDomain(Hold hold) {
        HoldDTO dto = new HoldDTO();
        dto.id = hold.getId().getValue();
        dto.bookId = hold.getBookId().getValue();
        dto.patronId = hold.getPatronId().getValue();
        dto.requestDate = hold.getRequestDate();
        dto.expirationDate = hold.getExpirationDate();
        dto.status = hold.getStatus().name();
        dto.queuePosition = hold.getQueuePosition();
        dto.fulfilledCopyId = hold.getFulfilledCopyId() != null ? hold.getFulfilledCopyId().getValue() : null;
        dto.availableUntilDate = hold.getAvailableUntilDate();
        dto.pickupLibraryId = hold.getPickupLibraryId();
        dto.createdAt = hold.getCreatedAt();
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }
    public String getFulfilledCopyId() { return fulfilledCopyId; }
    public void setFulfilledCopyId(String fulfilledCopyId) { this.fulfilledCopyId = fulfilledCopyId; }
    public LocalDateTime getAvailableUntilDate() { return availableUntilDate; }
    public void setAvailableUntilDate(LocalDateTime availableUntilDate) { this.availableUntilDate = availableUntilDate; }
    public String getPickupLibraryId() { return pickupLibraryId; }
    public void setPickupLibraryId(String pickupLibraryId) { this.pickupLibraryId = pickupLibraryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
