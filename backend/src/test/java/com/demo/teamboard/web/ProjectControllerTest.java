package com.demo.teamboard.web;

import java.util.List;

import com.demo.teamboard.projects.Project;
import com.demo.teamboard.projects.ProjectRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the two rules that matter: role gates writes, and organization gates everything.
 *
 * <p>The real {@link JwtDecoder} is mocked out and authentication is injected directly, so these
 * run offline with no Auth0 tenant. That also means they exercise the authorization layer rather
 * than token validation -- {@link com.demo.teamboard.security.OrgIdValidatorTest} covers the latter.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    private static final String ACME = "org_acme_test";
    private static final String GLOBEX = "org_globex_test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projects;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor caller(String orgId, String role) {
        return jwt()
                .jwt(builder -> builder.subject("auth0|demo-user").claim("org_id", orgId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyTheCallersOrganizationsProjects() throws Exception {
        mockMvc.perform(get("/api/projects").with(caller(ACME, "member")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orgId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo(ACME))));
    }

    @Test
    void memberCannotCreateProjects() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(caller(ACME, "member"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Should not exist\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateProjectInOwnOrganization() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(caller(ACME, "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Initiative\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").value(ACME))
                .andExpect(jsonPath("$.name").value("New Initiative"));
    }

    @Test
    void adminOfOneOrganizationCannotDeleteAnothersProject() throws Exception {
        List<Project> globexProjects = projects.findByOrg(GLOBEX);
        String victimId = globexProjects.get(0).id();

        // Valid admin, valid project id -- but the project belongs to a different tenant.
        mockMvc.perform(delete("/api/projects/" + victimId).with(caller(ACME, "admin")))
                .andExpect(status().isNotFound());

        // And it is still there.
        org.assertj.core.api.Assertions.assertThat(projects.findByIdAndOrg(victimId, GLOBEX))
                .isPresent();
    }
}
