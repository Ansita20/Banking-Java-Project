package bankproject.transaction.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "beneficiary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Beneficiary {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "holder_name")
    private String holderName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Beneficiary(String userId, String nickname, String accountNumber, String holderName) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.nickname = nickname;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.createdAt = Instant.now();
    }
}
