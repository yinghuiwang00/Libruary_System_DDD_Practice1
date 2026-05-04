package com.library.inventory.domain.model;

import com.library.inventory.domain.exception.InvalidOperationException;
import com.library.shared.domain.model.LibraryId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "libraries")
public class Library {

    @EmbeddedId
    private LibraryId id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "opening_hours", length = 200)
    private String openingHours;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "is_active")
    private Boolean active;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Library() {
    }

    public Library(LibraryId id, String code, String name) {
        this.id = Objects.requireNonNull(id, "Library ID must not be null");
        this.code = validateCode(code);
        this.name = validateName(name);
        this.active = true;
    }

    public static Library create(String code, String name) {
        return new Library(LibraryId.generate(), code, name);
    }

    public void updateContactInfo(String address, String city, String province,
                                   String postalCode, String phone, String email) {
        this.address = address;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.phone = phone;
        this.email = email;
    }

    public void updateOperatingInfo(String openingHours, Integer totalFloors) {
        this.openingHours = openingHours;
        if (totalFloors != null && totalFloors > 0) {
            this.totalFloors = totalFloors;
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active != null && this.active;
    }

    private String validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Library code must not be blank");
        }
        if (code.length() > 20) {
            throw new IllegalArgumentException("Library code must not exceed 20 characters");
        }
        return code.toUpperCase().trim();
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Library name must not be blank");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Library name must not exceed 200 characters");
        }
        return name.trim();
    }

    public LibraryId getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getPostalCode() { return postalCode; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getOpeningHours() { return openingHours; }
    public Integer getTotalFloors() { return totalFloors; }
    public Boolean getActive() { return active; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
