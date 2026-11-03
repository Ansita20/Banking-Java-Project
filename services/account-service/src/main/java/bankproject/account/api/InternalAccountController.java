package bankproject.account.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import bankproject.account.api.AccountDtos.AccountLookupResponse;
import bankproject.account.service.AccountService;
import lombok.RequiredArgsConstructor;

/** Service-to-service API. Not routed by the gateway; reachable only on the internal network. */
@RestController
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @GetMapping("/internal/accounts/{accountNumber}/lookup")
    public AccountLookupResponse lookup(@PathVariable String accountNumber) {
        return accountService.lookup(accountNumber);
    }
}
