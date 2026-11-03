package bankproject.account.api;

import java.math.BigDecimal;
import java.time.Instant;

import bankproject.account.domain.Account;
import bankproject.account.domain.AccountType;
import bankproject.account.domain.LedgerEntry;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AccountDtos {

    public record OpenAccountRequest(@NotNull AccountType type) {
    }

    public record CashRequest(
            @NotNull @DecimalMin(value = "1.00") @DecimalMax(value = "200000.00", message = "must not exceed 2,00,000 per transaction")
            @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record AccountResponse(String accountNumber, String userId, String type, String status, BigDecimal balance,
                                  String currency, String statusReason, Instant openedAt, Instant activatedAt) {

        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getAccountNumber(), a.getUserId(), a.getType().name(), a.getStatus().name(),
                    a.getBalance(), a.getCurrency(), a.getStatusReason(), a.getOpenedAt(), a.getActivatedAt());
        }
    }

    public record AdminAccountResponse(AccountResponse account, String holderName, String holderEmail) {
    }

    public record LedgerEntryResponse(String id, String entryType, String direction, BigDecimal amount,
                                      BigDecimal balanceAfter, String reference, String counterparty,
                                      String description, Instant createdAt) {

        public static LedgerEntryResponse from(LedgerEntry e) {
            return new LedgerEntryResponse(e.getId().toString(), e.getEntryType().name(), e.getDirection().name(),
                    e.getAmount(), e.getBalanceAfter(), e.getReference(), e.getCounterparty(), e.getDescription(),
                    e.getCreatedAt());
        }
    }

    /** What another customer may learn about an account before paying into it. */
    public record AccountLookupResponse(String accountNumber, String holderName, String type, boolean canReceive) {
    }

    public record BankSummary(long activeAccounts, long pendingApprovals, long frozenAccounts,
                              BigDecimal totalDeposits) {
    }

    private AccountDtos() {
    }
}
