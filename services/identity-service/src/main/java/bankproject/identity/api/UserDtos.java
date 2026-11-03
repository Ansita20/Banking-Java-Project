package bankproject.identity.api;

import java.time.Instant;
import java.time.LocalDate;

import bankproject.identity.domain.KycProfile;
import bankproject.identity.domain.UserAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    public record UserResponse(String userId, String email, String firstName, String lastName, String role,
                               boolean emailVerified, boolean enabled, boolean kycCompleted, Instant createdAt,
                               ProfileResponse profile) {

        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getRole().name(), user.isEmailVerified(), user.isEnabled(), user.isKycCompleted(),
                    user.getCreatedAt(), ProfileResponse.from(user.getProfile()));
        }
    }

    /** Identity numbers are never returned in full. */
    public record ProfileResponse(String phone, LocalDate dateOfBirth, String gender, String addressLine, String city,
                                  String state, String postalCode, String panNumberMasked,
                                  String governmentIdMasked) {

        static ProfileResponse from(KycProfile p) {
            if (p == null) {
                return null;
            }
            return new ProfileResponse(p.getPhone(), p.getDateOfBirth(), p.getGender(), p.getAddressLine(),
                    p.getCity(), p.getState(), p.getPostalCode(), mask(p.getPanNumber()),
                    mask(p.getGovernmentId()));
        }

        private static String mask(String value) {
            if (value == null || value.length() < 4) {
                return value;
            }
            return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
        }
    }

    public record KycRequest(
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a 10-digit mobile number") String phone,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "MALE|FEMALE|OTHER") String gender,
            @NotBlank @Size(max = 255) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit PIN code") String postalCode,
            @NotBlank @Pattern(regexp = "^[A-Z]{5}\\d{4}[A-Z]$", message = "must be a valid PAN, e.g. ABCDE1234F")
            String panNumber,
            @NotBlank @Pattern(regexp = "^\\d{12}$", message = "must be a 12-digit Aadhaar number")
            String governmentId) {
    }

    public record UserStatusRequest(boolean enabled) {
    }

    private UserDtos() {
    }
}
