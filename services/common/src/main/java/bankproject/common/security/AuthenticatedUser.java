package bankproject.common.security;

public record AuthenticatedUser(String userId, String email, String role) {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
