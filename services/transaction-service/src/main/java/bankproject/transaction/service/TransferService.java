package bankproject.transaction.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.events.AccountCommands.DebitAccount;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.web.ApiException;
import bankproject.transaction.api.TransferDtos.LimitsResponse;
import bankproject.transaction.api.TransferDtos.TransferRequest;
import bankproject.transaction.api.TransferDtos.TransferResponse;
import bankproject.transaction.client.AccountDirectoryClient;
import bankproject.transaction.domain.BeneficiaryRepository;
import bankproject.transaction.domain.Transfer;
import bankproject.transaction.domain.TransferRepository;
import bankproject.transaction.domain.TransferStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transfers;
    private final BeneficiaryRepository beneficiaries;
    private final TransferLimits limits;
    private final AccountDirectoryClient accountDirectory;
    private final OutboxWriter outbox;

    /**
     * Starts the saga and returns immediately with status PENDING; the client polls
     * GET /api/transfers/{id}. A repeated Idempotency-Key returns the original transfer.
     */
    @Transactional
    public TransferResponse initiate(AuthenticatedUser user, TransferRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
        String toAccount = resolveDestination(user, request);
        String fingerprint = fingerprint(request.fromAccount(), toAccount, request.amount().toPlainString());

        var existing = transfers.findByInitiatorUserIdAndIdempotencyKey(user.userId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw ApiException.conflict("Idempotency-Key already used for a different transfer");
            }
            return TransferResponse.from(existing.get());
        }

        if (request.fromAccount().equals(toAccount)) {
            throw ApiException.badRequest("Source and destination accounts must differ");
        }
        // Fail fast on obviously bad destinations; if account-service is down, the saga decides.
        var lookup = accountDirectory.lookup(toAccount);
        if (!lookup.unavailable()) {
            var account = lookup.account().orElseThrow(() ->
                    ApiException.unprocessable("UNKNOWN_BENEFICIARY", "Destination account does not exist"));
            if (!account.canReceive()) {
                throw ApiException.unprocessable("BENEFICIARY_INACTIVE", "Destination account cannot receive funds");
            }
        }

        limits.reserve(user.userId(), request.amount());
        String description = request.description() == null || request.description().isBlank()
                ? "Fund transfer" : request.description().trim();
        Transfer transfer = transfers.save(new Transfer(user.userId(), request.fromAccount(), toAccount,
                request.amount(), description, idempotencyKey, fingerprint));
        outbox.enqueue(Topics.ACCOUNT_COMMANDS, transfer.getFromAccount(), new DebitAccount(transfer.getId(),
                transfer.getFromAccount(), transfer.getAmount(), user.userId(), "To " + toAccount + ": " + description));
        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse get(AuthenticatedUser user, UUID id) {
        return transfers.findById(id)
                .filter(t -> user.isAdmin() || t.getInitiatorUserId().equals(user.userId()))
                .map(TransferResponse::from)
                .orElseThrow(() -> ApiException.notFound("Transfer not found"));
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> mine(AuthenticatedUser user, int page, int size) {
        return transfers.findByInitiatorUserIdOrderByCreatedAtDesc(user.userId(), PageRequest.of(page, Math.min(size, 100)))
                .map(TransferResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> all(TransferStatus status, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200));
        return (status == null ? transfers.findAllByOrderByCreatedAtDesc(pageable)
                : transfers.findByStatusOrderByCreatedAtDesc(status, pageable)).map(TransferResponse::from);
    }

    @Transactional(readOnly = true)
    public LimitsResponse limits(AuthenticatedUser user) {
        var used = limits.usedToday(user.userId());
        return new LimitsResponse(limits.perTransfer(), limits.daily(), used, limits.daily().subtract(used));
    }

    private String resolveDestination(AuthenticatedUser user, TransferRequest request) {
        if (request.beneficiaryId() != null) {
            return beneficiaries.findByIdAndUserId(request.beneficiaryId(), user.userId())
                    .orElseThrow(() -> ApiException.notFound("Beneficiary not found"))
                    .getAccountNumber();
        }
        if (request.toAccount() == null) {
            throw ApiException.badRequest("Either toAccount or beneficiaryId is required");
        }
        return request.toAccount();
    }

    private static String fingerprint(String... parts) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("|", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
