package com.demo.teamboard.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the caller's tenant out of the validated access token.
 *
 * <p>This is the single source of truth for "which organization is this request for". There is
 * deliberately no endpoint that takes an org id as a parameter, header or body field: if the client
 * could name its own org, every authorization decision in the app would be forgeable. The org comes
 * from the signed token or it doesn't come at all.
 */
public final class OrgContext {

    private OrgContext() {
    }

    public static String orgId(Jwt jwt) {
        return jwt.getClaimAsString(OrgIdValidator.ORG_ID_CLAIM);
    }
}
