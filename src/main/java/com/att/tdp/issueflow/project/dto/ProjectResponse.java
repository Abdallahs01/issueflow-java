package com.att.tdp.issueflow.project.dto;

import com.att.tdp.issueflow.project.Project;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        boolean deleted
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isDeleted()
        );
    }
}
