package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTicketRequest(
        @Size(max = 200) String title,
        @Size(max = 4000) String description,
        TicketStatus status,
        TicketPriority priority,
        TicketType type,
        Long assigneeId,
        LocalDate dueDate
) {
}
