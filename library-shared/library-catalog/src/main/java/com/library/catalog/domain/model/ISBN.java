package com.library.catalog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ISBN {

    private static final String ISBN_CLEAN_PATTERN = "[\\s-]";

    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    private String value;

    protected ISBN() {
    }

    public ISBN(String value) {
        Objects.requireNonNull(value, "ISBN must not be null");
        String cleaned = cleanISBN(value);
        validateCleanedISBN(cleaned);
        this.value = cleaned;
    }

    private String cleanISBN(String value) {
        return value.replaceAll(ISBN_CLEAN_PATTERN, "");
    }

    private void validateCleanedISBN(String cleaned) {
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("ISBN must not be empty");
        }
        if (cleaned.length() == 10) {
            validateISBN10(cleaned);
        } else if (cleaned.length() == 13) {
            validateISBN13(cleaned);
        } else {
            throw new IllegalArgumentException(
                "ISBN must be 10 or 13 digits, got " + cleaned.length() + " characters");
        }
    }

    private void validateISBN10(String value) {
        if (!value.matches("[0-9]{9}[0-9Xx]")) {
            throw new IllegalArgumentException("Invalid ISBN-10 format: " + value);
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (value.charAt(i) - '0') * (10 - i);
        }
        char checkChar = Character.toUpperCase(value.charAt(9));
        int checkDigit = (checkChar == 'X') ? 10 : (checkChar - '0');
        sum += checkDigit;

        if (sum % 11 != 0) {
            throw new IllegalArgumentException("Invalid ISBN-10 checksum: " + value);
        }
    }

    private void validateISBN13(String value) {
        if (!value.matches("[0-9]{13}")) {
            throw new IllegalArgumentException("Invalid ISBN-13 format: " + value);
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = value.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int expectedCheck = (10 - (sum % 10)) % 10;
        int actualCheck = value.charAt(12) - '0';

        if (expectedCheck != actualCheck) {
            throw new IllegalArgumentException("Invalid ISBN-13 checksum: " + value);
        }
    }

    public String getValue() {
        return value;
    }

    public boolean isISBN10() {
        return value.length() == 10;
    }

    public boolean isISBN13() {
        return value.length() == 13;
    }

    public String getFormattedValue() {
        if (isISBN13()) {
            return value.substring(0, 3) + "-" +
                   value.substring(3, 4) + "-" +
                   value.substring(4, 7) + "-" +
                   value.substring(7, 12) + "-" +
                   value.substring(12);
        } else {
            return value.substring(0, 1) + "-" +
                   value.substring(1, 4) + "-" +
                   value.substring(4, 9) + "-" +
                   value.charAt(9);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ISBN isbn = (ISBN) o;
        return value.equals(isbn.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
