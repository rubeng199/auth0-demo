package com.demo.teamboard.web;

import java.util.List;

import com.demo.teamboard.config.TeamboardProperties;
import com.demo.teamboard.security.OrgContext;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Echoes back what the server actually sees in the caller's token.
 *
 * <p>Not needed by the app, but it is what turns this from an app into a demo: the UI can show the
 * org and roles the API derived from the token, so switching organizations visibly changes what the
 * backend believes about you.
 */
@RestController
public class MeController {

    private final TeamboardProperties teamboard;

    MeController(TeamboardProperties teamboard) {
        this.teamboard = teamboard;
    }

    public record MeResponse(String subject, String orgId, List<String> roles) {
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(teamboard.rolesClaim());
        return new MeResponse(
                jwt.getSubject(),
                OrgContext.orgId(jwt),
                roles == null ? List.of() : roles);
    }
}
