package bankproject.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Every message on Kafka is a BankEvent serialized as JSON with a "type" discriminator,
 * so consumers can deserialize without relying on Java class names in headers.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = IdentityEvents.OtpIssued.class, name = "OtpIssued"),
        @JsonSubTypes.Type(value = IdentityEvents.CustomerUpdated.class, name = "CustomerUpdated"),
        @JsonSubTypes.Type(value = AccountCommands.DebitAccount.class, name = "DebitAccount"),
        @JsonSubTypes.Type(value = AccountCommands.CreditAccount.class, name = "CreditAccount"),
        @JsonSubTypes.Type(value = AccountCommands.RefundAccount.class, name = "RefundAccount"),
        @JsonSubTypes.Type(value = AccountReplies.AccountDebited.class, name = "AccountDebited"),
        @JsonSubTypes.Type(value = AccountReplies.DebitRejected.class, name = "DebitRejected"),
        @JsonSubTypes.Type(value = AccountReplies.AccountCredited.class, name = "AccountCredited"),
        @JsonSubTypes.Type(value = AccountReplies.CreditRejected.class, name = "CreditRejected"),
        @JsonSubTypes.Type(value = AccountReplies.AccountRefunded.class, name = "AccountRefunded"),
        @JsonSubTypes.Type(value = AccountEvents.AccountStatusChanged.class, name = "AccountStatusChanged"),
        @JsonSubTypes.Type(value = AccountEvents.LedgerPosted.class, name = "LedgerPosted"),
        @JsonSubTypes.Type(value = TransferEvents.TransferCompleted.class, name = "TransferCompleted"),
        @JsonSubTypes.Type(value = TransferEvents.TransferFailed.class, name = "TransferFailed"),
})
public interface BankEvent {
}
