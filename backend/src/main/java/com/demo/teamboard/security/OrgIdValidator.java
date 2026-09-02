package com.demo.teamboard.security;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects tokens whose {@code org_id} is missing or not an organization this API serves.
 *
 * <p>Auth0's documentation is explicit that applications must check {@code org_id} against a known
 * set rather than trusting whatever arrives. A validly signed token for an organization you have
 * never heard of is still a token you should refuse -- otherwise a newly created org in the same
 * tenant would silently gain access to this API.
 *
 * <p>Running this as a token validator (rather than a check inside controllers) means an
 * unrecognised org fails at the edge with a 401, and no request handler ever executes.
 */
public class OrgIdValidator implements OAuth2TokenValidator<Jwt> {

    public static final String ORG_ID_CLAIM = "org_id";

    private final List<String> allowedOrgIds;

    public OrgIdValidator(List<String> allowedOrgIds) {
        this.allowedOrgIds = List.copyOf(allowedOrgIds);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String orgId = token.getClaimAsString(ORG_ID_CLAIM);
        if (orgId == null || orgId.isBlank()) {
            return failure("Token has no org_id claim; log in through an organization");
        }
        if (!allowedOrgIds.contains(orgId)) {
            return failure("Unknown organization: " + orgId);
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }
}
