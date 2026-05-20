package com.att.tdp.issueflow.dependency.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTicketDependencyRequest(
        @NotNull Long blockedBy
) {
}
