package com.att.tdp.issueflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull Long ownerId,
        Set<Long> developerIds
) {

    public CreateProjectRequest(String name, String description, Long ownerId) {
        this(name, description, ownerId, Set.of());
    }
}
