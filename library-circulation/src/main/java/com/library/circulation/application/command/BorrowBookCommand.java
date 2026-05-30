package com.library.circulation.application.command;

import jakarta.validation.constraints.NotBlank;

public class BorrowBookCommand {

    @NotBlank(message = "Copy ID must not be blank")
    private String copyId;

    @NotBlank(message = "Patron ID must not be blank")
    private String patronId;

    @NotBlank(message = "Book ID must not be blank")
    private String bookId;

    public BorrowBookCommand() {
    }

    public BorrowBookCommand(String copyId, String patronId, String bookId) {
        this.copyId = copyId;
        this.patronId = patronId;
        this.bookId = bookId;
    }

    public String getCopyId() { return copyId; }
    public void setCopyId(String copyId) { this.copyId = copyId; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
}
