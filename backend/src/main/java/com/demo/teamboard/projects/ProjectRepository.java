package com.demo.teamboard.projects;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.demo.teamboard.config.TeamboardProperties;

import org.springframework.stereotype.Repository;

/**
 * In-memory store, seeded at startup with a couple of projects per configured organization.
 *
 * <p>Deliberately not a database: the interesting part of this demo is the token flow, and a real
 * datastore would add setup without adding anything to learn. Swapping this for Spring Data JPA
 * later changes nothing above it.
 *
 * <p>Every method that reaches a specific project takes an {@code orgId} alongside the id, so a
 * caller from one organization cannot address another organization's data even if it guesses a
 * valid id. Scoping is enforced by the method signature rather than by remembering to check.
 */
@Repository
public class ProjectRepository {

    private static final List<String> SEED_NAMES = List.of(
            "Website Redesign", "Q3 Roadmap", "Billing Migration", "Mobile App", "Data Warehouse");

    private final Map<String, Project> byId = new ConcurrentHashMap<>();

    ProjectRepository(TeamboardProperties properties) {
        List<String> orgIds = properties.allowedOrgIds();
        for (int i = 0; i < orgIds.size(); i++) {
            String orgId = orgIds.get(i);
            // Give each org a distinct slice of the seed names so the switch between tenants is
            // visually obvious in the UI.
            for (int j = 0; j < 2; j++) {
                String name = SEED_NAMES.get((i * 2 + j) % SEED_NAMES.size());
                Project project = new Project(UUID.randomUUID().toString(), orgId, name, "seed");
                byId.put(project.id(), project);
            }
        }
    }

    public List<Project> findByOrg(String orgId) {
        return byId.values().stream()
                .filter(project -> project.orgId().equals(orgId))
                .sorted(Comparator.comparing(Project::name))
                .toList();
    }

    public Optional<Project> findByIdAndOrg(String id, String orgId) {
        return Optional.ofNullable(byId.get(id))
                .filter(project -> project.orgId().equals(orgId));
    }

    public Project create(String orgId, String name, String createdBy) {
        Project project = new Project(UUID.randomUUID().toString(), orgId, name, createdBy);
        byId.put(project.id(), project);
        return project;
    }

    /** Returns true if a project was removed; false if it does not exist in that organization. */
    public boolean deleteByIdAndOrg(String id, String orgId) {
        Project existing = byId.get(id);
        if (existing == null || !existing.orgId().equals(orgId)) {
            return false;
        }
        return byId.remove(id, existing);
    }
}
