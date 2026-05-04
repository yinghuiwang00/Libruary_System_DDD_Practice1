package com.library.catalog.domain.model;

import com.library.shared.domain.model.PublisherId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "publishers")
public class Publisher {

    @EmbeddedId
    private PublisherId id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    protected Publisher() {
    }

    private Publisher(PublisherId id, String name, String description, String address,
                      String phone, String email, String website) {
        this.id = id;
        setName(name);
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
    }

    public static Publisher create(String name, String description, String address,
                                   String phone, String email, String website) {
        return new Publisher(PublisherId.generate(), name, description, address, phone, email, website);
    }

    public void updateInfo(String name, String description, String address,
                           String phone, String email, String website) {
        if (name != null) setName(name);
        if (description != null) this.description = description;
        if (address != null) this.address = address;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (website != null) this.website = website;
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Publisher name must not be blank");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Publisher name must not exceed 200 characters");
        }
        this.name = name;
    }

    public PublisherId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
