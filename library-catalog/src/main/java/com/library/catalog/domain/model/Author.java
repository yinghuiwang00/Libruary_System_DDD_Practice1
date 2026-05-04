package com.library.catalog.domain.model;

import com.library.shared.domain.model.AuthorId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "authors")
public class Author {

    @EmbeddedId
    private AuthorId id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String biography;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Column(name = "nationality", length = 100)
    private String nationality;

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

    protected Author() {
    }

    private Author(AuthorId id, String name, String biography, LocalDate birthDate,
                   LocalDate deathDate, String nationality) {
        this.id = id;
        setName(name);
        this.biography = biography;
        setDates(birthDate, deathDate);
        this.nationality = nationality;
    }

    public static Author create(String name, String biography, LocalDate birthDate,
                                LocalDate deathDate, String nationality) {
        return new Author(AuthorId.generate(), name, biography, birthDate, deathDate, nationality);
    }

    public void updateBiography(String biography) {
        this.biography = biography;
    }

    public void updatePersonalInfo(String name, String nationality, LocalDate birthDate, LocalDate deathDate) {
        if (name != null) {
            setName(name);
        }
        if (nationality != null) {
            this.nationality = nationality;
        }
        setDates(
            birthDate != null ? birthDate : this.birthDate,
            deathDate != null ? deathDate : this.deathDate
        );
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Author name must not be blank");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Author name must not exceed 200 characters");
        }
        this.name = name;
    }

    private void setDates(LocalDate birthDate, LocalDate deathDate) {
        if (birthDate != null && deathDate != null && deathDate.isBefore(birthDate)) {
            throw new IllegalArgumentException("Death date must not be before birth date");
        }
        this.birthDate = birthDate;
        this.deathDate = deathDate;
    }

    public AuthorId getId() { return id; }
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public LocalDate getBirthDate() { return birthDate; }
    public LocalDate getDeathDate() { return deathDate; }
    public String getNationality() { return nationality; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
