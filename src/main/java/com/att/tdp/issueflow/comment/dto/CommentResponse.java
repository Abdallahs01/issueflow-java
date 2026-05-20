package com.att.tdp.issueflow.comment.dto;

import com.att.tdp.issueflow.comment.Comment;
import com.att.tdp.issueflow.mention.MentionedUserResponse;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String body,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        List<MentionedUserResponse> mentionedUsers
) {

    public static CommentResponse from(Comment comment) {
        return from(comment, List.of());
    }

    public static CommentResponse from(Comment comment, List<MentionedUserResponse> mentionedUsers) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getVersion(),
                mentionedUsers
        );
    }
}
