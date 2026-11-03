package bankproject.account.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local, eventually consistent replica of identity-service's customer, built from
 * CustomerUpdated events. Lets this service enforce KYC/blocking without a synchronous call.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "kyc_completed", nullable = false)
    private boolean kycCompleted;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Customer(String userId) {
        this.userId = userId;
    }
}
