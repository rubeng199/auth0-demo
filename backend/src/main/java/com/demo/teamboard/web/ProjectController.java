package com.demo.teamboard.web;

import java.util.List;

import com.demo.teamboard.projects.Project;
import com.demo.teamboard.projects.ProjectRepository;
import com.demo.teamboard.security.OrgContext;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project CRUD, scoped to the caller's organization.
 *
 * <p>The organization always comes from {@link OrgContext#orgId(Jwt)} -- never from a path
 * variable, query parameter or request body. That is the whole tenancy model: the client can ask
 * for "my projects", but has no vocabulary for asking about anyone else's.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projects;

    ProjectController(ProjectRepository projects) {
        this.projects = projects;
    }

    public record CreateProjectRequest(String name) {
    }

    /** Any authenticated member of a known organization may read that organization's projects. */
    @GetMapping
    public List<Project> list(@AuthenticationPrincipal Jwt jwt) {
        return projects.findByOrg(OrgContext.orgId(jwt));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Project create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateProjectRequest request) {
        return projects.create(OrgContext.orgId(jwt), request.name(), jwt.getSubject());
    }

    /**
     * Deleting is admin-only, and additionally scoped by org. An Acme admin presenting a valid
     * Globex project id gets a 404, not a deletion -- being an admin somewhere does not make you an
     * admin everywhere.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        boolean deleted = projects.deleteByIdAndOrg(id, OrgContext.orgId(jwt));
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
