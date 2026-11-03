package bankproject.common.events;

/** Kafka topic names shared by every service. */
public final class Topics {

    /** Customer lifecycle snapshots and OTPs, published by identity-service. */
    public static final String IDENTITY_EVENTS = "identity.events";

    /** Saga commands sent by transaction-service to account-service. Keyed by account number. */
    public static final String ACCOUNT_COMMANDS = "account.commands";

    /** Saga replies sent by account-service back to transaction-service. Keyed by transfer id. */
    public static final String ACCOUNT_REPLIES = "account.replies";

    /** Facts about accounts (status changes, ledger postings), for notifications and analytics. */
    public static final String ACCOUNT_EVENTS = "account.events";

    /** Final outcome of transfers. */
    public static final String TRANSFER_EVENTS = "transfer.events";

    private Topics() {
    }
}
