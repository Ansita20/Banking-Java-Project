package bankproject.identity.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.events.IdentityEvents.OtpPurpose;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.security.JwtService;
import bankproject.common.web.ApiException;
import bankproject.identity.api.AuthDtos.ChangePasswordRequest;
import bankproject.identity.api.AuthDtos.LoginRequest;
import bankproject.identity.api.AuthDtos.LoginResponse;
import bankproject.identity.api.AuthDtos.RegisterRequest;
import bankproject.identity.api.AuthDtos.RegisterResponse;
import bankproject.identity.api.AuthDtos.ResetPasswordRequest;
import bankproject.identity.api.UserDtos.UserResponse;
import bankproject.identity.domain.Role;
import bankproject.identity.domain.UserAccount;
import bankproject.identity.domain.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final CustomerEvents customerEvents;

    /** Hash checked against when the email is unknown, so both paths cost one bcrypt verification. */
    private String dummyHash;

    @PostConstruct
    void initDummyHash() {
        dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        UserAccount user = users.save(UserAccount.register(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), Role.USER));
        otpService.issue(user, OtpPurpose.EMAIL_VERIFICATION);
        customerEvents.publishSnapshot(user);
        return new RegisterResponse(user.getId(), user.getEmail(),
                "A 6-digit verification code has been sent to your email");
    }

    /** noRollbackFor: a wrong code must still count as an attempt. */
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyEmail(String email, String code) {
        UserAccount user = users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> ApiException.badRequest("Code is invalid or expired, request a new one"));
        if (user.isEmailVerified()) {
            return;
        }
        otpService.verify(user, OtpPurpose.EMAIL_VERIFICATION, code);
        user.setEmailVerified(true);
        customerEvents.publishSnapshot(user);
    }

    /** Silent when the email is unknown so the endpoint cannot be used to enumerate customers. */
    @Transactional
    public void resendVerification(String email) {
        users.findByEmailIgnoreCase(normalize(email))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(u -> otpService.issue(u, OtpPurpose.EMAIL_VERIFICATION));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest request) {
        Optional<UserAccount> found = users.findByEmailIgnoreCase(normalize(request.email()));
        if (found.isEmpty()) {
            // Spend the same time as a real check to avoid leaking which emails exist.
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        UserAccount user = found.get();
        if (user.isLockedOut()) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Too many failed attempts. Try again in 15 minutes or reset your password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_DISABLED", "Your access has been suspended by the bank");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Verify your email before logging in");
        }
        user.recordSuccessfulLogin();
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name()));
        return new LoginResponse(token, "Bearer", jwtService.ttl().toSeconds(), UserResponse.from(user));
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(normalize(email))
                .ifPresent(u -> otpService.issue(u, OtpPurpose.PASSWORD_RESET));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> ApiException.badRequest("Code is invalid or expired, request a new one"));
        otpService.verify(user, OtpPurpose.PASSWORD_RESET, request.code());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.recordSuccessfulLogin();
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        UserAccount user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
