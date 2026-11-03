package bankproject.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import bankproject.common.web.ApiException;
import bankproject.transaction.domain.DailyUsage;
import bankproject.transaction.domain.DailyUsageRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransferLimits {

    static final ZoneId BANK_ZONE = ZoneId.of("Asia/Kolkata");

    private final DailyUsageRepository usage;

    @Value("${bank.limits.per-transfer:200000.00}")
    private BigDecimal perTransfer;

    @Value("${bank.limits.daily:500000.00}")
    private BigDecimal daily;

    /** Reserves {@code amount} against today's limit, holding a row lock until the transaction ends. */
    public void reserve(String userId, BigDecimal amount) {
        if (amount.compareTo(perTransfer) > 0) {
            throw ApiException.unprocessable("LIMIT_EXCEEDED", "Maximum per transfer is " + perTransfer);
        }
        DailyUsage today = lockToday(userId);
        if (today.getUsed().add(amount).compareTo(daily) > 0) {
            throw ApiException.unprocessable("LIMIT_EXCEEDED",
                    "Daily transfer limit reached. Remaining today: " + daily.subtract(today.getUsed()));
        }
        today.add(amount);
    }

    /** Gives back a reservation when a transfer fails. Uses the original day of the transfer. */
    public void release(String userId, LocalDate day, BigDecimal amount) {
        usage.lock(new DailyUsage.Key(userId, day)).ifPresent(u -> u.release(amount));
    }

    public BigDecimal perTransfer() {
        return perTransfer;
    }

    public BigDecimal daily() {
        return daily;
    }

    public BigDecimal usedToday(String userId) {
        return usage.findById(new DailyUsage.Key(userId, today())).map(DailyUsage::getUsed).orElse(BigDecimal.ZERO);
    }

    static LocalDate today() {
        return LocalDate.now(BANK_ZONE);
    }

    private DailyUsage lockToday(String userId) {
        var key = new DailyUsage.Key(userId, today());
        return usage.lock(key).orElseGet(() -> {
            try {
                return usage.saveAndFlush(new DailyUsage(userId, today()));
            } catch (DataIntegrityViolationException raced) {
                throw ApiException.conflict("Another transfer is being processed, please retry");
            }
        });
    }
}
