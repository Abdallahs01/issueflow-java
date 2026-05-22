package com.att.tdp.issueflow.project.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateProjectRequest(
        @Size(max = 120) String name,
        @Size(max = 1000) String description,
        Set<Long> developerIds
) {

    public UpdateProjectRequest(String name, String description) {
        this(name, description, null);
    }
}
