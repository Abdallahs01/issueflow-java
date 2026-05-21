package com.att.tdp.issueflow.comment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @JsonAlias("body") @NotBlank @Size(max = 4000) String content
) {
}
