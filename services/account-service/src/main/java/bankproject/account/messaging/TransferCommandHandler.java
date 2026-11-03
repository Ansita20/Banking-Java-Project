package bankproject.account.messaging;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.account.domain.Account;
import bankproject.account.domain.AccountRepository;
import bankproject.account.domain.CustomerRepository;
import bankproject.account.domain.EntryType;
import bankproject.account.domain.ProcessedCommand;
import bankproject.account.domain.ProcessedCommandRepository;
import bankproject.account.service.LedgerPoster;
import bankproject.common.events.AccountCommands.CreditAccount;
import bankproject.common.events.AccountCommands.DebitAccount;
import bankproject.common.events.AccountCommands.RefundAccount;
import bankproject.common.events.AccountReplies.AccountCredited;
import bankproject.common.events.AccountReplies.AccountDebited;
import bankproject.common.events.AccountReplies.AccountRefunded;
import bankproject.common.events.AccountReplies.CreditRejected;
import bankproject.common.events.AccountReplies.DebitRejected;
import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;

/**
 * Participant side of the transfer saga. Each command runs in one local transaction that
 * locks the account, posts to the ledger, records the reply for deduplication and queues
 * the reply in the outbox.
 */
@Service
@RequiredArgsConstructor
public class TransferCommandHandler {

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final ProcessedCommandRepository processed;
    private final LedgerPoster poster;
    private final OutboxWriter outbox;

    @Transactional
    public void handle(DebitAccount cmd) {
        if (replayIfProcessed(cmd.transferId(), "DebitAccount")) {
            return;
        }
        Optional<Account> found = accounts.lockByAccountNumber(cmd.accountNumber());
        String rejection = null;
        if (found.isEmpty() || !found.get().getUserId().equals(cmd.initiatorUserId())) {
            rejection = "Source account not found";
        } else if (!found.get().isActive()) {
            rejection = "Source account is " + found.get().getStatus();
        } else if (!customers.findById(cmd.initiatorUserId()).map(c -> c.isEnabled()).orElse(false)) {
            rejection = "Customer access suspended";
        } else if (found.get().getBalance().compareTo(cmd.amount()) < 0) {
            rejection = "Insufficient funds";
        }

        BankEvent reply;
        if (rejection != null) {
            reply = new DebitRejected(cmd.transferId(), cmd.accountNumber(), rejection);
        } else {
            Account account = found.get();
            poster.debit(account, EntryType.TRANSFER_DEBIT, cmd.amount(), cmd.transferId().toString(), null,
                    cmd.description());
            reply = new AccountDebited(cmd.transferId(), cmd.accountNumber(), account.getBalance());
        }
        respond(cmd.transferId(), "DebitAccount", reply);
    }

    @Transactional
    public void handle(CreditAccount cmd) {
        if (replayIfProcessed(cmd.transferId(), "CreditAccount")) {
            return;
        }
        Optional<Account> found = accounts.lockByAccountNumber(cmd.accountNumber());
        BankEvent reply;
        if (found.isEmpty()) {
            reply = new CreditRejected(cmd.transferId(), cmd.accountNumber(), "Beneficiary account not found");
        } else if (!found.get().isActive()) {
            reply = new CreditRejected(cmd.transferId(), cmd.accountNumber(),
                    "Beneficiary account cannot receive funds");
        } else {
            poster.credit(found.get(), EntryType.TRANSFER_CREDIT, cmd.amount(), cmd.transferId().toString(), null,
                    cmd.description());
            reply = new AccountCredited(cmd.transferId(), cmd.accountNumber());
        }
        respond(cmd.transferId(), "CreditAccount", reply);
    }

    /** Compensation always succeeds, whatever the account status: the money must go back. */
    @Transactional
    public void handle(RefundAccount cmd) {
        if (replayIfProcessed(cmd.transferId(), "RefundAccount")) {
            return;
        }
        Account account = accounts.lockByAccountNumber(cmd.accountNumber())
                .orElseThrow(() -> new IllegalStateException("Refund target vanished: " + cmd.accountNumber()));
        poster.credit(account, EntryType.TRANSFER_REVERSAL, cmd.amount(), cmd.transferId().toString(), null,
                "Reversal: " + cmd.reason());
        respond(cmd.transferId(), "RefundAccount", new AccountRefunded(cmd.transferId(), cmd.accountNumber()));
    }

    private boolean replayIfProcessed(UUID transferId, String commandType) {
        return processed.findById(new ProcessedCommand.Key(transferId, commandType))
                .map(done -> {
                    outbox.enqueue(Topics.ACCOUNT_REPLIES, transferId.toString(),
                            EventCodec.decode(done.getReplyPayload()));
                    return true;
                })
                .orElse(false);
    }

    private void respond(UUID transferId, String commandType, BankEvent reply) {
        processed.save(new ProcessedCommand(transferId, commandType, EventCodec.encode(reply)));
        outbox.enqueue(Topics.ACCOUNT_REPLIES, transferId.toString(), reply);
    }
}
