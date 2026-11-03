package bankproject.common.events;

import java.math.BigDecimal;

public final class AccountEvents {

    public record AccountStatusChanged(String accountNumber, String userId, String accountType, String status,
                                       String reason) implements BankEvent {
    }

    /** A posting hit an account's ledger; drives "your account was credited/debited" alerts. */
    public record LedgerPosted(String accountNumber, String userId, String entryType, String direction,
                               BigDecimal amount, BigDecimal balanceAfter, String reference, String description)
            implements BankEvent {
    }

    private AccountEvents() {
    }
}
