package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.CategoryNotFoundException;
import com.library.catalog.domain.model.Category;
import com.library.catalog.domain.repository.CategoryRepository;
import com.library.shared.domain.model.CategoryId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;

    public CategoryManagementService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category createRootCategory(String name, String description) {
        Category category = Category.createRoot(name, description);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category addChildCategory(CategoryId parentId, String name, String description) {
        Category parent = findCategoryOrThrow(parentId);
        Category child = parent.addChild(name, description);
        categoryRepository.save(parent);
        return child;
    }

    @Transactional
    public Category updateCategory(CategoryId categoryId, String name, String description) {
        Category category = findCategoryOrThrow(categoryId);
        category.updateInfo(name, description);
        return categoryRepository.save(category);
    }

    public Category getCategory(CategoryId categoryId) {
        return findCategoryOrThrow(categoryId);
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public void deleteCategory(CategoryId categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException("Category not found: " + categoryId.getValue());
        }
        categoryRepository.deleteById(categoryId);
    }

    private Category findCategoryOrThrow(CategoryId categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + categoryId.getValue()));
    }
}
