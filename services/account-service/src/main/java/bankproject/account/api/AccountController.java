package bankproject.account.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bankproject.account.api.AccountDtos.AccountResponse;
import bankproject.account.api.AccountDtos.CashRequest;
import bankproject.account.api.AccountDtos.LedgerEntryResponse;
import bankproject.account.api.AccountDtos.OpenAccountRequest;
import bankproject.account.domain.EntryType;
import bankproject.account.service.AccountService;
import bankproject.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        return accountService.open(CurrentUser.get(), request.type());
    }

    @GetMapping
    public List<AccountResponse> mine() {
        return accountService.listMine(CurrentUser.get());
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        return accountService.get(CurrentUser.get(), accountNumber);
    }

    @GetMapping("/{accountNumber}/statement")
    public Page<LedgerEntryResponse> statement(@PathVariable String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return accountService.statement(CurrentUser.get(), accountNumber, from, to, page, size);
    }

    @PostMapping("/{accountNumber}/deposits")
    public LedgerEntryResponse deposit(@PathVariable String accountNumber,
                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                       @Valid @RequestBody CashRequest request) {
        return accountService.cash(CurrentUser.get(), accountNumber, EntryType.DEPOSIT, request.amount(),
                request.description(), idempotencyKey);
    }

    @PostMapping("/{accountNumber}/withdrawals")
    public LedgerEntryResponse withdraw(@PathVariable String accountNumber,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @Valid @RequestBody CashRequest request) {
        return accountService.cash(CurrentUser.get(), accountNumber, EntryType.WITHDRAWAL, request.amount(),
                request.description(), idempotencyKey);
    }

    @PostMapping("/{accountNumber}/close")
    public AccountResponse close(@PathVariable String accountNumber) {
        return accountService.close(CurrentUser.get(), accountNumber);
    }
}
