package com.library.circulation.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CirculationPolicyTest {

    @Test
    void shouldCreateStandardPolicy() {
        CirculationPolicy policy = CirculationPolicy.standard();
        assertEquals(30, policy.getLoanPeriodDays());
        assertEquals(2, policy.getMaxRenewalsAllowed());
        assertEquals(new BigDecimal("0.50"), policy.getDailyFineRate());
        assertEquals(new BigDecimal("50.00"), policy.getMaxFineAmount());
    }

    @Test
    void shouldCreateFacultyPolicy() {
        CirculationPolicy policy = CirculationPolicy.faculty();
        assertEquals(90, policy.getLoanPeriodDays());
        assertEquals(3, policy.getMaxRenewalsAllowed());
    }

    @Test
    void shouldCalculateFine() {
        CirculationPolicy policy = CirculationPolicy.standard();
        BigDecimal fine = policy.calculateFine(5);
        assertEquals(new BigDecimal("2.50"), fine);
    }

    @Test
    void shouldCapFineAtMax() {
        CirculationPolicy policy = CirculationPolicy.standard();
        BigDecimal fine = policy.calculateFine(200);
        assertEquals(new BigDecimal("50.00"), fine);
    }

    @Test
    void shouldDetectGracePeriod() {
        CirculationPolicy policy = CirculationPolicy.standard();
        assertTrue(policy.isInGracePeriod(2));
        assertTrue(policy.isInGracePeriod(3));
        assertFalse(policy.isInGracePeriod(4));
        assertFalse(policy.isInGracePeriod(0));
    }

    @Test
    void shouldThrowForInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new CirculationPolicy(null, 2, BigDecimal.ONE, BigDecimal.TEN, 0, 7, 5, 7, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new CirculationPolicy(30, -1, BigDecimal.ONE, BigDecimal.TEN, 0, 7, 5, 7, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new CirculationPolicy(30, 2, BigDecimal.ZERO, BigDecimal.TEN, 0, 7, 5, 7, 3));
    }
}
