package bankproject.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    static final String PASSWORD_MESSAGE = "must be 8-72 characters with at least one letter and one digit";

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password) {
    }

    public record RegisterResponse(String userId, String email, String message) {
    }

    public record VerifyEmailRequest(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
    }

    public record EmailRequest(@NotBlank @Email String email) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserDtos.UserResponse user) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    private AuthDtos() {
    }
}
