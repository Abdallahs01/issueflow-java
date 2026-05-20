package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.UserRole;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 120) String fullName,
        UserRole role
) {
}
