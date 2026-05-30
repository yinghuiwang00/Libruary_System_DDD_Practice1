package com.library.patron.domain.service;

import com.library.patron.domain.event.*;
import com.library.patron.domain.exception.DuplicateEmailException;
import com.library.patron.domain.exception.InvalidOperationException;
import com.library.patron.domain.exception.PatronNotFoundException;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.MembershipStatus;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.event.DomainEventPublisher;
import com.library.shared.domain.model.PatronId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PatronManagementService {

    private final PatronRepository patronRepository;
    private final DomainEventPublisher eventPublisher;

    public PatronManagementService(PatronRepository patronRepository, DomainEventPublisher eventPublisher) {
        this.patronRepository = patronRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Patron registerPatron(String firstName, String lastName, String email,
                                  String phone, String address, String city, String postalCode,
                                  PatronType patronType) {
        if (patronRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        Patron patron = Patron.create(firstName, lastName, email, patronType);
        patron.updatePersonalInfo(firstName, lastName, email, phone, address, city, postalCode);
        Patron saved = patronRepository.save(patron);

        eventPublisher.publish(new PatronRegisteredEvent(
            saved.getId(), saved.getFullName(), saved.getEmail(), saved.getPatronType(), saved.getMemberSince()
        ));
        return saved;
    }

    @Transactional
    public Patron updatePatronInfo(PatronId patronId, String firstName, String lastName, String email,
                                    String phone, String address, String city, String postalCode) {
        Patron patron = findOrThrow(patronId);

        if (email != null && !email.equals(patron.getEmail()) && patronRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        patron.updatePersonalInfo(firstName, lastName, email, phone, address, city, postalCode);
        Patron saved = patronRepository.save(patron);

        eventPublisher.publish(new PatronUpdatedEvent(saved.getId()));
        return saved;
    }

    @Transactional
    public void suspendPatron(PatronId patronId, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.suspend(reason);
        patronRepository.save(patron);
        eventPublisher.publish(new PatronSuspendedEvent(patronId, reason));
    }

    @Transactional
    public void reactivatePatron(PatronId patronId, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.reactivate(reason);
        patronRepository.save(patron);
        eventPublisher.publish(new PatronReactivatedEvent(patronId, reason));
    }

    @Transactional
    public void terminatePatron(PatronId patronId, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.terminate(reason);
        patronRepository.save(patron);
        eventPublisher.publish(new PatronTerminatedEvent(patronId, reason));
    }

    @Transactional
    public void extendMembership(PatronId patronId, int months, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.extendMembership(months);
        patronRepository.save(patron);
    }

    @Transactional
    public void addFine(PatronId patronId, BigDecimal amount, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.addFine(amount);
        patronRepository.save(patron);
    }

    @Transactional
    public void payFine(PatronId patronId, BigDecimal amount) {
        Patron patron = findOrThrow(patronId);
        patron.payFine(amount);
        patronRepository.save(patron);
    }

    @Transactional
    public void waiveFine(PatronId patronId, BigDecimal amount, String reason) {
        Patron patron = findOrThrow(patronId);
        patron.waiveFine(amount);
        patronRepository.save(patron);
    }

    @Transactional
    public void changePatronType(PatronId patronId, PatronType newType) {
        Patron patron = findOrThrow(patronId);
        PatronType oldType = patron.getPatronType();
        patron.updatePatronType(newType);
        patronRepository.save(patron);
        eventPublisher.publish(new PatronTypeChangedEvent(patronId, oldType, newType));
    }

    @Transactional
    public int suspendExpiredMemberships() {
        List<Patron> expired = patronRepository.findExpiredActiveMembers(LocalDate.now());
        for (Patron patron : expired) {
            try {
                patron.suspend("Membership expired");
                patronRepository.save(patron);
                eventPublisher.publish(new PatronSuspendedEvent(patron.getId(), "Membership expired"));
            } catch (Exception ignored) {
            }
        }
        return expired.size();
    }

    @Transactional(readOnly = true)
    public Patron getPatron(PatronId patronId) {
        return findOrThrow(patronId);
    }

    @Transactional(readOnly = true)
    public List<Patron> getAllPatrons() {
        return patronRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Patron> getPatronsByStatus(MembershipStatus status) {
        return patronRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Patron> getPatronsByType(PatronType type) {
        return patronRepository.findByPatronType(type);
    }

    private Patron findOrThrow(PatronId patronId) {
        return patronRepository.findById(patronId)
            .orElseThrow(() -> new PatronNotFoundException(patronId));
    }
}
