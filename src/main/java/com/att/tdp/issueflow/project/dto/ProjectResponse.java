package com.att.tdp.issueflow.project.dto;

import com.att.tdp.issueflow.project.Project;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        boolean deleted
) {

    public static ProjectResponse from(Project project) {
        Long ownerId = project.getOwner() == null ? null : project.getOwner().getId();

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                ownerId,
                project.isDeleted()
        );
    }
}
