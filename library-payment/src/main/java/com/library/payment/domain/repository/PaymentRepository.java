package com.library.payment.domain.repository;

import com.library.payment.domain.model.Payment;
import com.library.payment.domain.model.enums.PaymentStatus;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, PaymentId> {

    List<Payment> findByPatronId(PatronId patronId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByReferenceNumber(String referenceNumber);

    @Query("SELECT p FROM Payment p WHERE p.patronId = :patronId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsByPatron(PatronId patronId);
}
