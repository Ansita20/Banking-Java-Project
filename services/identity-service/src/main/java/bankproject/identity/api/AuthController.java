package bankproject.identity.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bankproject.identity.api.AuthDtos.EmailRequest;
import bankproject.identity.api.AuthDtos.LoginRequest;
import bankproject.identity.api.AuthDtos.LoginResponse;
import bankproject.identity.api.AuthDtos.RegisterRequest;
import bankproject.identity.api.AuthDtos.RegisterResponse;
import bankproject.identity.api.AuthDtos.ResetPasswordRequest;
import bankproject.identity.api.AuthDtos.VerifyEmailRequest;
import bankproject.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Map<String, String> ACCEPTED =
            Map.of("message", "If the email is registered, a code has been sent");

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.email(), request.code());
        return Map.of("message", "Email verified, you can now log in");
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> resendOtp(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ACCEPTED;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ACCEPTED;
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Map.of("message", "Password updated, you can now log in");
    }
}
