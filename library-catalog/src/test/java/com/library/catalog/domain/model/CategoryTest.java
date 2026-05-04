package com.library.catalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create root category")
        void shouldCreateRoot() {
            Category root = Category.createRoot("Fiction", "All fiction books");
            assertNotNull(root.getId());
            assertEquals("Fiction", root.getName());
            assertEquals("All fiction books", root.getDescription());
            assertEquals(0, root.getLevel());
            assertNull(root.getParent());
            assertTrue(root.getChildren().isEmpty());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                Category.createRoot("", null));
        }

        @Test
        @DisplayName("should reject name over 100 chars")
        void shouldRejectLongName() {
            assertThrows(IllegalArgumentException.class, () ->
                Category.createRoot("a".repeat(101), null));
        }
    }

    @Nested
    @DisplayName("Hierarchy Tests")
    class HierarchyTests {

        @Test
        @DisplayName("should add child category")
        void shouldAddChild() {
            Category root = Category.createRoot("Fiction", null);
            Category child = root.addChild("Science Fiction", null);
            assertEquals("Science Fiction", child.getName());
            assertEquals(1, child.getLevel());
            assertEquals(root, child.getParent());
            assertEquals(1, root.getChildren().size());
        }

        @Test
        @DisplayName("should support multi-level hierarchy")
        void shouldSupportMultiLevel() {
            Category root = Category.createRoot("Fiction", null);
            Category l1 = root.addChild("Science Fiction", null);
            Category l2 = l1.addChild("Hard SF", null);
            assertEquals(2, l2.getLevel());
            assertEquals(l1, l2.getParent());
        }

        @Test
        @DisplayName("should remove child")
        void shouldRemoveChild() {
            Category root = Category.createRoot("Fiction", null);
            Category child = root.addChild("Fantasy", null);
            root.removeChild(child);
            assertTrue(root.getChildren().isEmpty());
            assertNull(child.getParent());
            assertEquals(0, child.getLevel());
        }

        @Test
        @DisplayName("should return unmodifiable children list")
        void shouldReturnUnmodifiableChildren() {
            Category root = Category.createRoot("Fiction", null);
            root.addChild("Child", null);
            assertThrows(UnsupportedOperationException.class, () ->
                root.getChildren().add(null));
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("should update info")
        void shouldUpdateInfo() {
            Category cat = Category.createRoot("Old", "old desc");
            cat.updateInfo("New", "new desc");
            assertEquals("New", cat.getName());
            assertEquals("new desc", cat.getDescription());
        }

        @Test
        @DisplayName("should preserve values when null passed")
        void shouldPreserveOnNull() {
            Category cat = Category.createRoot("Name", "desc");
            cat.updateInfo(null, null);
            assertEquals("Name", cat.getName());
            assertEquals("desc", cat.getDescription());
        }
    }
}
