package com.att.tdp.issueflow.mention;

import com.att.tdp.issueflow.comment.dto.CommentResponse;

import java.util.List;

public record MentionPageResponse(
        List<CommentResponse> data,
        long total,
        int page
) {
}
