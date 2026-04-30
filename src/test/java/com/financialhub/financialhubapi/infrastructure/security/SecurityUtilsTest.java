package com.financialhub.financialhubapi.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    private Jwt buildJwt(UUID subject, String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject.toString())
                .claim("role", role)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void extractUserId_returnsUUIDFromSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId, "MEMBER");

        UUID result = SecurityUtils.extractUserId(jwt);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void isAdmin_returnsTrueForAdminRole() {
        Jwt jwt = buildJwt(UUID.randomUUID(), "ADMIN");
        assertThat(SecurityUtils.isAdmin(jwt)).isTrue();
    }

    @Test
    void isAdmin_returnsFalseForMemberRole() {
        Jwt jwt = buildJwt(UUID.randomUUID(), "MEMBER");
        assertThat(SecurityUtils.isAdmin(jwt)).isFalse();
    }

    @Test
    void resolveUserId_forMember_alwaysReturnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        UUID requestedUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "MEMBER");

        UUID result = SecurityUtils.resolveUserId(jwt, requestedUserId);

        assertThat(result).isEqualTo(jwtUserId);
    }

    @Test
    void resolveUserId_forMemberWithNullRequest_returnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "MEMBER");

        UUID result = SecurityUtils.resolveUserId(jwt, null);

        assertThat(result).isEqualTo(jwtUserId);
    }

    @Test
    void resolveUserId_forAdmin_returnsRequestedUserIdWhenProvided() {
        UUID jwtUserId = UUID.randomUUID();
        UUID requestedUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "ADMIN");

        UUID result = SecurityUtils.resolveUserId(jwt, requestedUserId);

        assertThat(result).isEqualTo(requestedUserId);
    }

    @Test
    void resolveUserId_forAdminWithNullRequest_returnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "ADMIN");

        UUID result = SecurityUtils.resolveUserId(jwt, null);

        assertThat(result).isEqualTo(jwtUserId);
    }
}
