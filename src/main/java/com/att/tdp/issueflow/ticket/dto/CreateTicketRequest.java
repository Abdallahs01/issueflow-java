package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull TicketStatus status,
        @NotNull TicketPriority priority,
        @NotNull TicketType type,
        @NotNull Long projectId,
        Long assigneeId,
        Instant dueDate
) {
}
