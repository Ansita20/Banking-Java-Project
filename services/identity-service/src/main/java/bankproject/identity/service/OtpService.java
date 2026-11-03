package bankproject.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import bankproject.common.events.IdentityEvents.OtpIssued;
import bankproject.common.events.IdentityEvents.OtpPurpose;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import bankproject.common.web.ApiException;
import bankproject.identity.domain.OneTimeCode;
import bankproject.identity.domain.OneTimeCodeRepository;
import bankproject.identity.domain.UserAccount;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final OneTimeCodeRepository codes;
    private final OutboxWriter outbox;
    private final SecureRandom random = new SecureRandom();

    /** Creates a new code and queues its delivery; caller must be in a transaction. */
    public void issue(UserAccount user, OtpPurpose purpose) {
        codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(last -> last.getCreatedAt().isAfter(Instant.now().minus(RESEND_COOLDOWN)))
                .ifPresent(last -> {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN",
                            "Please wait a minute before requesting another code");
                });
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.save(new OneTimeCode(user.getId(), purpose, hash(user.getId(), code)));
        outbox.enqueue(Topics.IDENTITY_EVENTS, user.getId(),
                new OtpIssued(user.getId(), user.getEmail(), user.getFirstName(), code, purpose));
    }

    /** Checks the latest code; every wrong guess counts towards the attempt limit. */
    public void verify(UserAccount user, OtpPurpose purpose, String code) {
        OneTimeCode otp = codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(OneTimeCode::isUsable)
                .orElseThrow(() -> ApiException.badRequest("Code is invalid or expired, request a new one"));
        otp.recordAttempt();
        boolean matches = MessageDigest.isEqual(
                otp.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(user.getId(), code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw ApiException.badRequest("Incorrect code");
        }
        otp.consume();
    }

    private static String hash(String userId, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((userId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
