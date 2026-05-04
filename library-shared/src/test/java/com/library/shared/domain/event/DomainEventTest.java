package com.library.shared.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    static class TestEvent extends DomainEvent {
        private final String payload;

        TestEvent(String payload) {
            super();
            this.payload = payload;
        }

        TestEvent(String eventId, LocalDateTime occurredAt, int version, String payload) {
            super(eventId, occurredAt, version);
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("should create event with default values")
        void shouldCreateWithDefaults() {
            TestEvent event = new TestEvent("test");
            assertNotNull(event.getEventId());
            assertNotNull(event.getOccurredAt());
            assertEquals(1, event.getVersion());
            assertEquals("TestEvent", event.getEventType());
        }

        @Test
        @DisplayName("should create event with custom values")
        void shouldCreateWithCustomValues() {
            LocalDateTime now = LocalDateTime.now();
            TestEvent event = new TestEvent("evt-123", now, 2, "data");
            assertEquals("evt-123", event.getEventId());
            assertEquals(now, event.getOccurredAt());
            assertEquals(2, event.getVersion());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("should be equal by eventId")
        void shouldBeEqualByEventId() {
            LocalDateTime now = LocalDateTime.now();
            TestEvent e1 = new TestEvent("same-id", now, 1, "a");
            TestEvent e2 = new TestEvent("same-id", now, 2, "b");
            assertEquals(e1, e2);
        }

        @Test
        @DisplayName("should not be equal for different eventId")
        void shouldNotBeEqualForDifferentId() {
            TestEvent e1 = new TestEvent("id-1");
            TestEvent e2 = new TestEvent("id-2");
            assertNotEquals(e1, e2);
        }
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceToString() {
        TestEvent event = new TestEvent("data");
        assertTrue(event.toString().startsWith("TestEvent{"));
        assertTrue(event.toString().contains("eventId="));
    }
}
