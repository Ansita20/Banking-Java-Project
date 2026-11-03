package bankproject.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import bankproject.common.events.IdentityEvents.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An OTP. Only its hash is stored; it expires and allows a limited number of guesses. */
@Entity
@Table(name = "one_time_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OneTimeCode {

    public static final Duration VALIDITY = Duration.ofMinutes(10);
    public static final int MAX_ATTEMPTS = 5;

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public OneTimeCode(String userId, OtpPurpose purpose, String codeHash) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plus(VALIDITY);
    }

    public boolean isUsable() {
        return consumedAt == null && attempts < MAX_ATTEMPTS && expiresAt.isAfter(Instant.now());
    }

    public void recordAttempt() {
        attempts++;
    }

    public void consume() {
        consumedAt = Instant.now();
    }
}
