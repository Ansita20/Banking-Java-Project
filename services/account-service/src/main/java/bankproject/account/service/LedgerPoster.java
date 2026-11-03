package bankproject.account.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import bankproject.account.domain.Account;
import bankproject.account.domain.EntryType;
import bankproject.account.domain.LedgerEntry;
import bankproject.account.domain.LedgerEntryRepository;
import bankproject.common.events.AccountEvents.LedgerPosted;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;

/** The only place balances change: mutates the (already locked) account and records the entry. */
@Component
@RequiredArgsConstructor
public class LedgerPoster {

    private final LedgerEntryRepository ledger;
    private final OutboxWriter outbox;

    public LedgerEntry credit(Account account, EntryType type, BigDecimal amount, String reference,
                              String counterparty, String description) {
        account.credit(amount);
        return record(account, type, amount, reference, counterparty, description);
    }

    public LedgerEntry debit(Account account, EntryType type, BigDecimal amount, String reference,
                             String counterparty, String description) {
        account.debit(amount);
        return record(account, type, amount, reference, counterparty, description);
    }

    private LedgerEntry record(Account account, EntryType type, BigDecimal amount, String reference,
                               String counterparty, String description) {
        LedgerEntry entry = ledger.save(new LedgerEntry(account, type, amount, reference, counterparty, description));
        outbox.enqueue(Topics.ACCOUNT_EVENTS, account.getAccountNumber(),
                new LedgerPosted(account.getAccountNumber(), account.getUserId(), type.name(),
                        type.direction().name(), amount, account.getBalance(), reference, description));
        return entry;
    }
}
