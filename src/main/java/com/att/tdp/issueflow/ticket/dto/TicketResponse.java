package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;

import java.time.Instant;
import java.time.LocalDate;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketType type,
        Long projectId,
        Long assigneeId,
        LocalDate dueDate,
        boolean overdue,
        boolean deleted,
        Instant lastAutoEscalatedAt,
        Long version
) {

    public static TicketResponse from(Ticket ticket) {
        Long assigneeId = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getType(),
                ticket.getProject().getId(),
                assigneeId,
                ticket.getDueDate(),
                ticket.isOverdue(),
                ticket.isDeleted(),
                ticket.getLastAutoEscalatedAt(),
                ticket.getVersion()
        );
    }
}
