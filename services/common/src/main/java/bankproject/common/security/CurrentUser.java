package bankproject.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import bankproject.common.web.ApiException;

public final class CurrentUser {

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw ApiException.unauthorized("Not authenticated");
    }

    private CurrentUser() {
    }
}
