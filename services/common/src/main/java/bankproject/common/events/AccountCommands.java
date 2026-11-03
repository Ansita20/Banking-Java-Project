package bankproject.common.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Commands in the transfer saga, handled by account-service. */
public final class AccountCommands {

    public record DebitAccount(UUID transferId, String accountNumber, BigDecimal amount, String initiatorUserId,
                               String description) implements BankEvent {
    }

    public record CreditAccount(UUID transferId, String accountNumber, BigDecimal amount, String description)
            implements BankEvent {
    }

    /** Compensation: give back money taken by a DebitAccount whose credit leg failed. */
    public record RefundAccount(UUID transferId, String accountNumber, BigDecimal amount, String reason)
            implements BankEvent {
    }

    private AccountCommands() {
    }
}
