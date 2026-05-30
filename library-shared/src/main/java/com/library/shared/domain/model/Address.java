package com.library.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * 地址值对象，封装街道、城市、邮编等信息。
 * 不可变对象。
 */
@Embeddable
public class Address {

    @Column(name = "street", length = 300)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    protected Address() {
    }

    public Address(String street, String city, String postalCode, String state, String country) {
        this.street = street != null ? street.trim() : null;
        this.city = Objects.requireNonNull(city, "City must not be null").trim();
        this.postalCode = postalCode != null ? postalCode.trim() : null;
        this.state = state != null ? state.trim() : null;
        this.country = country != null ? country.trim() : "China";
    }

    public Address(String street, String city, String postalCode) {
        this(street, city, postalCode, null, "China");
    }

    public static Address of(String street, String city, String postalCode) {
        return new Address(street, city, postalCode);
    }

    public static Address of(String street, String city, String postalCode, String state, String country) {
        return new Address(street, city, postalCode, state, country);
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            sb.append(street).append(", ");
        }
        sb.append(city);
        if (state != null && !state.isEmpty()) {
            sb.append(", ").append(state);
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            sb.append(" ").append(postalCode);
        }
        if (country != null && !country.isEmpty()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(street, address.street)
            && Objects.equals(city, address.city)
            && Objects.equals(postalCode, address.postalCode)
            && Objects.equals(state, address.state)
            && Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, postalCode, state, country);
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
