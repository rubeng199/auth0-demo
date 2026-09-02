package com.demo.teamboard.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo-specific configuration, bound from the {@code teamboard.*} block in application.yml.
 *
 * @param claimNamespace namespace prefix for custom claims. Auth0 discards custom claims that
 *                       are not namespaced URIs, so the post-login Action writes roles to
 *                       "{namespace}/roles" and we have to read them back under the same key.
 * @param allowedOrgIds  organizations this API will serve. Any token carrying an org_id outside
 *                       this list is rejected outright.
 * @param corsOrigin     origin of the Vite dev server.
 */
@ConfigurationProperties(prefix = "teamboard")
public record TeamboardProperties(
        String claimNamespace,
        List<String> allowedOrgIds,
        String corsOrigin) {

    public String rolesClaim() {
        return claimNamespace() + "/roles";
    }
}
