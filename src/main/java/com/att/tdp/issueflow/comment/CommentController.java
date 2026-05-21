package com.att.tdp.issueflow.comment;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> getCommentsForTicket(@PathVariable Long ticketId) {
        return commentService.getCommentsForTicket(ticketId);
    }

    @PostMapping
    public CommentResponse createComment(@PathVariable Long ticketId, @Valid @RequestBody CreateCommentRequest request) {
        return commentService.createComment(ticketId, request);
    }

    @PostMapping("/update/{commentId}")
    public void updateComment(@PathVariable Long ticketId, @PathVariable Long commentId, @Valid @RequestBody UpdateCommentRequest request) {
        commentService.updateComment(ticketId, commentId, request);
    }

    @PatchMapping("/{commentId}")
    public void patchComment(@PathVariable Long ticketId, @PathVariable Long commentId, @Valid @RequestBody UpdateCommentRequest request) {
        commentService.updateComment(ticketId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long ticketId, @PathVariable Long commentId) {
        commentService.deleteComment(ticketId, commentId);
    }
}
