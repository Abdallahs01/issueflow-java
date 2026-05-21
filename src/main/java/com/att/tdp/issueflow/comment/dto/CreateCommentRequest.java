package com.att.tdp.issueflow.comment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull Long authorId,
        @JsonAlias("body") @NotBlank @Size(max = 4000) String content
) {
}
