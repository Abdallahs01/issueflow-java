package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull UserRole role
) {
}
