package com.library.catalog.domain.model;

import com.library.catalog.domain.model.enums.BookStatus;
import com.library.catalog.domain.exception.*;
import com.library.catalog.domain.model.enums.AuthorRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    private Book createDraftBook() {
        return Book.create(new ISBN("978-0-13-468599-1"), "Effective Java",
            "Programming guide", LocalDate.of(2018, 1, 6), 416, "en");
    }

    private Book createPublishableBook() {
        Book book = createDraftBook();
        book.addAuthor("author-1", "Joshua Bloch", AuthorRole.AUTHOR);
        book.setPublisher("publisher-1");
        book.addCategory("cat-1");
        return book;
    }

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create book in DRAFT status")
        void shouldCreateDraft() {
            Book book = createDraftBook();
            assertNotNull(book.getId());
            assertEquals("9780134685991", book.getIsbn().getValue());
            assertEquals("Effective Java", book.getTitle());
            assertEquals("Programming guide", book.getDescription());
            assertEquals(LocalDate.of(2018, 1, 6), book.getPublicationDate());
            assertEquals(416, book.getPageCount());
            assertEquals("en", book.getLanguage());
            assertEquals(BookStatus.DRAFT, book.getStatus());
            assertTrue(book.getAuthors().isEmpty());
            assertTrue(book.getCategoryIds().isEmpty());
            assertNull(book.getPublisherId());
        }

        @Test
        @DisplayName("should reject blank title")
        void shouldRejectBlankTitle() {
            assertThrows(IllegalArgumentException.class, () ->
                Book.create(new ISBN("978-0-13-468599-1"), "", null, null, null, null));
        }

        @Test
        @DisplayName("should reject title over 500 chars")
        void shouldRejectLongTitle() {
            assertThrows(IllegalArgumentException.class, () ->
                Book.create(new ISBN("978-0-13-468599-1"), "x".repeat(501), null, null, null, null));
        }
    }

    @Nested
    @DisplayName("Author Management Tests")
    class AuthorTests {

        @Test
        @DisplayName("should add author")
        void shouldAddAuthor() {
            Book book = createDraftBook();
            book.addAuthor("a1", "Author One", AuthorRole.AUTHOR);
            assertEquals(1, book.getAuthors().size());
            assertEquals("Author One", book.getAuthors().get(0).getAuthorName());
        }

        @Test
        @DisplayName("should reject duplicate author")
        void shouldRejectDuplicate() {
            Book book = createDraftBook();
            book.addAuthor("a1", "Author One", AuthorRole.AUTHOR);
            assertThrows(DuplicateAuthorException.class, () ->
                book.addAuthor("a1", "Author One", AuthorRole.CO_AUTHOR));
        }

        @Test
        @DisplayName("should remove author")
        void shouldRemoveAuthor() {
            Book book = createDraftBook();
            book.addAuthor("a1", "Author One", AuthorRole.AUTHOR);
            book.addAuthor("a2", "Author Two", AuthorRole.CO_AUTHOR);
            book.removeAuthor("a1");
            assertEquals(1, book.getAuthors().size());
            assertEquals("Author Two", book.getAuthors().get(0).getAuthorName());
        }

        @Test
        @DisplayName("should throw when removing non-existent author")
        void shouldThrowOnRemoveNonExistent() {
            Book book = createDraftBook();
            assertThrows(AuthorNotFoundException.class, () -> book.removeAuthor("ghost"));
        }
    }

    @Nested
    @DisplayName("Publisher Tests")
    class PublisherTests {

        @Test
        @DisplayName("should set publisher")
        void shouldSetPublisher() {
            Book book = createDraftBook();
            book.setPublisher("pub-1");
            assertEquals("pub-1", book.getPublisherId());
        }

        @Test
        @DisplayName("should remove publisher from draft")
        void shouldRemovePublisher() {
            Book book = createDraftBook();
            book.setPublisher("pub-1");
            book.removePublisher();
            assertNull(book.getPublisherId());
        }

        @Test
        @DisplayName("should not remove publisher from published book")
        void shouldNotRemoveFromPublished() {
            Book book = createPublishableBook();
            book.publish();
            assertThrows(InvalidOperationException.class, book::removePublisher);
        }
    }

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("should add category")
        void shouldAddCategory() {
            Book book = createDraftBook();
            book.addCategory("cat-1");
            assertEquals(1, book.getCategoryIds().size());
        }

        @Test
        @DisplayName("should reject duplicate category")
        void shouldRejectDuplicate() {
            Book book = createDraftBook();
            book.addCategory("cat-1");
            assertThrows(InvalidOperationException.class, () -> book.addCategory("cat-1"));
        }

        @Test
        @DisplayName("should remove category")
        void shouldRemoveCategory() {
            Book book = createDraftBook();
            book.addCategory("cat-1");
            book.removeCategory("cat-1");
            assertTrue(book.getCategoryIds().isEmpty());
        }
    }

    @Nested
    @DisplayName("Publishing Tests")
    class PublishingTests {

        @Test
        @DisplayName("should publish draft with all requirements")
        void shouldPublishDraft() {
            Book book = createPublishableBook();
            book.publish();
            assertEquals(BookStatus.PUBLISHED, book.getStatus());
        }

        @Test
        @DisplayName("should reject publish without author")
        void shouldRejectPublishWithoutAuthor() {
            Book book = createDraftBook();
            book.setPublisher("pub-1");
            book.addCategory("cat-1");
            assertThrows(InvalidOperationException.class, book::publish);
        }

        @Test
        @DisplayName("should reject publish without publisher")
        void shouldRejectPublishWithoutPublisher() {
            Book book = createDraftBook();
            book.addAuthor("a1", "Author", AuthorRole.AUTHOR);
            book.addCategory("cat-1");
            assertThrows(InvalidOperationException.class, book::publish);
        }

        @Test
        @DisplayName("should reject publish without category")
        void shouldRejectPublishWithoutCategory() {
            Book book = createDraftBook();
            book.addAuthor("a1", "Author", AuthorRole.AUTHOR);
            book.setPublisher("pub-1");
            assertThrows(InvalidOperationException.class, book::publish);
        }

        @Test
        @DisplayName("should unpublish")
        void shouldUnpublish() {
            Book book = createPublishableBook();
            book.publish();
            book.unpublish();
            assertEquals(BookStatus.UNPUBLISHED, book.getStatus());
        }

        @Test
        @DisplayName("should reject unpublish non-published")
        void shouldRejectUnpublishDraft() {
            Book book = createDraftBook();
            assertThrows(InvalidOperationException.class, book::unpublish);
        }

        @Test
        @DisplayName("should republish after unpublish")
        void shouldRepublish() {
            Book book = createPublishableBook();
            book.publish();
            book.unpublish();
            book.publish();
            assertEquals(BookStatus.PUBLISHED, book.getStatus());
        }
    }

    @Nested
    @DisplayName("Deletion Tests")
    class DeletionTests {

        @Test
        @DisplayName("should delete draft")
        void shouldDeleteDraft() {
            Book book = createDraftBook();
            book.delete();
            assertEquals(BookStatus.DELETED, book.getStatus());
        }

        @Test
        @DisplayName("should not delete published book")
        void shouldNotDeletePublished() {
            Book book = createPublishableBook();
            book.publish();
            assertThrows(InvalidOperationException.class, book::delete);
        }

        @Test
        @DisplayName("should delete unpublished book")
        void shouldDeleteUnpublished() {
            Book book = createPublishableBook();
            book.publish();
            book.unpublish();
            book.delete();
            assertEquals(BookStatus.DELETED, book.getStatus());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("should update basic info")
        void shouldUpdateBasicInfo() {
            Book book = createDraftBook();
            book.updateBasicInfo("New Title", "New desc", LocalDate.of(2020, 1, 1), 500, "fr");
            assertEquals("New Title", book.getTitle());
            assertEquals("New desc", book.getDescription());
            assertEquals(LocalDate.of(2020, 1, 1), book.getPublicationDate());
            assertEquals(500, book.getPageCount());
            assertEquals("fr", book.getLanguage());
        }

        @Test
        @DisplayName("should not update deleted book")
        void shouldNotUpdateDeleted() {
            Book book = createDraftBook();
            book.delete();
            assertThrows(InvalidOperationException.class, () ->
                book.updateBasicInfo("New", null, null, null, null));
        }

        @Test
        @DisplayName("should preserve values when null passed")
        void shouldPreserveOnNull() {
            Book book = createDraftBook();
            String origTitle = book.getTitle();
            book.updateBasicInfo(null, null, null, null, null);
            assertEquals(origTitle, book.getTitle());
        }
    }
}
