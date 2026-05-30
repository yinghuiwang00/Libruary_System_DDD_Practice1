package com.library.payment.domain.model;

import com.library.payment.domain.exception.InvalidOperationException;
import com.library.payment.domain.model.enums.PaymentMethod;
import com.library.payment.domain.model.enums.PaymentStatus;
import com.library.payment.domain.model.enums.PaymentType;
import com.library.shared.domain.model.PatronId;
import com.library.shared.domain.model.PaymentId;
import com.library.shared.domain.model.RefundId;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_patron_id", columnList = "patron_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_date", columnList = "payment_date"),
    @Index(name = "idx_payment_reference", columnList = "reference_number")
})
public class Payment {

    private static final Random RANDOM = new Random();

    @EmbeddedId
    private PaymentId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "patron_id", nullable = false, length = 36))
    private PatronId patronId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "reference_number", unique = true, nullable = false, length = 30)
    private String referenceNumber;

    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(length = 500)
    private String description;

    @Column(length = 3)
    private String currency;

    @Column(name = "fee_amount", precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private final List<Refund> refunds = new ArrayList<>();

    protected Payment() {
    }

    private Payment(PaymentId id, PatronId patronId, PaymentType paymentType,
                    BigDecimal amount, PaymentMethod paymentMethod, String description) {
        this.id = Objects.requireNonNull(id, "Payment ID must not be null");
        this.patronId = Objects.requireNonNull(patronId, "Patron ID must not be null");
        this.paymentType = Objects.requireNonNull(paymentType, "Payment type must not be null");
        this.amount = validateAmount(amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method must not be null");
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.referenceNumber = generateReferenceNumber();
        this.currency = "CNY";
        this.feeAmount = BigDecimal.ZERO;
        this.netAmount = amount;
    }

    public static Payment create(PatronId patronId, PaymentType paymentType,
                                  BigDecimal amount, PaymentMethod paymentMethod,
                                  String description) {
        return new Payment(PaymentId.generate(), patronId, paymentType, amount, paymentMethod, description);
    }

    public void process(String externalTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidOperationException(
                "Cannot process payment in status: " + this.status);
        }
        this.status = PaymentStatus.PROCESSING;
        this.externalTransactionId = externalTransactionId;
    }

    public void complete() {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new InvalidOperationException(
                "Cannot complete payment in status: " + this.status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
        this.processedDate = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new InvalidOperationException(
                "Cannot fail payment in status: " + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void cancel(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidOperationException(
                "Cannot cancel payment in status: " + this.status);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
    }

    public Refund requestRefund(RefundId refundId, BigDecimal amount, String reason) {
        if (!canBeRefunded()) {
            throw new InvalidOperationException(
                "Payment cannot be refunded in status: " + this.status);
        }
        BigDecimal refundableAmount = getRefundableAmount();
        if (amount.compareTo(refundableAmount) > 0) {
            throw new InvalidOperationException(
                "Refund amount " + amount + " exceeds refundable amount " + refundableAmount);
        }

        Refund refund = Refund.create(refundId, this.id.getValue(), amount, reason);
        this.refunds.add(refund);

        if (amount.compareTo(this.amount) == 0) {
            this.status = PaymentStatus.REFUNDED;
        }

        return refund;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return this.status == PaymentStatus.REFUNDED;
    }

    public boolean canBeRefunded() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public BigDecimal getRefundableAmount() {
        BigDecimal totalRefunded = BigDecimal.ZERO;
        for (Refund refund : refunds) {
            if (refund.isCompleted()) {
                totalRefunded = totalRefunded.add(refund.getAmount());
            }
        }
        return this.amount.subtract(totalRefunded);
    }

    private String generateReferenceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = RANDOM.nextInt(9000) + 1000;
        return "PAY" + timestamp + randomSuffix;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    // Getters
    public PaymentId getId() { return id; }
    public PatronId getPatronId() { return patronId; }
    public PaymentType getPaymentType() { return paymentType; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public String getDescription() { return description; }
    public String getCurrency() { return currency; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public String getFailureReason() { return failureReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<Refund> getRefunds() { return refunds; }
}
