package com.library.integration.inventory;

import com.library.integration.BaseEndToEndTest;
import com.library.inventory.domain.model.BookCopy;
import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.model.Location;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.inventory.domain.repository.BookCopyRepository;
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
 * Cross-Context Test: Borrow Book Updates Inventory
 *
 * Flow: Circulation publishes BookBorrowedEvent →
 *   - Inventory consumes via BookBorrowedInventoryHandler
 *   - BookCopy status changes to BORROWED
 *   - CopyInventory available count decreases
 */
@DirtiesContext
class BorrowBookInventoryEndToEndTest extends BaseEndToEndTest {

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private CopyInventoryRepository copyInventoryRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    private BookCopy testCopy;
    private CopyInventory testInventory;

    @BeforeEach
    void setUp() {
        setUpKafka();
        cleanAllTables();

        // Setup: Library → CopyInventory → BookCopy(AVAILABLE)
        Library library = Library.create("MAIN-001", "Main Library");
        libraryRepository.save(library);

        testInventory = CopyInventory.create("book-001", library.getId().getValue(), "MAIN-001", "SYSTEM");
        testCopy = testInventory.addCopy("BAR-001", Location.simple("MAIN-001", "A01-A-001"), "SYSTEM");
        copyInventoryRepository.save(testInventory);
    }

    @Test
    void shouldChangeCopyStatusToBorrowed_whenBookBorrowedEvent() {
        // Given: a BookBorrowedEvent is published by Circulation
        String copyId = testCopy.getId().getValue();
        String eventJson = buildEventJson("BookBorrowedEvent",
            "loanId", idObject("loan-001"),
            "copyId", idObject(copyId),
            "patronId", idObject("patron-001"),
            "bookId", idObject("book-001"),
            "loanDate", jsonString("2026-06-01T10:00:00"),
            "dueDate", jsonString("2026-07-01T10:00:00")
        );

        // When
        sendEvent(CIRCULATION_TOPIC, eventJson);

        // Then: copy status should change to BORROWED
        await().atMost(10, SECONDS).untilAsserted(() -> {
            BookCopy updated = bookCopyRepository.findById(testCopy.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(CopyStatus.BORROWED);
        });

        // And: available copies should decrease
        await().atMost(5, SECONDS).untilAsserted(() -> {
            CopyInventory updatedInventory = copyInventoryRepository.findById(testInventory.getId()).orElseThrow();
            assertThat(updatedInventory.getAvailableCopies()).isEqualTo(0);
        });
    }
}
