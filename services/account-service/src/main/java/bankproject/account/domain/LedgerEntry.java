package bankproject.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only record of every balance movement. (reference, entry_type) is unique, which makes
 * replays of the same deposit or saga step impossible at the database level.
 */
@Entity
@Table(name = "ledger_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType.Direction direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private String reference;

    @Column
    private String counterparty;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public LedgerEntry(Account account, EntryType type, BigDecimal amount, String reference, String counterparty,
                       String description) {
        this.id = UUID.randomUUID();
        this.accountNumber = account.getAccountNumber();
        this.entryType = type;
        this.direction = type.direction();
        this.amount = amount;
        this.balanceAfter = account.getBalance();
        this.reference = reference;
        this.counterparty = counterparty;
        this.description = description;
        this.createdAt = Instant.now();
    }
}
