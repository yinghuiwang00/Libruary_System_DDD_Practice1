package com.library.patron.domain.repository;

import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.MembershipStatus;
import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.model.PatronId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatronRepository extends JpaRepository<Patron, PatronId> {

    Optional<Patron> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Patron> findByStatus(MembershipStatus status);

    List<Patron> findByPatronType(PatronType patronType);

    List<Patron> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);

    @Query("SELECT p FROM Patron p WHERE p.status = 'ACTIVE' AND p.membershipExpiry < :date")
    List<Patron> findExpiredActiveMembers(LocalDate date);

    @Query("SELECT p FROM Patron p WHERE p.status = 'ACTIVE' AND p.outstandingFines > 0")
    List<Patron> findActivePatronsWithOutstandingFines();

    long countByStatus(MembershipStatus status);

    long countByPatronType(PatronType patronType);
}
