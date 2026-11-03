package bankproject.transaction.domain;

/**
 * Saga states.
 * <pre>
 * PENDING --debited--> DEBITED --credited--> COMPLETED
 *    |                    |
 *    | debit rejected     | credit rejected
 *    v                    v
 *  FAILED            COMPENSATING --refunded--> REVERSED
 * </pre>
 */
public enum TransferStatus {
    PENDING,
    DEBITED,
    COMPLETED,
    COMPENSATING,
    FAILED,
    REVERSED;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REVERSED;
    }
}
