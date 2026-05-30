package com.library.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 电话号码值对象，封装号码格式验证和规范化。
 * 不可变对象。
 */
@Embeddable
public class PhoneNumber {

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\+?[0-9]{7,15}$");

    @Column(name = "phone", length = 20)
    private String value;

    protected PhoneNumber() {
    }

    public PhoneNumber(String value) {
        Objects.requireNonNull(value, "Phone number must not be null");
        String digits = value.replaceAll("[\\s\\-()]", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
        this.value = digits;
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public String getValue() {
        return value;
    }

    public boolean isInternational() {
        return value.startsWith("+");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
        return value.equals(that.value);
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
