package com.demo.teamboard.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class OrgIdValidatorTest {

    private final OrgIdValidator validator = new OrgIdValidator(List.of("org_acme", "org_globex"));

    private Jwt tokenWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void acceptsKnownOrganization() {
        Jwt jwt = tokenWithClaims(Map.of("sub", "auth0|1", "org_id", "org_acme"));
        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsUnknownOrganization() {
        // A validly signed token for an org in the same tenant that this API does not serve.
        Jwt jwt = tokenWithClaims(Map.of("sub", "auth0|1", "org_id", "org_someone_else"));
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithNoOrganization() {
        // A personal (non-org) login should not reach a B2B API.
        Jwt jwt = tokenWithClaims(Map.of("sub", "auth0|1"));
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}
