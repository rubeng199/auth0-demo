package com.demo.teamboard.projects;

/**
 * A project belonging to exactly one organization.
 *
 * <p>{@code orgId} is the tenant boundary. Every read and write goes through it.
 */
public record Project(String id, String orgId, String name, String createdBy) {
}
