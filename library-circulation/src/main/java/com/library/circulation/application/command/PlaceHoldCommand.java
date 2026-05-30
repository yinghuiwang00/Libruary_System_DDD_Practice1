package com.library.circulation.application.command;

import jakarta.validation.constraints.NotBlank;

public class PlaceHoldCommand {

    @NotBlank(message = "Book ID must not be blank")
    private String bookId;

    @NotBlank(message = "Patron ID must not be blank")
    private String patronId;

    private String pickupLibraryId;

    public PlaceHoldCommand() {
    }

    public PlaceHoldCommand(String bookId, String patronId, String pickupLibraryId) {
        this.bookId = bookId;
        this.patronId = patronId;
        this.pickupLibraryId = pickupLibraryId;
    }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public String getPickupLibraryId() { return pickupLibraryId; }
    public void setPickupLibraryId(String pickupLibraryId) { this.pickupLibraryId = pickupLibraryId; }
}
