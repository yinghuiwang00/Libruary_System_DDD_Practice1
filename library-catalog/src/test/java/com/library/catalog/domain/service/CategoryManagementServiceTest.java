package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.CategoryNotFoundException;
import com.library.catalog.domain.model.Category;
import com.library.catalog.domain.repository.CategoryRepository;
import com.library.shared.domain.model.CategoryId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryManagementServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryManagementService service;

    private CategoryId categoryId;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    // ------------------------------------------------------------------ //
    //  createRootCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("createRootCategory")
    class CreateRootCategoryTests {

        @Test
        @DisplayName("should create root category and return saved entity")
        void shouldCreateRootCategory() {
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            Category result = service.createRootCategory("Programming", "Books about programming");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Programming");
            assertThat(result.getDescription()).isEqualTo("Books about programming");
            assertThat(result.getLevel()).isEqualTo(0);
            assertThat(result.getParent()).isNull();
            verify(categoryRepository).save(any(Category.class));
        }
    }

    // ------------------------------------------------------------------ //
    //  addChildCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("addChildCategory")
    class AddChildCategoryTests {

        @Test
        @DisplayName("should add child category to parent")
        void shouldAddChildCategory() {
            Category parent = Category.createRoot("Programming", "Programming books");
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            Category child = service.addChildCategory(categoryId, "Java", "Java programming");

            assertThat(child).isNotNull();
            assertThat(child.getName()).isEqualTo("Java");
            assertThat(child.getDescription()).isEqualTo("Java programming");
            assertThat(child.getLevel()).isEqualTo(1);
            assertThat(child.getParent()).isSameAs(parent);
            assertThat(parent.getChildren()).hasSize(1);
            assertThat(parent.getChildren().get(0).getName()).isEqualTo("Java");
            verify(categoryRepository).save(parent);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when parent not found")
        void shouldThrow_whenParentNotFound() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addChildCategory(categoryId, "Java", "desc"))
                .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  updateCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("should update category name and description")
        void shouldUpdateCategory() {
            Category category = Category.createRoot("Old Name", "Old desc");
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            Category result = service.updateCategory(categoryId, "New Name", "New desc");

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New desc");
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category not found")
        void shouldThrow_whenNotFound() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCategory(categoryId, "Name", "desc"))
                .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  getCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getCategory")
    class GetCategoryTests {

        @Test
        @DisplayName("should return category when found")
        void shouldReturnCategory() {
            Category category = Category.createRoot("Programming", "desc");
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            Category result = service.getCategory(categoryId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Programming");
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCategory(categoryId))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("Category not found");
        }
    }

    // ------------------------------------------------------------------ //
    //  getRootCategories
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getRootCategories")
    class GetRootCategoriesTests {

        @Test
        @DisplayName("should return only root categories")
        void shouldReturnRootCategories() {
            Category root1 = Category.createRoot("Programming", null);
            Category root2 = Category.createRoot("Fiction", null);
            when(categoryRepository.findByParentIsNull()).thenReturn(List.of(root1, root2));

            List<Category> result = service.getRootCategories();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Category::getName)
                .containsExactly("Programming", "Fiction");
        }

        @Test
        @DisplayName("should return empty list when no root categories exist")
        void shouldReturnEmptyList() {
            when(categoryRepository.findByParentIsNull()).thenReturn(Collections.emptyList());

            List<Category> result = service.getRootCategories();

            assertThat(result).isEmpty();
        }
    }

    // ------------------------------------------------------------------ //
    //  getAllCategories
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("should return all categories including children")
        void shouldReturnAllCategories() {
            Category root = Category.createRoot("Programming", null);
            Category child = root.addChild("Java", null);
            when(categoryRepository.findAll()).thenReturn(List.of(root, child));

            List<Category> result = service.getAllCategories();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no categories exist")
        void shouldReturnEmptyList() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<Category> result = service.getAllCategories();

            assertThat(result).isEmpty();
        }
    }

    // ------------------------------------------------------------------ //
    //  deleteCategory
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("should delete category when exists")
        void shouldDeleteCategory() {
            when(categoryRepository.existsById(categoryId)).thenReturn(true);

            service.deleteCategory(categoryId);

            verify(categoryRepository).deleteById(categoryId);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category not found")
        void shouldThrow_whenNotFound() {
            when(categoryRepository.existsById(categoryId)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteCategory(categoryId))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("Category not found");

            verify(categoryRepository, never()).deleteById(any());
        }
    }
}
