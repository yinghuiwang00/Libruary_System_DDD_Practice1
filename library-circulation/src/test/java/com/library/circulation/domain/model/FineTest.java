package com.library.circulation.domain.model;

import com.library.shared.domain.model.FineId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FineTest {

    private Fine createFine() {
        return new Fine(FineId.generate(), new BigDecimal("10.00"), 5, LocalDateTime.now());
    }

    @Test
    void shouldCreateFine() {
        Fine fine = createFine();
        assertEquals(new BigDecimal("10.00"), fine.getAmount());
        assertEquals(5, fine.getOverdueDays());
        assertTrue(fine.isOutstanding());
        assertFalse(fine.isPaid());
        assertFalse(fine.isWaived());
    }

    @Test
    void shouldPayFine() {
        Fine fine = createFine();
        fine.pay(new BigDecimal("10.00"), LocalDateTime.now());
        assertTrue(fine.isPaid());
        assertFalse(fine.isOutstanding());
    }

    @Test
    void shouldThrowWhenPayingAlreadyPaid() {
        Fine fine = createFine();
        fine.pay(new BigDecimal("10.00"), LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> fine.pay(new BigDecimal("10.00"), LocalDateTime.now()));
    }

    @Test
    void shouldThrowWhenPaymentAmountIsLessThanFine() {
        Fine fine = createFine();
        assertThrows(IllegalArgumentException.class,
                () -> fine.pay(new BigDecimal("5.00"), LocalDateTime.now()));
    }

    @Test
    void shouldThrowWhenPaymentAmountIsZero() {
        Fine fine = createFine();
        assertThrows(IllegalArgumentException.class,
                () -> fine.pay(BigDecimal.ZERO, LocalDateTime.now()));
    }

    @Test
    void shouldThrowWhenPaymentAmountIsNull() {
        Fine fine = createFine();
        assertThrows(NullPointerException.class,
                () -> fine.pay(null, LocalDateTime.now()));
    }

    @Test
    void shouldWaiveFine() {
        Fine fine = createFine();
        fine.waive("system error", LocalDateTime.now());
        assertTrue(fine.isWaived());
        assertFalse(fine.isOutstanding());
    }

    @Test
    void shouldThrowWhenWaivingPaidFine() {
        Fine fine = createFine();
        fine.pay(new BigDecimal("10.00"), LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> fine.waive("reason", LocalDateTime.now()));
    }

    @Test
    void shouldThrowWhenWaivingAlreadyWaived() {
        Fine fine = createFine();
        fine.waive("reason", LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> fine.waive("again", LocalDateTime.now()));
    }

    @Test
    void shouldThrowForNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Fine(FineId.generate(), new BigDecimal("-5.00"), 5, LocalDateTime.now()));
    }

    @Test
    void shouldThrowForNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Fine(FineId.generate(), null, 5, LocalDateTime.now()));
    }
}
