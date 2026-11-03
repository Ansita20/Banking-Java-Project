package bankproject.transaction.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import bankproject.transaction.domain.Beneficiary;
import bankproject.transaction.domain.Transfer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TransferDtos {

    /** Pay either a raw account number or a saved beneficiary. */
    public record TransferRequest(
            @NotBlank @Pattern(regexp = "\\d{12}") String fromAccount,
            @Pattern(regexp = "\\d{12}") String toAccount,
            UUID beneficiaryId,
            @NotNull @DecimalMin("1.00") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description) {
    }

    public record TransferResponse(UUID transferId, String fromAccount, String toAccount, BigDecimal amount,
                                   String description, String status, String failureReason, Instant createdAt,
                                   Instant updatedAt) {

        public static TransferResponse from(Transfer t) {
            return new TransferResponse(t.getId(), t.getFromAccount(), t.getToAccount(), t.getAmount(),
                    t.getDescription(), t.getStatus().name(), t.getFailureReason(), t.getCreatedAt(),
                    t.getUpdatedAt());
        }
    }

    public record LimitsResponse(BigDecimal perTransferLimit, BigDecimal dailyLimit, BigDecimal usedToday,
                                 BigDecimal remainingToday) {
    }

    public record BeneficiaryRequest(
            @NotBlank @Size(max = 60) String nickname,
            @NotBlank @Pattern(regexp = "\\d{12}", message = "must be a 12-digit account number") String accountNumber) {
    }

    public record BeneficiaryResponse(UUID id, String nickname, String accountNumber, String holderName,
                                      Instant createdAt) {

        public static BeneficiaryResponse from(Beneficiary b) {
            return new BeneficiaryResponse(b.getId(), b.getNickname(), b.getAccountNumber(), b.getHolderName(),
                    b.getCreatedAt());
        }
    }

    private TransferDtos() {
    }
}
