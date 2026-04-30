package com.financialhub.financialhubapi.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Throws 404 if the caller is not ADMIN and the target id does not match the JWT userId.
     * Uses NOT_FOUND instead of FORBIDDEN to avoid leaking resource existence.
     */
    public static void requireOwnership(Jwt jwt, UUID id) {
        if (!isAdmin(jwt) && !id.equals(extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
