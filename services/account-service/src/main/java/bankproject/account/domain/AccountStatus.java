package bankproject.account.domain;

public enum AccountStatus {
    /** Opened by the customer, waiting for a bank officer. */
    PENDING_APPROVAL,
    ACTIVE,
    /** Blocked by the bank: no debits or credits except refunds. */
    FROZEN,
    REJECTED,
    CLOSED
}
