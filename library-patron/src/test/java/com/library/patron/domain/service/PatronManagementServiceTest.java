package com.library.patron.domain.service;

import com.library.patron.domain.event.*;
import com.library.patron.domain.exception.DuplicateEmailException;
import com.library.patron.domain.exception.PatronNotFoundException;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.MembershipStatus;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.PatronId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatronManagementServiceTest {

    @Mock
    private PatronRepository patronRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private PatronManagementService service;

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PHONE = "555-1234";
    private static final String ADDRESS = "123 Main St";
    private static final String CITY = "Springfield";
    private static final String POSTAL = "62701";

    @BeforeEach
    void setUp() {
        service = new PatronManagementService(patronRepository, eventPublisher);
    }

    private Patron createTestPatron() {
        return Patron.create(FIRST_NAME, LAST_NAME, EMAIL, PatronType.STUDENT);
    }

    private Patron createTestPatronWithId(PatronId id) {
        return new Patron(id, FIRST_NAME, LAST_NAME, EMAIL, PatronType.STUDENT);
    }

    // ---------------------------------------------------------------------------
    // registerPatron
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("registerPatron")
    class RegisterPatron {

        @Test
        @DisplayName("should register a new patron successfully")
        void shouldRegister() {
            when(patronRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(patronRepository.save(any(Patron.class))).thenAnswer(inv -> inv.getArgument(0));

            Patron result = service.registerPatron(
                FIRST_NAME, LAST_NAME, EMAIL, PHONE, ADDRESS, CITY, POSTAL, PatronType.STUDENT
            );

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);

            verify(patronRepository).existsByEmail(EMAIL);
            verify(patronRepository).save(any(Patron.class));
            verify(eventPublisher).publish(any(PatronRegisteredEvent.class));
        }

        @Test
        @DisplayName("should throw DuplicateEmailException for existing email")
        void shouldThrowOnDuplicateEmail() {
            when(patronRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> service.registerPatron(
                FIRST_NAME, LAST_NAME, EMAIL, PHONE, ADDRESS, CITY, POSTAL, PatronType.STUDENT
            ))
                .isInstanceOf(DuplicateEmailException.class);

            verify(patronRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("should publish PatronRegisteredEvent with correct data")
        void shouldPublishEvent() {
            when(patronRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(patronRepository.save(any(Patron.class))).thenAnswer(inv -> inv.getArgument(0));

            service.registerPatron(FIRST_NAME, LAST_NAME, EMAIL, null, null, null, null, PatronType.FACULTY);

            ArgumentCaptor<PatronRegisteredEvent> captor = ArgumentCaptor.forClass(PatronRegisteredEvent.class);
            verify(eventPublisher).publish(captor.capture());

            PatronRegisteredEvent event = captor.getValue();
            assertThat(event.getFullName()).isEqualTo(FIRST_NAME + " " + LAST_NAME);
            assertThat(event.getEmail()).isEqualTo(EMAIL);
            assertThat(event.getPatronType()).isEqualTo(PatronType.FACULTY);
        }
    }

    // ---------------------------------------------------------------------------
    // updatePatronInfo
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("updatePatronInfo")
    class UpdatePatronInfo {

        @Test
        @DisplayName("should update patron info successfully")
        void shouldUpdate() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            Patron result = service.updatePatronInfo(
                id, "Jane", "Smith", "jane@example.com", PHONE, ADDRESS, CITY, POSTAL
            );

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            verify(eventPublisher).publish(any(PatronUpdatedEvent.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePatronInfo(
                id, FIRST_NAME, LAST_NAME, EMAIL, null, null, null, null
            ))
                .isInstanceOf(PatronNotFoundException.class);
        }

        @Test
        @DisplayName("should throw DuplicateEmailException when new email already used")
        void shouldThrowDuplicateEmail() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.existsByEmail("other@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.updatePatronInfo(
                id, FIRST_NAME, LAST_NAME, "other@example.com", null, null, null, null
            ))
                .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("should not check email uniqueness when email unchanged")
        void shouldSkipEmailCheckWhenSame() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.updatePatronInfo(id, "Jane", "Smith", EMAIL, null, null, null, null);

            verify(patronRepository, never()).existsByEmail(anyString());
        }
    }

    // ---------------------------------------------------------------------------
    // suspendPatron
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("suspendPatron")
    class SuspendPatron {

        @Test
        @DisplayName("should suspend patron successfully")
        void shouldSuspend() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.suspendPatron(id, "violation");

            verify(eventPublisher).publish(any(PatronSuspendedEvent.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.suspendPatron(id, "violation"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // reactivatePatron
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("reactivatePatron")
    class ReactivatePatron {

        @Test
        @DisplayName("should reactivate patron successfully")
        void shouldReactivate() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            patron.suspend("violation");
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.reactivatePatron(id, "resolved");

            verify(eventPublisher).publish(any(PatronReactivatedEvent.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reactivatePatron(id, "resolved"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // terminatePatron
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("terminatePatron")
    class TerminatePatron {

        @Test
        @DisplayName("should terminate patron successfully")
        void shouldTerminate() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.terminatePatron(id, "leaving");

            verify(eventPublisher).publish(any(PatronTerminatedEvent.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.terminatePatron(id, "leaving"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // addFine
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("addFine")
    class AddFine {

        @Test
        @DisplayName("should add fine to patron")
        void shouldAddFine() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.addFine(id, new BigDecimal("10.00"), "overdue");

            verify(patronRepository).save(any(Patron.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addFine(id, new BigDecimal("10.00"), "overdue"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // payFine
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("payFine")
    class PayFine {

        @Test
        @DisplayName("should pay fine for patron")
        void shouldPayFine() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            patron.addFine(new BigDecimal("20.00"));
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.payFine(id, new BigDecimal("5.00"));

            verify(patronRepository).save(any(Patron.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.payFine(id, new BigDecimal("5.00")))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // waiveFine
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("waiveFine")
    class WaiveFine {

        @Test
        @DisplayName("should waive fine for patron")
        void shouldWaiveFine() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            patron.addFine(new BigDecimal("20.00"));
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.waiveFine(id, new BigDecimal("10.00"), "goodwill");

            verify(patronRepository).save(any(Patron.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.waiveFine(id, new BigDecimal("5.00"), "goodwill"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // changePatronType
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("changePatronType")
    class ChangePatronType {

        @Test
        @DisplayName("should change patron type successfully")
        void shouldChangeType() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.changePatronType(id, PatronType.FACULTY);

            ArgumentCaptor<PatronTypeChangedEvent> captor = ArgumentCaptor.forClass(PatronTypeChangedEvent.class);
            verify(eventPublisher).publish(captor.capture());

            PatronTypeChangedEvent event = captor.getValue();
            assertThat(event.getOldType()).isEqualTo(PatronType.STUDENT);
            assertThat(event.getNewType()).isEqualTo(PatronType.FACULTY);
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changePatronType(id, PatronType.FACULTY))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // extendMembership
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("extendMembership")
    class ExtendMembership {

        @Test
        @DisplayName("should extend membership successfully")
        void shouldExtend() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));
            when(patronRepository.save(any(Patron.class))).thenReturn(patron);

            service.extendMembership(id, 12, "annual renewal");

            verify(patronRepository).save(any(Patron.class));
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when patron not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.extendMembership(id, 12, "renewal"))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // getPatron
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("getPatron")
    class GetPatron {

        @Test
        @DisplayName("should return patron when found")
        void shouldReturnPatron() {
            PatronId id = PatronId.generate();
            Patron patron = createTestPatronWithId(id);
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.of(patron));

            Patron result = service.getPatron(id);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("should throw PatronNotFoundException when not found")
        void shouldThrowNotFound() {
            PatronId id = PatronId.generate();
            when(patronRepository.findById(any(PatronId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPatron(id))
                .isInstanceOf(PatronNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------------------
    // getAllPatrons
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllPatrons")
    class GetAllPatrons {

        @Test
        @DisplayName("should return all patrons")
        void shouldReturnAll() {
            Patron p1 = createTestPatron();
            Patron p2 = Patron.create("Jane", "Smith", "jane@example.com", PatronType.FACULTY);
            when(patronRepository.findAll()).thenReturn(List.of(p1, p2));

            List<Patron> result = service.getAllPatrons();

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(p1, p2);
        }

        @Test
        @DisplayName("should return empty list when no patrons")
        void shouldReturnEmpty() {
            when(patronRepository.findAll()).thenReturn(Collections.emptyList());

            List<Patron> result = service.getAllPatrons();

            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------------------
    // suspendExpiredMemberships
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("suspendExpiredMemberships")
    class SuspendExpiredMemberships {

        @Test
        @DisplayName("should suspend all expired active patrons")
        void shouldSuspendExpired() {
            Patron expired1 = Patron.create("John", "Doe", "john@expired.com", PatronType.STUDENT);
            expired1.extendMembership(-2); // expired
            Patron expired2 = Patron.create("Jane", "Smith", "jane@expired.com", PatronType.FACULTY);
            expired2.extendMembership(-1); // expired

            when(patronRepository.findExpiredActiveMembers(LocalDate.now()))
                .thenReturn(List.of(expired1, expired2));
            when(patronRepository.save(any(Patron.class))).thenAnswer(inv -> inv.getArgument(0));

            int count = service.suspendExpiredMemberships();

            assertThat(count).isEqualTo(2);
            verify(patronRepository, times(2)).save(any(Patron.class));
            verify(eventPublisher, times(2)).publish(any(PatronSuspendedEvent.class));
        }

        @Test
        @DisplayName("should return zero when no expired patrons")
        void shouldReturnZeroWhenNone() {
            when(patronRepository.findExpiredActiveMembers(LocalDate.now()))
                .thenReturn(Collections.emptyList());

            int count = service.suspendExpiredMemberships();

            assertThat(count).isEqualTo(0);
            verify(patronRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("should continue suspending when one patron throws exception")
        void shouldContinueOnException() {
            Patron expired1 = Patron.create("John", "Doe", "john@expired.com", PatronType.STUDENT);
            expired1.extendMembership(-2);
            Patron alreadySuspended = Patron.create("Jane", "Smith", "jane@expired.com", PatronType.FACULTY);
            alreadySuspended.suspend("already"); // already suspended, suspend() will throw

            when(patronRepository.findExpiredActiveMembers(LocalDate.now()))
                .thenReturn(List.of(expired1, alreadySuspended));
            when(patronRepository.save(any(Patron.class))).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw, the exception is caught internally
            int count = service.suspendExpiredMemberships();

            // Count is based on the list size, not successful suspensions
            assertThat(count).isEqualTo(2);
            // expired1 should be saved, alreadySuspended's exception is swallowed
            verify(eventPublisher, atLeastOnce()).publish(any(PatronSuspendedEvent.class));
        }
    }
}
