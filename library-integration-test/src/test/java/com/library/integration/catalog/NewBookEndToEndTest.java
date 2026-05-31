package com.library.integration.catalog;

import com.library.integration.BaseEndToEndTest;
import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * UC-6: New Book Cataloging End-to-End Test
 *
 * Flow: Catalog publishes BookCreatedEvent →
 *   - Inventory creates an inventory record for the new book (requires a Library entity)
 *
 * Note: BookCreatedAnalyticsHandler currently only logs, so we don't verify AnalyticsReport creation.
 */
@DirtiesContext
class NewBookEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private CopyInventoryRepository copyInventoryRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        // BookCreatedEventHandler requires a Library with code "MAIN-LIB-001"
        Library library = Library.create("MAIN-LIB-001", "Main Library");
        libraryRepository.save(library);
    }

    @Test
    void shouldCreateInventoryRecord_whenBookCreatedEvent() {
        String bookId = "book-new-001";
        String eventJson = buildEventJson("BookCreatedEvent",
            "bookId", jsonString(bookId),
            "isbn", jsonString("9780134685991"),
            "title", jsonString("Effective Java")
        );

        sendEvent(CATALOG_TOPIC, eventJson);

        // Inventory record should be created for the new book
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<CopyInventory> inventories = copyInventoryRepository.findByBookId(bookId);
            assertThat(inventories).isNotEmpty();
            assertThat(inventories.get(0).getTotalCopies()).isEqualTo(0);
        });
    }
}
