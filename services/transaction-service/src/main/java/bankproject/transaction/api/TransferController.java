package bankproject.transaction.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bankproject.common.security.CurrentUser;
import bankproject.transaction.api.TransferDtos.BeneficiaryRequest;
import bankproject.transaction.api.TransferDtos.BeneficiaryResponse;
import bankproject.transaction.api.TransferDtos.LimitsResponse;
import bankproject.transaction.api.TransferDtos.TransferRequest;
import bankproject.transaction.api.TransferDtos.TransferResponse;
import bankproject.transaction.client.AccountDirectoryClient.AccountLookup;
import bankproject.transaction.domain.TransferStatus;
import bankproject.transaction.service.BeneficiaryService;
import bankproject.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final BeneficiaryService beneficiaryService;

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransferResponse transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody TransferRequest request) {
        return transferService.initiate(CurrentUser.get(), request, idempotencyKey);
    }

    @GetMapping("/api/transfers")
    public Page<TransferResponse> mine(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return transferService.mine(CurrentUser.get(), page, size);
    }

    @GetMapping("/api/transfers/limits")
    public LimitsResponse limits() {
        return transferService.limits(CurrentUser.get());
    }

    @GetMapping("/api/transfers/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return transferService.get(CurrentUser.get(), id);
    }

    @GetMapping("/api/admin/transfers")
    public Page<TransferResponse> all(@RequestParam(required = false) TransferStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        return transferService.all(status, page, size);
    }

    @GetMapping("/api/beneficiaries")
    public List<BeneficiaryResponse> beneficiaries() {
        return beneficiaryService.list(CurrentUser.get());
    }

    @GetMapping("/api/beneficiaries/lookup/{accountNumber}")
    public AccountLookup lookup(@PathVariable String accountNumber) {
        return beneficiaryService.lookup(accountNumber);
    }

    @PostMapping("/api/beneficiaries")
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse addBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        return beneficiaryService.add(CurrentUser.get(), request);
    }

    @DeleteMapping("/api/beneficiaries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBeneficiary(@PathVariable UUID id) {
        beneficiaryService.remove(CurrentUser.get(), id);
    }
}
