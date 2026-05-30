package com.library.circulation.domain.model;

import com.library.circulation.domain.exception.InvalidOperationException;
import com.library.circulation.domain.model.enums.HoldStatus;
import com.library.shared.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HoldTest {

    private Hold createWaitingHold() {
        return new Hold(HoldId.generate(), BookId.generate(), PatronId.generate(),
                LocalDateTime.now(), LocalDateTime.now().plusDays(7), 1);
    }

    @Test
    void shouldCreateHoldWithWaitingStatus() {
        Hold hold = createWaitingHold();
        assertEquals(HoldStatus.WAITING, hold.getStatus());
        assertEquals(1, hold.getQueuePosition());
        assertFalse(hold.isReadyForPickup());
    }

    @Test
    void shouldFulfillHold() {
        Hold hold = createWaitingHold();
        CopyId copyId = CopyId.generate();
        hold.fulfill(copyId, LocalDateTime.now(), 5);
        assertEquals(HoldStatus.READY_FOR_PICKUP, hold.getStatus());
        assertEquals(copyId, hold.getFulfilledCopyId());
        assertNotNull(hold.getAvailableUntilDate());
        assertTrue(hold.isReadyForPickup());
    }

    @Test
    void shouldThrowWhenFulfillingNonWaitingHold() {
        Hold hold = createWaitingHold();
        hold.cancel("no longer needed");
        assertThrows(InvalidOperationException.class,
                () -> hold.fulfill(CopyId.generate(), LocalDateTime.now(), 5));
    }

    @Test
    void shouldCancelWaitingHold() {
        Hold hold = createWaitingHold();
        hold.cancel("changed mind");
        assertEquals(HoldStatus.CANCELLED, hold.getStatus());
        assertEquals("changed mind", hold.getCancelReason());
    }

    @Test
    void shouldThrowWhenCancellingFulfilledHold() {
        Hold hold = createWaitingHold();
        hold.fulfill(CopyId.generate(), LocalDateTime.now(), 5);
        hold.markAsPickedUp(LocalDateTime.now());
        assertThrows(InvalidOperationException.class, () -> hold.cancel("reason"));
    }

    @Test
    void shouldMarkAsPickedUp() {
        Hold hold = createWaitingHold();
        hold.fulfill(CopyId.generate(), LocalDateTime.now(), 5);
        hold.markAsPickedUp(LocalDateTime.now());
        assertEquals(HoldStatus.FULFILLED, hold.getStatus());
    }

    @Test
    void shouldThrowWhenPickupExpired() {
        Hold hold = createWaitingHold();
        hold.fulfill(CopyId.generate(), LocalDateTime.now().minusDays(10), 5);
        assertThrows(InvalidOperationException.class,
                () -> hold.markAsPickedUp(LocalDateTime.now()));
    }

    @Test
    void shouldMarkAsExpiredNotPickedUp() {
        Hold hold = createWaitingHold();
        hold.fulfill(CopyId.generate(), LocalDateTime.now(), 5);
        hold.markAsExpiredNotPickedUp();
        assertEquals(HoldStatus.EXPIRED_NOT_PICKED_UP, hold.getStatus());
    }

    @Test
    void shouldMarkAsExpired() {
        Hold hold = createWaitingHold();
        hold.markAsExpired();
        assertEquals(HoldStatus.EXPIRED, hold.getStatus());
    }

    @Test
    void shouldUpdateQueuePosition() {
        Hold hold = createWaitingHold();
        hold.updateQueuePosition(3);
        assertEquals(3, hold.getQueuePosition());
    }

    @Test
    void shouldThrowForInvalidQueuePosition() {
        Hold hold = createWaitingHold();
        assertThrows(IllegalArgumentException.class, () -> hold.updateQueuePosition(0));
    }

    @Test
    void shouldExtendExpiration() {
        Hold hold = createWaitingHold();
        LocalDateTime oldExpiry = hold.getExpirationDate();
        hold.extendExpiration(7);
        assertEquals(oldExpiry.plusDays(7), hold.getExpirationDate());
    }

    @Test
    void shouldDetectExpiration() {
        Hold hold = new Hold(HoldId.generate(), BookId.generate(), PatronId.generate(),
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1), 1);
        assertTrue(hold.isExpired(LocalDateTime.now()));
        assertFalse(hold.isExpired(LocalDateTime.now().minusDays(5)));
    }

    @Test
    void shouldDetectPickupExpired() {
        Hold hold = createWaitingHold();
        assertFalse(hold.isPickupExpired(LocalDateTime.now()));
        hold.fulfill(CopyId.generate(), LocalDateTime.now().minusDays(10), 5);
        assertTrue(hold.isPickupExpired(LocalDateTime.now()));
    }

    @Test
    void shouldCreateHoldViaFactoryMethod() {
        Hold hold = Hold.create(BookId.generate(), PatronId.generate(), LocalDateTime.now(), 2, 7);
        assertNotNull(hold.getId());
        assertEquals(HoldStatus.WAITING, hold.getStatus());
        assertEquals(2, hold.getQueuePosition());
    }

    @Test
    void shouldSetPickupLibraryId() {
        Hold hold = createWaitingHold();
        hold.setPickupLibraryId("lib-123");
        assertEquals("lib-123", hold.getPickupLibraryId());
    }

    @Test
    void shouldMarkNotificationSent() {
        Hold hold = createWaitingHold();
        assertFalse(hold.getNotificationSent());
        hold.markNotificationSent();
        assertTrue(hold.getNotificationSent());
    }
}
