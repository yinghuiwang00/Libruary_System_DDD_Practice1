package com.library.catalog.domain.model;

import com.library.shared.domain.model.CategoryId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @EmbeddedId
    private CategoryId id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "level", nullable = false)
    private int level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Category() {
    }

    private Category(CategoryId id, String name, String description, Category parent, int level) {
        this.id = id;
        setName(name);
        this.description = description;
        this.parent = parent;
        this.level = level;
    }

    public static Category createRoot(String name, String description) {
        return new Category(CategoryId.generate(), name, description, null, 0);
    }

    public Category addChild(String name, String description) {
        Category child = new Category(CategoryId.generate(), name, description, this, this.level + 1);
        this.children.add(child);
        return child;
    }

    public void removeChild(Category child) {
        this.children.remove(child);
        child.parent = null;
        child.level = 0;
    }

    public void updateInfo(String name, String description) {
        if (name != null) setName(name);
        if (description != null) this.description = description;
    }

    private void checkCircularReference(Category target) {
        Category current = target;
        while (current != null) {
            if (current == this) {
                throw new IllegalArgumentException("Circular reference detected in category hierarchy");
            }
            current = current.parent;
        }
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Category name must not exceed 100 characters");
        }
        this.name = name;
    }

    public CategoryId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    public Category getParent() { return parent; }
    public List<Category> getChildren() { return Collections.unmodifiableList(children); }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
