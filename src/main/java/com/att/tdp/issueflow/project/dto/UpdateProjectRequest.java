package com.att.tdp.issueflow.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 120) String name,
        @Size(max = 1000) String description
) {
}
