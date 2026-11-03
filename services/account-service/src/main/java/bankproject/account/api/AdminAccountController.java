package bankproject.account.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bankproject.account.api.AccountDtos.AccountResponse;
import bankproject.account.api.AccountDtos.AdminAccountResponse;
import bankproject.account.api.AccountDtos.BankSummary;
import bankproject.account.api.AccountDtos.ReasonRequest;
import bankproject.account.domain.AccountStatus;
import bankproject.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AdminAccountResponse> list(@RequestParam(required = false) AccountStatus status) {
        return accountService.adminList(status);
    }

    @GetMapping("/summary")
    public BankSummary summary() {
        return accountService.summary();
    }

    @PostMapping("/{accountNumber}/approve")
    public AccountResponse approve(@PathVariable String accountNumber) {
        return accountService.approve(accountNumber);
    }

    @PostMapping("/{accountNumber}/reject")
    public AccountResponse reject(@PathVariable String accountNumber, @Valid @RequestBody ReasonRequest request) {
        return accountService.reject(accountNumber, request.reason());
    }

    @PostMapping("/{accountNumber}/freeze")
    public AccountResponse freeze(@PathVariable String accountNumber, @Valid @RequestBody ReasonRequest request) {
        return accountService.freeze(accountNumber, request.reason());
    }

    @PostMapping("/{accountNumber}/unfreeze")
    public AccountResponse unfreeze(@PathVariable String accountNumber) {
        return accountService.unfreeze(accountNumber);
    }
}
