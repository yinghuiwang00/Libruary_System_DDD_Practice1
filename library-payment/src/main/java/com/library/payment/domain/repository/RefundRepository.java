package com.library.payment.domain.repository;

import com.library.payment.domain.model.Refund;
import com.library.payment.domain.model.enums.RefundStatus;
import com.library.shared.domain.model.RefundId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, RefundId> {

    List<Refund> findByPaymentId(String paymentId);

    List<Refund> findByStatus(RefundStatus status);
}
