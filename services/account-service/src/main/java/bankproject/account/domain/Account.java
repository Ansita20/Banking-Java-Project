package bankproject.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import bankproject.common.web.ApiException;
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
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    public static final String CURRENCY = "INR";

    @Id
    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Version
    private long version;

    public static Account open(String accountNumber, String userId, AccountType type) {
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.userId = userId;
        account.type = type;
        account.status = AccountStatus.PENDING_APPROVAL;
        account.balance = BigDecimal.ZERO.setScale(2);
        account.currency = CURRENCY;
        account.openedAt = Instant.now();
        return account;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void approve() {
        requireStatus(AccountStatus.PENDING_APPROVAL);
        status = AccountStatus.ACTIVE;
        statusReason = null;
        activatedAt = Instant.now();
    }

    public void reject(String reason) {
        requireStatus(AccountStatus.PENDING_APPROVAL);
        status = AccountStatus.REJECTED;
        statusReason = reason;
    }

    public void freeze(String reason) {
        requireStatus(AccountStatus.ACTIVE);
        status = AccountStatus.FROZEN;
        statusReason = reason;
    }

    public void unfreeze() {
        requireStatus(AccountStatus.FROZEN);
        status = AccountStatus.ACTIVE;
        statusReason = null;
    }

    public void close() {
        if (status != AccountStatus.ACTIVE && status != AccountStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Only active or pending accounts can be closed");
        }
        if (balance.signum() != 0) {
            throw ApiException.conflict("Withdraw or transfer the remaining balance before closing");
        }
        status = AccountStatus.CLOSED;
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw ApiException.unprocessable("INSUFFICIENT_FUNDS", "Insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    private void requireStatus(AccountStatus expected) {
        if (status != expected) {
            throw ApiException.conflict("Account is " + status + ", expected " + expected);
        }
    }
}
