package com.att.tdp.issueflow.comment;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.mention.MentionService;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final MentionService mentionService;

    public CommentService(CommentRepository commentRepository, TicketService ticketService, UserService userService, AuditLogService auditLogService, MentionService mentionService) {
        this.commentRepository = commentRepository;
        this.ticketService = ticketService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.mentionService = mentionService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForTicket(Long ticketId) {
        ticketService.findActiveTicketById(ticketId);

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(comment -> CommentResponse.from(comment, mentionService.getMentionedUsers(comment.getId())))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketService.findActiveTicketById(ticketId);
        User author = userService.findUserById(request.authorId());

        Comment comment = new Comment(ticket, author, request.body());
        Comment savedComment = commentRepository.save(comment);
        mentionService.refreshMentions(savedComment);
        auditLogService.recordCurrentUserAction("COMMENT", savedComment.getId(), "CREATE", "Comment was created.");

        return CommentResponse.from(savedComment, mentionService.getMentionedUsers(savedComment.getId()));
    }

    @Transactional
    public void updateComment(Long ticketId, Long commentId, UpdateCommentRequest request) {
        ticketService.findActiveTicketById(ticketId);
        Comment comment = findCommentById(commentId);
        comment.setBody(request.body());
        mentionService.refreshMentions(comment);
        auditLogService.recordCurrentUserAction("COMMENT", comment.getId(), "UPDATE", "Comment was updated.");
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        ticketService.findActiveTicketById(ticketId);
        Comment comment = findCommentById(commentId);
        auditLogService.recordCurrentUserAction("COMMENT", comment.getId(), "DELETE", "Comment was deleted.");
        commentRepository.delete(comment);
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment was not found."));
    }
}
