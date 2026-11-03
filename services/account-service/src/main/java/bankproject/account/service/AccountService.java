package bankproject.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.account.api.AccountDtos.AccountLookupResponse;
import bankproject.account.api.AccountDtos.AccountResponse;
import bankproject.account.api.AccountDtos.AdminAccountResponse;
import bankproject.account.api.AccountDtos.BankSummary;
import bankproject.account.api.AccountDtos.LedgerEntryResponse;
import bankproject.account.domain.Account;
import bankproject.account.domain.AccountNumberGenerator;
import bankproject.account.domain.AccountRepository;
import bankproject.account.domain.AccountStatus;
import bankproject.account.domain.AccountType;
import bankproject.account.domain.Customer;
import bankproject.account.domain.CustomerRepository;
import bankproject.account.domain.EntryType;
import bankproject.account.domain.LedgerEntry;
import bankproject.account.domain.LedgerEntryRepository;
import bankproject.common.events.AccountEvents.AccountStatusChanged;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.web.ApiException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    static final ZoneId BANK_ZONE = ZoneId.of("Asia/Kolkata");
    private static final EnumSet<AccountStatus> OPEN_STATUSES =
            EnumSet.of(AccountStatus.PENDING_APPROVAL, AccountStatus.ACTIVE, AccountStatus.FROZEN);

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final LedgerEntryRepository ledger;
    private final AccountNumberGenerator numberGenerator;
    private final LedgerPoster poster;
    private final OutboxWriter outbox;

    @Transactional
    public AccountResponse open(AuthenticatedUser user, AccountType type) {
        Customer customer = customers.findById(user.userId()).orElseThrow(() -> ApiException.unprocessable(
                "CUSTOMER_NOT_SYNCED", "Your profile is still being set up, please retry in a few seconds"));
        if (!customer.isEnabled()) {
            throw ApiException.forbidden("Your access has been suspended by the bank");
        }
        if (!customer.isKycCompleted()) {
            throw ApiException.unprocessable("KYC_REQUIRED", "Complete your KYC profile before opening an account");
        }
        if (accounts.existsByUserIdAndTypeAndStatusIn(user.userId(), type, OPEN_STATUSES)) {
            throw ApiException.conflict("You already have a " + type.name().toLowerCase() + " account");
        }
        Account account = accounts.save(Account.open(numberGenerator.next(), user.userId(), type));
        publishStatus(account);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listMine(AuthenticatedUser user) {
        return accounts.findByUserIdOrderByOpenedAt(user.userId()).stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(AuthenticatedUser user, String accountNumber) {
        return AccountResponse.from(findOwned(user, accountNumber));
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> statement(AuthenticatedUser user, String accountNumber, LocalDate from,
                                               LocalDate to, int page, int size) {
        findOwned(user, accountNumber);
        Instant start = (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay(BANK_ZONE).toInstant();
        Instant end = (to == null ? LocalDate.now(BANK_ZONE) : to).plusDays(1).atStartOfDay(BANK_ZONE).toInstant();
        return ledger.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtDesc(accountNumber, start, end,
                        PageRequest.of(page, Math.min(size, 200)))
                .map(LedgerEntryResponse::from);
    }

    /** Deposit/withdrawal. The idempotency key makes a retried request return the original result. */
    @Transactional
    public LedgerEntryResponse cash(AuthenticatedUser user, String accountNumber, EntryType type, BigDecimal amount,
                                    String description, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
        var previous = ledger.findByReferenceAndEntryType(idempotencyKey, type);
        if (previous.isPresent()) {
            if (!previous.get().getAccountNumber().equals(accountNumber)) {
                throw ApiException.conflict("Idempotency-Key already used for a different request");
            }
            return LedgerEntryResponse.from(previous.get());
        }
        Account account = accounts.lockByAccountNumber(accountNumber)
                .filter(a -> a.getUserId().equals(user.userId()))
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!account.isActive()) {
            throw ApiException.unprocessable("ACCOUNT_NOT_ACTIVE", "Account is " + account.getStatus());
        }
        String text = description == null || description.isBlank()
                ? (type == EntryType.DEPOSIT ? "Cash deposit" : "Cash withdrawal") : description.trim();
        LedgerEntry entry = type == EntryType.DEPOSIT
                ? poster.credit(account, type, amount, idempotencyKey, null, text)
                : poster.debit(account, type, amount, idempotencyKey, null, text);
        return LedgerEntryResponse.from(entry);
    }

    @Transactional
    public AccountResponse close(AuthenticatedUser user, String accountNumber) {
        Account account = accounts.lockByAccountNumber(accountNumber)
                .filter(a -> a.getUserId().equals(user.userId()))
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        account.close();
        publishStatus(account);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountLookupResponse lookup(String accountNumber) {
        Account account = accounts.findById(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        String holder = customers.findById(account.getUserId()).map(Customer::getFullName).orElse("Account holder");
        return new AccountLookupResponse(account.getAccountNumber(), maskName(holder), account.getType().name(),
                account.isActive());
    }

    // ---- back office ----

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> adminList(AccountStatus status) {
        List<Account> found = status == null ? accounts.findAllByOrderByOpenedAtDesc()
                : accounts.findByStatusOrderByOpenedAt(status);
        return found.stream().map(a -> {
            var holder = customers.findById(a.getUserId());
            return new AdminAccountResponse(AccountResponse.from(a),
                    holder.map(Customer::getFullName).orElse(null), holder.map(Customer::getEmail).orElse(null));
        }).toList();
    }

    @Transactional
    public AccountResponse approve(String accountNumber) {
        return changeStatus(accountNumber, Account::approve);
    }

    @Transactional
    public AccountResponse reject(String accountNumber, String reason) {
        return changeStatus(accountNumber, a -> a.reject(reason));
    }

    @Transactional
    public AccountResponse freeze(String accountNumber, String reason) {
        return changeStatus(accountNumber, a -> a.freeze(reason));
    }

    @Transactional
    public AccountResponse unfreeze(String accountNumber) {
        return changeStatus(accountNumber, Account::unfreeze);
    }

    @Transactional(readOnly = true)
    public BankSummary summary() {
        return new BankSummary(accounts.countByStatus(AccountStatus.ACTIVE),
                accounts.countByStatus(AccountStatus.PENDING_APPROVAL), accounts.countByStatus(AccountStatus.FROZEN),
                accounts.totalDeposits());
    }

    private AccountResponse changeStatus(String accountNumber, java.util.function.Consumer<Account> change) {
        Account account = accounts.lockByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        change.accept(account);
        publishStatus(account);
        return AccountResponse.from(account);
    }

    private Account findOwned(AuthenticatedUser user, String accountNumber) {
        return accounts.findById(accountNumber)
                .filter(a -> user.isAdmin() || a.getUserId().equals(user.userId()))
                .orElseThrow(() -> ApiException.notFound("Account not found"));
    }

    private void publishStatus(Account account) {
        outbox.enqueue(Topics.ACCOUNT_EVENTS, account.getAccountNumber(),
                new AccountStatusChanged(account.getAccountNumber(), account.getUserId(), account.getType().name(),
                        account.getStatus().name(), account.getStatusReason()));
    }

    /** "Ravi Kumar" -> "Ra** Ku***": enough to confirm a payee without exposing the full name. */
    static String maskName(String name) {
        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            int keep = Math.min(2, part.length());
            out.append(part, 0, keep).append("*".repeat(part.length() - keep));
        }
        return out.toString();
    }
}
