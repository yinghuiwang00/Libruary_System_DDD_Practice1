package com.library.catalog.domain.service;

import com.library.catalog.domain.model.ISBN;
import org.springframework.stereotype.Service;

/**
 * Domain service for ISBN validation and conversion.
 * External API integration is stubbed for now.
 */
@Service
public class ISBNValidationService {

    /**
     * Validates whether the given string is a valid ISBN-10 or ISBN-13.
     *
     * @param isbnValue the raw ISBN string (may contain hyphens/spaces)
     * @return true if valid, false otherwise
     */
    public boolean isValidISBN(String isbnValue) {
        if (isbnValue == null || isbnValue.isBlank()) {
            return false;
        }
        try {
            new ISBN(isbnValue);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Converts an ISBN-10 to ISBN-13.
     * The conversion prepends the GS1 prefix "978" and recalculates the check digit.
     *
     * @param isbn10 a valid ISBN-10 value object
     * @return the corresponding ISBN-13 value object
     * @throws IllegalArgumentException if the given ISBN is not an ISBN-10
     */
    public ISBN convertToISBN13(ISBN isbn10) {
        if (!isbn10.isISBN10()) {
            throw new IllegalArgumentException("Cannot convert non-ISBN-10 to ISBN-13: " + isbn10.getValue());
        }
        String digits = isbn10.getValue().substring(0, 9);
        String isbn13Body = "978" + digits;
        int checkDigit = calculateISBN13CheckDigit(isbn13Body);
        return new ISBN(isbn13Body + checkDigit);
    }

    /**
     * Looks up ISBN metadata from an external service.
     * Stubbed implementation - returns true to indicate the ISBN exists externally.
     *
     * @param isbn the ISBN to look up
     * @return true if the ISBN is found in the external registry (stubbed)
     */
    public boolean lookupExternalRegistry(ISBN isbn) {
        // TODO: Integrate with external ISBN registry API (e.g., Google Books, OpenLibrary)
        return true;
    }

    private int calculateISBN13CheckDigit(String first12Digits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12Digits.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
