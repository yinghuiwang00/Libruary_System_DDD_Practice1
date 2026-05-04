package com.library.catalog.domain.exception;

public class PublisherNotFoundException extends DomainException {
    public PublisherNotFoundException(String message) {
        super("PUBLISHER_NOT_FOUND", message);
    }
}
