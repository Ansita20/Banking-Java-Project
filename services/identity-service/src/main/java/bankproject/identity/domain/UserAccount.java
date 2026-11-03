package bankproject.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCKOUT = Duration.ofMinutes(15);

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Embedded
    private KycProfile profile = new KycProfile();

    @Column(name = "kyc_completed_at")
    private Instant kycCompletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    public static UserAccount register(String email, String passwordHash, String firstName, String lastName,
                                       Role role) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.passwordHash = passwordHash;
        user.firstName = firstName;
        user.lastName = lastName;
        user.role = role;
        user.createdAt = Instant.now();
        return user;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean isKycCompleted() {
        return kycCompletedAt != null;
    }

    public boolean isLockedOut() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void recordFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_LOGINS) {
            lockedUntil = Instant.now().plus(LOCKOUT);
            failedLoginAttempts = 0;
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
