package com.library.catalog.domain.repository;

import com.library.catalog.domain.model.Category;
import com.library.shared.domain.model.CategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, CategoryId> {

    Optional<Category> findByName(String name);

    List<Category> findByParentIsNull();

    List<Category> findByParentId(CategoryId parentId);

    boolean existsByName(String name);
}
