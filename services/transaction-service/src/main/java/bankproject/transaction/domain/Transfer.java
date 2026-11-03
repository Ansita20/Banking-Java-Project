package bankproject.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transfer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "initiator_user_id", nullable = false)
    private String initiatorUserId;

    @Column(name = "from_account", nullable = false)
    private String fromAccount;

    @Column(name = "to_account", nullable = false)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false)
    private String requestFingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public Transfer(String initiatorUserId, String fromAccount, String toAccount, BigDecimal amount,
                    String description, String idempotencyKey, String requestFingerprint) {
        this.id = UUID.randomUUID();
        this.initiatorUserId = initiatorUserId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = TransferStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    /** Moves to {@code next} only from {@code expected}; returns false for duplicate/out-of-order replies. */
    public boolean advance(TransferStatus expected, TransferStatus next, String reason) {
        if (status != expected) {
            return false;
        }
        status = next;
        if (reason != null) {
            failureReason = reason;
        }
        updatedAt = Instant.now();
        return true;
    }

    public void touch() {
        updatedAt = Instant.now();
    }
}
