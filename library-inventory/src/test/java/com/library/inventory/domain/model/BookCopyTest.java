package com.library.inventory.domain.model;

import com.library.inventory.domain.exception.InvalidOperationException;
import com.library.inventory.domain.model.enums.CopyCondition;
import com.library.inventory.domain.model.enums.CopyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class BookCopyTest {

    private BookCopy createAvailableCopy() {
        Location location = Location.of("LIB01", 1, "A", "01", "B", "001");
        return BookCopy.create("inv-123", "BAR001", location, "PURCHASE");
    }

    @Test
    void shouldCreateCopyAsAvailable() {
        BookCopy copy = createAvailableCopy();

        assertThat(copy.getId()).isNotNull();
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(copy.getBarcode()).isEqualTo("BAR001");
        assertThat(copy.getCondition()).isEqualTo(CopyCondition.NEW);
        assertThat(copy.getBorrowCount()).isEqualTo(0);
        assertThat(copy.isAvailable()).isTrue();
    }

    @Test
    void shouldRejectBlankBarcode() {
        Location location = Location.of("LIB01", 1, "A", "01", "B", "001");
        assertThatThrownBy(() -> BookCopy.create("inv-1", "", location, "PURCHASE"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLongBarcode() {
        Location location = Location.of("LIB01", 1, "A", "01", "B", "001");
        assertThatThrownBy(() -> BookCopy.create("inv-1", "A".repeat(51), location, "PURCHASE"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMarkAsBorrowed() {
        BookCopy copy = createAvailableCopy();
        copy.markAsBorrowed();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.BORROWED);
        assertThat(copy.isBorrowed()).isTrue();
        assertThat(copy.isAvailable()).isFalse();
        assertThat(copy.getBorrowCount()).isEqualTo(1);
        assertThat(copy.getLastBorrowedDate()).isNotNull();
    }

    @Test
    void shouldMarkAsReturned() {
        BookCopy copy = createAvailableCopy();
        copy.markAsBorrowed();
        copy.markAsReturned();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(copy.isAvailable()).isTrue();
    }

    @Test
    void shouldRejectBorrowNonAvailable() {
        BookCopy copy = createAvailableCopy();
        copy.markAsBorrowed();

        assertThatThrownBy(copy::markAsBorrowed)
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldRejectReturnNonBorrowed() {
        BookCopy copy = createAvailableCopy();

        assertThatThrownBy(copy::markAsReturned)
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldMarkAsReserved() {
        BookCopy copy = createAvailableCopy();
        copy.markAsReserved();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.RESERVED);
    }

    @Test
    void shouldReleaseReservation() {
        BookCopy copy = createAvailableCopy();
        copy.markAsReserved();
        copy.releaseReservation();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void shouldMarkAsDamaged() {
        BookCopy copy = createAvailableCopy();
        copy.markAsDamaged("Water damage on pages");

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.DAMAGED);
        assertThat(copy.getCondition()).isEqualTo(CopyCondition.DAMAGED);
        assertThat(copy.getDamageDescription()).isEqualTo("Water damage on pages");
        assertThat(copy.isDamagedOrLost()).isTrue();
    }

    @Test
    void shouldMarkAsLost() {
        BookCopy copy = createAvailableCopy();
        copy.markAsLost("Cannot locate");

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOST);
        assertThat(copy.isDamagedOrLost()).isTrue();
    }

    @Test
    void shouldMarkAsUnderRepair() {
        BookCopy copy = createAvailableCopy();
        copy.markAsDamaged("Broken spine");
        copy.markAsUnderRepair();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.UNDER_REPAIR);
    }

    @Test
    void shouldMarkAsRepaired() {
        BookCopy copy = createAvailableCopy();
        copy.markAsDamaged("Broken spine");
        copy.markAsUnderRepair();
        copy.markAsRepaired();

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(copy.getCondition()).isEqualTo(CopyCondition.GOOD);
    }

    @Test
    void shouldRejectRepairNonDamaged() {
        BookCopy copy = createAvailableCopy();
        assertThatThrownBy(copy::markAsUnderRepair)
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldMarkAsRemoved() {
        BookCopy copy = createAvailableCopy();
        copy.markAsRemoved("Discarded");

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.REMOVED);
        assertThat(copy.getRemovalReason()).isEqualTo("Discarded");
    }

    @Test
    void shouldRejectRemoveBorrowedCopy() {
        BookCopy copy = createAvailableCopy();
        copy.markAsBorrowed();

        assertThatThrownBy(() -> copy.markAsRemoved("Discarded"))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldRejectRemoveAlreadyRemovedCopy() {
        BookCopy copy = createAvailableCopy();
        copy.markAsRemoved("Discarded");

        assertThatThrownBy(() -> copy.markAsRemoved("Again"))
            .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldCheckCanBeRemoved() {
        BookCopy copy = createAvailableCopy();
        assertThat(copy.canBeRemoved()).isTrue();

        copy.markAsBorrowed();
        assertThat(copy.canBeRemoved()).isFalse();
    }

    @Test
    void shouldCheckCanBeTransferred() {
        BookCopy copy = createAvailableCopy();
        assertThat(copy.canBeTransferred()).isTrue();

        copy.markAsBorrowed();
        assertThat(copy.canBeTransferred()).isFalse();
    }

    @Test
    void shouldUpdateLocation() {
        BookCopy copy = createAvailableCopy();
        Location newLocation = Location.of("LIB02", 2, "C", "03", "D", "005");
        copy.updateLocation(newLocation);

        assertThat(copy.getLocation()).isEqualTo(newLocation);
    }

    @Test
    void shouldUpdateCondition() {
        BookCopy copy = createAvailableCopy();
        copy.updateCondition(CopyCondition.GOOD);

        assertThat(copy.getCondition()).isEqualTo(CopyCondition.GOOD);
    }

    @Test
    void shouldSetAcquisitionCost() {
        BookCopy copy = createAvailableCopy();
        copy.setAcquisitionCost(new BigDecimal("29.99"));

        assertThat(copy.getAcquisitionCost()).isEqualByComparingTo(new BigDecimal("29.99"));
    }
}
