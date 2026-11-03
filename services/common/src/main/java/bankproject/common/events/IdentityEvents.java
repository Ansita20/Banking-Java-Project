package bankproject.common.events;

public final class IdentityEvents {

    public enum OtpPurpose { EMAIL_VERIFICATION, PASSWORD_RESET }

    public record OtpIssued(String userId, String email, String firstName, String otp, OtpPurpose purpose)
            implements BankEvent {
    }

    /**
     * Full snapshot of the customer, published on every change. Downstream services keep a
     * local read replica of it instead of calling identity-service synchronously.
     */
    public record CustomerUpdated(String userId, String email, String fullName, boolean emailVerified,
                                  boolean kycCompleted, boolean enabled) implements BankEvent {
    }

    private IdentityEvents() {
    }
}
