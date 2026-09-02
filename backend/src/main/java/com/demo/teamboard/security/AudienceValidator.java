package com.demo.teamboard.security;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects tokens that were not minted for this API.
 *
 * <p>Without this check, any token signed by the same Auth0 tenant would be accepted here --
 * including one issued for a completely different API. Spring Boot can do this declaratively via
 * {@code spring.security.oauth2.resourceserver.jwt.audiences}, but we build the decoder by hand in
 * {@link SecurityConfig} so that all three checks (issuer, audience, organization) are visible in
 * one place.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audience = token.getAudience();
        if (audience != null && audience.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "Token audience does not include " + expectedAudience,
                null));
    }
}
