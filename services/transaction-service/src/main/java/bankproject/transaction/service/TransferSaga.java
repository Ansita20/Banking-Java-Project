package bankproject.transaction.service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.events.AccountCommands.CreditAccount;
import bankproject.common.events.AccountCommands.DebitAccount;
import bankproject.common.events.AccountCommands.RefundAccount;
import bankproject.common.events.AccountReplies.AccountCredited;
import bankproject.common.events.AccountReplies.AccountDebited;
import bankproject.common.events.AccountReplies.AccountRefunded;
import bankproject.common.events.AccountReplies.CreditRejected;
import bankproject.common.events.AccountReplies.DebitRejected;
import bankproject.common.events.Topics;
import bankproject.common.events.TransferEvents.TransferCompleted;
import bankproject.common.events.TransferEvents.TransferFailed;
import bankproject.common.outbox.OutboxWriter;
import bankproject.transaction.domain.Transfer;
import bankproject.transaction.domain.TransferRepository;
import bankproject.transaction.domain.TransferStatus;
import lombok.RequiredArgsConstructor;

/**
 * Orchestrator of the transfer saga. Each reply advances the state machine in one local
 * transaction and queues the next command. Duplicate or late replies are ignored because
 * {@link Transfer#advance} only moves from the expected state.
 */
@Service
@RequiredArgsConstructor
public class TransferSaga {

    private static final Logger log = LoggerFactory.getLogger(TransferSaga.class);
    private static final Duration STUCK_AFTER = Duration.ofSeconds(60);

    private final TransferRepository transfers;
    private final TransferLimits limits;
    private final OutboxWriter outbox;

    @Transactional
    public void on(AccountDebited reply) {
        Transfer t = find(reply.transferId());
        if (t != null && t.advance(TransferStatus.PENDING, TransferStatus.DEBITED, null)) {
            sendCredit(t);
        }
    }

    @Transactional
    public void on(DebitRejected reply) {
        Transfer t = find(reply.transferId());
        if (t != null && t.advance(TransferStatus.PENDING, TransferStatus.FAILED, reply.reason())) {
            fail(t);
        }
    }

    @Transactional
    public void on(AccountCredited reply) {
        Transfer t = find(reply.transferId());
        if (t != null && t.advance(TransferStatus.DEBITED, TransferStatus.COMPLETED, null)) {
            outbox.enqueue(Topics.TRANSFER_EVENTS, t.getId().toString(), new TransferCompleted(t.getId(),
                    t.getInitiatorUserId(), t.getFromAccount(), t.getToAccount(), t.getAmount()));
        }
    }

    @Transactional
    public void on(CreditRejected reply) {
        Transfer t = find(reply.transferId());
        if (t != null && t.advance(TransferStatus.DEBITED, TransferStatus.COMPENSATING, reply.reason())) {
            sendRefund(t);
        }
    }

    @Transactional
    public void on(AccountRefunded reply) {
        Transfer t = find(reply.transferId());
        if (t != null && t.advance(TransferStatus.COMPENSATING, TransferStatus.REVERSED, null)) {
            fail(t);
        }
    }

    /**
     * Safety net for lost messages or crashed consumers: re-sends the command for any saga that
     * has not progressed recently. Participants deduplicate, so this is always safe.
     */
    @Scheduled(fixedDelayString = "${bank.saga.recovery-interval-ms:30000}")
    @Transactional
    public void resumeStuckTransfers() {
        var stuck = transfers.findTop100ByStatusInAndUpdatedAtBefore(
                EnumSet.of(TransferStatus.PENDING, TransferStatus.DEBITED, TransferStatus.COMPENSATING),
                Instant.now().minus(STUCK_AFTER));
        for (Transfer t : stuck) {
            log.warn("Resuming stuck transfer {} in state {}", t.getId(), t.getStatus());
            switch (t.getStatus()) {
                case PENDING -> outbox.enqueue(Topics.ACCOUNT_COMMANDS, t.getFromAccount(), new DebitAccount(t.getId(),
                        t.getFromAccount(), t.getAmount(), t.getInitiatorUserId(),
                        "To " + t.getToAccount() + ": " + t.getDescription()));
                case DEBITED -> sendCredit(t);
                case COMPENSATING -> sendRefund(t);
                default -> {
                }
            }
            t.touch();
        }
    }

    private void sendCredit(Transfer t) {
        outbox.enqueue(Topics.ACCOUNT_COMMANDS, t.getToAccount(), new CreditAccount(t.getId(), t.getToAccount(),
                t.getAmount(), "From " + t.getFromAccount() + ": " + t.getDescription()));
    }

    private void sendRefund(Transfer t) {
        outbox.enqueue(Topics.ACCOUNT_COMMANDS, t.getFromAccount(), new RefundAccount(t.getId(), t.getFromAccount(),
                t.getAmount(), t.getFailureReason()));
    }

    private void fail(Transfer t) {
        limits.release(t.getInitiatorUserId(), t.getCreatedAt().atZone(TransferLimits.BANK_ZONE).toLocalDate(),
                t.getAmount());
        outbox.enqueue(Topics.TRANSFER_EVENTS, t.getId().toString(), new TransferFailed(t.getId(),
                t.getInitiatorUserId(), t.getFromAccount(), t.getToAccount(), t.getAmount(), t.getFailureReason()));
    }

    private Transfer find(UUID id) {
        Transfer t = transfers.findById(id).orElse(null);
        if (t == null) {
            log.warn("Reply for unknown transfer {}", id);
        }
        return t;
    }
}
