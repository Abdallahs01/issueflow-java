package com.att.tdp.issueflow.project.dto;

import com.att.tdp.issueflow.project.Project;

import java.util.Set;
import java.util.stream.Collectors;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        Set<Long> developerIds,
        boolean deleted
) {

    public static ProjectResponse from(Project project) {
        Long ownerId = project.getOwner() == null ? null : project.getOwner().getId();
        Set<Long> developerIds = project.getDevelopers()
                .stream()
                .map(developer -> developer.getId())
                .collect(Collectors.toSet());

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                ownerId,
                developerIds,
                project.isDeleted()
        );
    }
}
