package com.finantialhub.finantialhubapi.infrastructure.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    /**
     * Returns requestedUserId if the caller is ADMIN and requestedUserId is non-null.
     * Otherwise returns the userId from the JWT token.
     */
    public static UUID resolveUserId(Jwt jwt, UUID requestedUserId) {
        if (isAdmin(jwt) && requestedUserId != null) {
            return requestedUserId;
        }
        return extractUserId(jwt);
    }
}
