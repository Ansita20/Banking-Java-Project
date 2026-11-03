package bankproject.common.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Replies from account-service to the transfer saga orchestrator. */
public final class AccountReplies {

    public record AccountDebited(UUID transferId, String accountNumber, BigDecimal balanceAfter)
            implements BankEvent {
    }

    public record DebitRejected(UUID transferId, String accountNumber, String reason) implements BankEvent {
    }

    public record AccountCredited(UUID transferId, String accountNumber) implements BankEvent {
    }

    public record CreditRejected(UUID transferId, String accountNumber, String reason) implements BankEvent {
    }

    public record AccountRefunded(UUID transferId, String accountNumber) implements BankEvent {
    }

    private AccountReplies() {
    }
}
