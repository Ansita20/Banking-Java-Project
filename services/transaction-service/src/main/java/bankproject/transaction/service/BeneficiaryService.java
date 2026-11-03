package bankproject.transaction.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.security.AuthenticatedUser;
import bankproject.common.web.ApiException;
import bankproject.transaction.api.TransferDtos.BeneficiaryRequest;
import bankproject.transaction.api.TransferDtos.BeneficiaryResponse;
import bankproject.transaction.client.AccountDirectoryClient;
import bankproject.transaction.client.AccountDirectoryClient.AccountLookup;
import bankproject.transaction.domain.Beneficiary;
import bankproject.transaction.domain.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;
    private final AccountDirectoryClient accountDirectory;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> list(AuthenticatedUser user) {
        return beneficiaries.findByUserIdOrderByNickname(user.userId()).stream()
                .map(BeneficiaryResponse::from).toList();
    }

    public AccountLookup lookup(String accountNumber) {
        var result = accountDirectory.lookup(accountNumber);
        if (result.unavailable()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LOOKUP_UNAVAILABLE",
                    "Account verification is temporarily unavailable, please retry");
        }
        return result.account().orElseThrow(() -> ApiException.notFound("Account does not exist"));
    }

    /** Payees are verified against account-service before being saved. */
    @Transactional
    public BeneficiaryResponse add(AuthenticatedUser user, BeneficiaryRequest request) {
        if (beneficiaries.existsByUserIdAndAccountNumber(user.userId(), request.accountNumber())) {
            throw ApiException.conflict("This account is already a beneficiary");
        }
        AccountLookup account = lookup(request.accountNumber());
        return BeneficiaryResponse.from(beneficiaries.save(new Beneficiary(user.userId(), request.nickname().trim(),
                account.accountNumber(), account.holderName())));
    }

    @Transactional
    public void remove(AuthenticatedUser user, UUID id) {
        beneficiaries.delete(beneficiaries.findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> ApiException.notFound("Beneficiary not found")));
    }
}
