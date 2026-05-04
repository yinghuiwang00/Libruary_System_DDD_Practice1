package com.library.inventory.domain.model;

import com.library.inventory.domain.exception.InvalidOperationException;
import com.library.inventory.domain.exception.NoAvailableCopyException;
import com.library.inventory.domain.model.enums.CopyStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CopyInventoryTest {

    private Location defaultLocation = Location.of("LIB01", 1, "A", "01", "B", "001");

    private CopyInventory createTestInventory() {
        return CopyInventory.create("book-123", "lib-456", "LIB01", "admin");
    }

    @Test
    void shouldCreateInventory() {
        CopyInventory inventory = createTestInventory();

        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.getBookId()).isEqualTo("book-123");
        assertThat(inventory.getLibraryId()).isEqualTo("lib-456");
        assertThat(inventory.getLibraryCode()).isEqualTo("LIB01");
        assertThat(inventory.getTotalCopies()).isEqualTo(0);
        assertThat(inventory.getAvailableCopies()).isEqualTo(0);
    }

    @Test
    void shouldAddCopy() {
        CopyInventory inventory = createTestInventory();
        BookCopy copy = inventory.addCopy("BAR001", defaultLocation, "PURCHASE");

        assertThat(inventory.getTotalCopies()).isEqualTo(1);
        assertThat(inventory.getAvailableCopies()).isEqualTo(1);
        assertThat(copy).isNotNull();
        assertThat(copy.isAvailable()).isTrue();
    }

    @Test
    void shouldGenerateBarcodeWhenAddingCopies() {
        CopyInventory inventory = createTestInventory();
        BookCopy copy = inventory.addCopy("BAR001", defaultLocation, "PURCHASE");

        assertThat(copy.getBarcode()).isEqualTo("BAR001");
    }

    @Test
    void shouldAddMultipleCopies() {
        CopyInventory inventory = createTestInventory();
        var copies = inventory.addCopies(5, defaultLocation, "PURCHASE");

        assertThat(inventory.getTotalCopies()).isEqualTo(5);
        assertThat(inventory.getAvailableCopies()).isEqualTo(5);
        assertThat(copies).hasSize(5);
    }

    @Test
    void shouldRejectZeroOrNegativeCopiesCount() {
        CopyInventory inventory = createTestInventory();

        assertThatThrownBy(() -> inventory.addCopies(0, defaultLocation, "PURCHASE"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.addCopies(-1, defaultLocation, "PURCHASE"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRemoveCopy() {
        CopyInventory inventory = createTestInventory();
        BookCopy copy = inventory.addCopy("BAR001", defaultLocation, "PURCHASE");

        inventory.removeCopy(copy.getId().getValue(), "Damaged beyond repair");

        assertThat(inventory.getTotalCopies()).isEqualTo(0);
        assertThat(inventory.getAvailableCopies()).isEqualTo(0);
    }

    @Test
    void shouldRejectRemoveBorrowedCopy() {
        CopyInventory inventory = createTestInventory();
        BookCopy copy = inventory.addCopy("BAR001", defaultLocation, "PURCHASE");
        copy.markAsBorrowed();

        assertThatThrownBy(() -> inventory.removeCopy(copy.getId().getValue(), "test"))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldGetAvailableCopy() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(3, defaultLocation, "PURCHASE");

        BookCopy available = inventory.getAvailableCopy();
        assertThat(available).isNotNull();
        assertThat(available.isAvailable()).isTrue();
    }

    @Test
    void shouldThrowWhenNoAvailableCopies() {
        CopyInventory inventory = createTestInventory();

        assertThatThrownBy(inventory::getAvailableCopy)
            .isInstanceOf(NoAvailableCopyException.class);
    }

    @Test
    void shouldTrackAvailabilityCorrectly() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(5, defaultLocation, "PURCHASE");

        BookCopy copy1 = inventory.getCopies().get(0);
        BookCopy copy2 = inventory.getCopies().get(1);

        // Borrow 2 copies
        CopyStatus old1 = copy1.getStatus();
        copy1.markAsBorrowed();
        inventory.onCopyStatusChanged(old1, copy1.getStatus());

        CopyStatus old2 = copy2.getStatus();
        copy2.markAsBorrowed();
        inventory.onCopyStatusChanged(old2, copy2.getStatus());

        assertThat(inventory.getAvailableCopies()).isEqualTo(3);
        assertThat(inventory.getTotalCopies()).isEqualTo(5);
    }

    @Test
    void shouldCalculateAvailabilityRate() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(5, defaultLocation, "PURCHASE");

        BookCopy copy = inventory.getCopies().get(0);
        CopyStatus old = copy.getStatus();
        copy.markAsBorrowed();
        inventory.onCopyStatusChanged(old, copy.getStatus());

        double rate = inventory.getAvailabilityRate();
        assertThat(rate).isCloseTo(0.8, within(0.001));
    }

    @Test
    void shouldReturnZeroRateForEmptyInventory() {
        CopyInventory inventory = createTestInventory();
        assertThat(inventory.getAvailabilityRate()).isEqualTo(0.0);
    }

    @Test
    void shouldDetectLowStock() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(2, defaultLocation, "PURCHASE");

        BookCopy copy1 = inventory.getCopies().get(0);
        CopyStatus old1 = copy1.getStatus();
        copy1.markAsBorrowed();
        inventory.onCopyStatusChanged(old1, copy1.getStatus());

        assertThat(inventory.isLowStock()).isTrue();
    }

    @Test
    void shouldNotBeLowStockWhenSufficient() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopies(5, defaultLocation, "PURCHASE");

        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    void shouldReturnUnmodifiableCopies() {
        CopyInventory inventory = createTestInventory();
        inventory.addCopy("BAR001", defaultLocation, "PURCHASE");

        assertThatThrownBy(() -> inventory.getCopies().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldIncrementAvailableOnReturn() {
        CopyInventory inventory = createTestInventory();
        BookCopy copy = inventory.addCopy("BAR001", defaultLocation, "PURCHASE");

        CopyStatus old1 = copy.getStatus();
        copy.markAsBorrowed();
        inventory.onCopyStatusChanged(old1, copy.getStatus());
        assertThat(inventory.getAvailableCopies()).isEqualTo(0);

        CopyStatus old2 = copy.getStatus();
        copy.markAsReturned();
        inventory.onCopyStatusChanged(old2, copy.getStatus());
        assertThat(inventory.getAvailableCopies()).isEqualTo(1);
    }
}
