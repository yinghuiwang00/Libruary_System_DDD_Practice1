package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryDTO tests")
class CategoryDTOTest {

    // ---------------------------------------------------------------
    // Root category mapping
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("CategoryDTO.from() root category")
    class RootCategoryTests {

        @Test
        @DisplayName("should map root category fields correctly")
        void shouldMapRootCategoryFields() {
            Category root = Category.createRoot("Software Engineering", "Books about software engineering");

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.id()).isEqualTo(root.getId().getValue());
            assertThat(dto.name()).isEqualTo("Software Engineering");
            assertThat(dto.description()).isEqualTo("Books about software engineering");
            assertThat(dto.level()).isEqualTo(0);
        }

        @Test
        @DisplayName("should map parentId as null for root category")
        void shouldMapNullParentIdForRoot() {
            Category root = Category.createRoot("Root", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.parentId()).isNull();
        }

        @Test
        @DisplayName("should map empty children for root category without children")
        void shouldMapEmptyChildrenForRoot() {
            Category root = Category.createRoot("Root", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.children()).isEmpty();
        }

        @Test
        @DisplayName("should map id from CategoryId value")
        void shouldMapCategoryId() {
            Category root = Category.createRoot("Root", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.id()).isEqualTo(root.getId().getValue());
        }

        @Test
        @DisplayName("should map null description for root category")
        void shouldMapNullDescription() {
            Category root = Category.createRoot("Root", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.description()).isNull();
        }

