package bankproject.common.events;

import java.math.BigDecimal;
import java.util.UUID;

public final class TransferEvents {

    public record TransferCompleted(UUID transferId, String initiatorUserId, String fromAccount, String toAccount,
                                    BigDecimal amount) implements BankEvent {
    }

    public record TransferFailed(UUID transferId, String initiatorUserId, String fromAccount, String toAccount,
                                 BigDecimal amount, String reason) implements BankEvent {
    }

    private TransferEvents() {
    }
}
