package com.library.inventory.domain.service;

import com.library.inventory.domain.exception.DuplicateInventoryException;
import com.library.inventory.domain.exception.InventoryNotFoundException;
import com.library.inventory.domain.exception.LibraryNotFoundException;
import com.library.inventory.domain.exception.CopyNotFoundException;
import com.library.inventory.domain.exception.InvalidOperationException;
import com.library.inventory.domain.event.*;
import com.library.inventory.domain.model.BookCopy;
import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.model.Location;
import com.library.inventory.domain.repository.BookCopyRepository;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import com.library.inventory.infrastructure.messaging.InventoryDomainEventPublisher;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CopyId;
import com.library.shared.domain.model.CopyInventoryId;
import com.library.shared.domain.model.LibraryId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryManagementServiceTest {

    @Mock
    private CopyInventoryRepository inventoryRepository;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private InventoryDomainEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private InventoryManagementService service;

    private static final String BOOK_ID = "book-123";
    private static final String LIBRARY_ID = "lib-456";
    private static final String LIBRARY_CODE = "LIB01";
    private static final Location DEFAULT_LOCATION = Location.of("LIB01", 1, "A", "01", "B", "001");

    @BeforeEach
    void setUp() {
        service = new InventoryManagementService(
            inventoryRepository, copyRepository, libraryRepository, eventPublisher);
    }

    private Library createActiveLibrary() {
        return Library.create(LIBRARY_CODE, "Main Library");
    }

    private CopyInventory createTestInventory() {
        return CopyInventory.create(BOOK_ID, LIBRARY_ID, LIBRARY_CODE, "admin");
    }

    // --- createInitialInventory ---

    @Test
    void shouldCreateInitialInventory() {
        Library library = createActiveLibrary();
        when(libraryRepository.findById(LibraryId.of(LIBRARY_ID))).thenReturn(Optional.of(library));
        when(inventoryRepository.existsByBookIdAndLibraryId(BOOK_ID, LIBRARY_ID)).thenReturn(false);
        when(inventoryRepository.save(any(CopyInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        CopyInventory result = service.createInitialInventory(
            BOOK_ID, LIBRARY_ID, 3, DEFAULT_LOCATION, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getBookId()).isEqualTo(BOOK_ID);
        assertThat(result.getLibraryId()).isEqualTo(LIBRARY_ID);
        assertThat(result.getTotalCopies()).isEqualTo(3);

        verify(eventPublisher).publish(any(InventoryCreatedEvent.class));
    }

    @Test
    void shouldCreateInitialInventoryWithZeroCopies() {
        Library library = createActiveLibrary();
        when(libraryRepository.findById(LibraryId.of(LIBRARY_ID))).thenReturn(Optional.of(library));
        when(inventoryRepository.existsByBookIdAndLibraryId(BOOK_ID, LIBRARY_ID)).thenReturn(false);
        when(inventoryRepository.save(any(CopyInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        CopyInventory result = service.createInitialInventory(
            BOOK_ID, LIBRARY_ID, 0, DEFAULT_LOCATION, "admin");

        assertThat(result.getTotalCopies()).isEqualTo(0);
        verify(eventPublisher).publish(any(InventoryCreatedEvent.class));
    }

    @Test
    void shouldRejectCreateInventoryForNonExistentLibrary() {
        when(libraryRepository.findById(LibraryId.of(LIBRARY_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInitialInventory(
                BOOK_ID, LIBRARY_ID, 1, DEFAULT_LOCATION, "admin"))
            .isInstanceOf(LibraryNotFoundException.class);

        verify(inventoryRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectCreateInventoryForInactiveLibrary() {
        Library library = createActiveLibrary();
        library.deactivate();
        when(libraryRepository.findById(LibraryId.of(LIBRARY_ID))).thenReturn(Optional.of(library));

        assertThatThrownBy(() -> service.createInitialInventory(
                BOOK_ID, LIBRARY_ID, 1, DEFAULT_LOCATION, "admin"))
            .isInstanceOf(InvalidOperationException.class);

        verify(inventoryRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectDuplicateInventory() {
        Library library = createActiveLibrary();
        when(libraryRepository.findById(LibraryId.of(LIBRARY_ID))).thenReturn(Optional.of(library));
        when(inventoryRepository.existsByBookIdAndLibraryId(BOOK_ID, LIBRARY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createInitialInventory(
                BOOK_ID, LIBRARY_ID, 1, DEFAULT_LOCATION, "admin"))
            .isInstanceOf(DuplicateInventoryException.class);

        verify(inventoryRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // --- addCopyToInventory ---

    @Test
    void shouldAddCopyToInventory() {
        CopyInventory inventory = createTestInventory();
        String inventoryId = inventory.getId().getValue();

        when(inventoryRepository.findById(CopyInventoryId.of(inventoryId)))
            .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(CopyInventory.class))).thenReturn(inventory);

        BookCopy result = service.addCopyToInventory(
            inventoryId, DEFAULT_LOCATION, "PURCHASE", new BigDecimal("29.99"));

        assertThat(result).isNotNull();
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getAcquisitionCost()).isEqualByComparingTo(new BigDecimal("29.99"));

        verify(eventPublisher).publish(any(CopyAddedEvent.class));
    }

    @Test
    void shouldRejectAddCopyToNonExistentInventory() {
        String fakeId = "non-existent";
        when(inventoryRepository.findById(CopyInventoryId.of(fakeId)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCopyToInventory(
                fakeId, DEFAULT_LOCATION, "PURCHASE", null))
            .isInstanceOf(InventoryNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }

    // --- batchAddCopies ---

    @Test
    void shouldBatchAddCopies() {
        CopyInventory inventory = createTestInventory();
        String inventoryId = inventory.getId().getValue();

        when(inventoryRepository.findById(CopyInventoryId.of(inventoryId)))
            .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(CopyInventory.class))).thenReturn(inventory);

        List<BookCopy> result = service.batchAddCopies(
            inventoryId, 5, DEFAULT_LOCATION, "DONATION", new BigDecimal("19.99"));

        assertThat(result).hasSize(5);
        assertThat(result.stream().allMatch(BookCopy::isAvailable)).isTrue();
        result.forEach(copy ->
            assertThat(copy.getAcquisitionCost()).isEqualByComparingTo(new BigDecimal("19.99")));

        verify(eventPublisher).publish(any(CopiesBatchAddedEvent.class));
    }

    @Test
    void shouldRejectBatchAddToNonExistentInventory() {
        String fakeId = "non-existent";
        when(inventoryRepository.findById(CopyInventoryId.of(fakeId)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.batchAddCopies(
                fakeId, 3, DEFAULT_LOCATION, "PURCHASE", null))
            .isInstanceOf(InventoryNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }

    // --- checkoutCopy ---

    @Test
    void shouldCheckoutCopy() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(1, DEFAULT_LOCATION, "PURCHASE");
        BookCopy copy = inventory.getCopies().get(0);

        when(copyRepository.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId())))
            .thenReturn(Optional.of(inventory));

        service.checkoutCopy(copy.getId().getValue());

        assertThat(copy.getStatus()).isEqualTo(
            com.library.inventory.domain.model.enums.CopyStatus.BORROWED);

        verify(eventPublisher).publish(any(CopyBorrowedEvent.class));
    }

    @Test
    void shouldRejectCheckoutNonExistentCopy() {
        String fakeCopyId = "non-existent";
        when(copyRepository.findById(CopyId.of(fakeCopyId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkoutCopy(fakeCopyId))
            .isInstanceOf(CopyNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }

    // --- returnCopy ---

    @Test
    void shouldReturnCopy() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(1, DEFAULT_LOCATION, "PURCHASE");
        BookCopy copy = inventory.getCopies().get(0);
        copy.markAsBorrowed();

        when(copyRepository.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId())))
            .thenReturn(Optional.of(inventory));

        service.returnCopy(copy.getId().getValue());

        assertThat(copy.getStatus()).isEqualTo(
            com.library.inventory.domain.model.enums.CopyStatus.AVAILABLE);

        verify(eventPublisher).publish(any(CopyReturnedEvent.class));
    }

    @Test
    void shouldRejectReturnNonExistentCopy() {
        String fakeCopyId = "non-existent";
        when(copyRepository.findById(CopyId.of(fakeCopyId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnCopy(fakeCopyId))
            .isInstanceOf(CopyNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }

    // --- reportCopyDamage ---

    @Test
    void shouldReportCopyDamage() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(1, DEFAULT_LOCATION, "PURCHASE");
        BookCopy copy = inventory.getCopies().get(0);

        when(copyRepository.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId())))
            .thenReturn(Optional.of(inventory));

        service.reportCopyDamage(copy.getId().getValue(), "Water damage on pages");

        assertThat(copy.getStatus()).isEqualTo(
            com.library.inventory.domain.model.enums.CopyStatus.DAMAGED);
        assertThat(copy.getDamageDescription()).isEqualTo("Water damage on pages");

        verify(eventPublisher).publish(any(CopyDamagedEvent.class));
    }

    @Test
    void shouldRejectDamageReportForNonExistentCopy() {
        String fakeCopyId = "non-existent";
        when(copyRepository.findById(CopyId.of(fakeCopyId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportCopyDamage(fakeCopyId, "damage"))
            .isInstanceOf(CopyNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }

    // --- reportCopyLoss ---

    @Test
    void shouldReportCopyLoss() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(1, DEFAULT_LOCATION, "PURCHASE");
        BookCopy copy = inventory.getCopies().get(0);

        when(copyRepository.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId())))
            .thenReturn(Optional.of(inventory));

        service.reportCopyLoss(copy.getId().getValue(), "Cannot locate on shelf");

        assertThat(copy.getStatus()).isEqualTo(
            com.library.inventory.domain.model.enums.CopyStatus.LOST);
        assertThat(copy.getLostReason()).isEqualTo("Cannot locate on shelf");

        verify(eventPublisher).publish(any(CopyLostEvent.class));
    }

    @Test
    void shouldRejectLossReportForNonExistentCopy() {
        String fakeCopyId = "non-existent";
        when(copyRepository.findById(CopyId.of(fakeCopyId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportCopyLoss(fakeCopyId, "lost"))
            .isInstanceOf(CopyNotFoundException.class);

        verify(eventPublisher, never()).publish(any());
    }
}