        @Test
        @DisplayName("should map JPA-managed fields as null when not persisted")
        void shouldMapNullVersionAndTimestampsForNewEntity() {
            Category root = Category.createRoot("Root", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.version()).isNull();
            assertThat(dto.createdAt()).isNull();
            assertThat(dto.updatedAt()).isNull();
        }
    }

    // ---------------------------------------------------------------
    // Category with children
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("CategoryDTO.from() category with children")
    class CategoryWithChildrenTests {

        @Test
        @DisplayName("should map children list for category with children")
        void shouldMapChildrenList() {
            Category parent = Category.createRoot("Programming", "Programming books");
            parent.addChild("Java", "Java programming books");
            parent.addChild("Python", "Python programming books");

            CategoryDTO dto = CategoryDTO.from(parent);

            assertThat(dto.children()).hasSize(2);
            assertThat(dto.children().get(0).name()).isEqualTo("Java");
            assertThat(dto.children().get(1).name()).isEqualTo("Python");
        }

        @Test
        @DisplayName("should set parentId on child DTOs")
        void shouldSetParentIdOnChildren() {
            Category parent = Category.createRoot("Programming", null);
            parent.addChild("Java", null);

            CategoryDTO dto = CategoryDTO.from(parent);

            CategoryDTO childDto = dto.children().get(0);
            assertThat(childDto.parentId()).isEqualTo(parent.getId().getValue());
        }

        @Test
        @DisplayName("should set correct level on child DTOs")
        void shouldSetCorrectLevelOnChildren() {
            Category parent = Category.createRoot("Programming", null);
            parent.addChild("Java", null);

            CategoryDTO dto = CategoryDTO.from(parent);

            assertThat(dto.level()).isEqualTo(0);
            assertThat(dto.children().get(0).level()).isEqualTo(1);
        }

        @Test
        @DisplayName("should map child description")
        void shouldMapChildDescription() {
            Category parent = Category.createRoot("Programming", null);
            parent.addChild("Java", "Books about the Java language");

            CategoryDTO dto = CategoryDTO.from(parent);

            assertThat(dto.children().get(0).description()).isEqualTo("Books about the Java language");
        }

        @Test
        @DisplayName("should map child id from CategoryId value")
        void shouldMapChildId() {
            Category parent = Category.createRoot("Programming", null);
            Category child = parent.addChild("Java", null);

            CategoryDTO dto = CategoryDTO.from(parent);

            assertThat(dto.children().get(0).id()).isEqualTo(child.getId().getValue());
        }
    }

    // ---------------------------------------------------------------
    // Nested children (grandchildren)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("CategoryDTO.from() nested children")
    class NestedChildrenTests {

        @Test
        @DisplayName("should map grandchildren recursively")
        void shouldMapGrandchildrenRecursively() {
            Category root = Category.createRoot("Technology", null);
            Category programming = root.addChild("Programming", null);
            programming.addChild("Java", null);
            programming.addChild("Python", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.children()).hasSize(1);
            CategoryDTO programmingDto = dto.children().get(0);
            assertThat(programmingDto.name()).isEqualTo("Programming");
            assertThat(programmingDto.children()).hasSize(2);
            assertThat(programmingDto.children().get(0).name()).isEqualTo("Java");
            assertThat(programmingDto.children().get(1).name()).isEqualTo("Python");
        }

        @Test
        @DisplayName("should set correct levels for nested hierarchy")
        void shouldSetCorrectLevelsForNestedHierarchy() {
            Category root = Category.createRoot("Level 0", null);
            Category level1 = root.addChild("Level 1", null);
            Category level2 = level1.addChild("Level 2", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.level()).isEqualTo(0);
            assertThat(dto.children().get(0).level()).isEqualTo(1);
            assertThat(dto.children().get(0).children().get(0).level()).isEqualTo(2);
        }

        @Test
        @DisplayName("should set parentId correctly at each level")
        void shouldSetParentIdCorrectlyAtEachLevel() {
            Category root = Category.createRoot("Root", null);
            Category child = root.addChild("Child", null);
            Category grandchild = child.addChild("Grandchild", null);

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.parentId()).isNull();
            CategoryDTO childDto = dto.children().get(0);
            assertThat(childDto.parentId()).isEqualTo(root.getId().getValue());
            CategoryDTO grandchildDto = childDto.children().get(0);
            assertThat(grandchildDto.parentId()).isEqualTo(child.getId().getValue());
        }

        @Test
        @DisplayName("should handle deep three-level hierarchy")
        void shouldHandleDeepThreeLevelHierarchy() {
            Category root = Category.createRoot("Science", "All science books");
            Category physics = root.addChild("Physics", "Physics books");
            Category quantum = physics.addChild("Quantum Mechanics", "Quantum mechanics books");

            CategoryDTO dto = CategoryDTO.from(root);

            assertThat(dto.name()).isEqualTo("Science");
            assertThat(dto.description()).isEqualTo("All science books");
            assertThat(dto.children()).hasSize(1);

            CategoryDTO physicsDto = dto.children().get(0);
            assertThat(physicsDto.name()).isEqualTo("Physics");
            assertThat(physicsDto.parentId()).isEqualTo(root.getId().getValue());
            assertThat(physicsDto.children()).hasSize(1);

            CategoryDTO quantumDto = physicsDto.children().get(0);
            assertThat(quantumDto.name()).isEqualTo("Quantum Mechanics");
            assertThat(quantumDto.description()).isEqualTo("Quantum mechanics books");
            assertThat(quantumDto.parentId()).isEqualTo(physics.getId().getValue());
            assertThat(quantumDto.children()).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // Record behavior and snapshot isolation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("CategoryDTO record behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("should produce equal DTOs from same Category")
        void shouldProduceEqualDTOsFromSameCategory() {
            Category category = Category.createRoot("Category", "desc");

            CategoryDTO dto1 = CategoryDTO.from(category);
            CategoryDTO dto2 = CategoryDTO.from(category);

            assertThat(dto1).isEqualTo(dto2);
        }

        @Test
        @DisplayName("should produce different DTOs from different Categories")
        void shouldProduceDifferentDTOsFromDifferentCategories() {
            Category category1 = Category.createRoot("Category A", null);
            Category category2 = Category.createRoot("Category B", null);

            CategoryDTO dto1 = CategoryDTO.from(category1);
            CategoryDTO dto2 = CategoryDTO.from(category2);

            assertThat(dto1).isNotEqualTo(dto2);
            assertThat(dto1.name()).isEqualTo("Category A");
            assertThat(dto2.name()).isEqualTo("Category B");
        }

        @Test
        @DisplayName("should produce independent DTO snapshot")
        void shouldProduceIndependentSnapshot() {
            Category category = Category.createRoot("Original Name", "Original Desc");

            CategoryDTO dto = CategoryDTO.from(category);

            // Mutate the category after DTO creation
            category.updateInfo("Updated Name", "Updated Desc");

            // DTO should still hold original values
            assertThat(dto.name()).isEqualTo("Original Name");
            assertThat(dto.description()).isEqualTo("Original Desc");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            Category category = Category.createRoot("Category", null);
            CategoryDTO dto = CategoryDTO.from(category);

            assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
        }
    }
}
