package com.library.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@MappedSuperclass
public abstract class AggregateId implements Serializable, Comparable<AggregateId> {

    @Column(name = "id", nullable = false)
    private String value;

    protected AggregateId() {
    }

    protected AggregateId(String value) {
        this.value = Objects.requireNonNull(value, "ID value must not be null");
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateId that = (AggregateId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public int compareTo(AggregateId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
