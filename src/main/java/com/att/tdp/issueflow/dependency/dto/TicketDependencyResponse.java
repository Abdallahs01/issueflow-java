package com.att.tdp.issueflow.dependency.dto;

import com.att.tdp.issueflow.dependency.TicketDependency;
import com.att.tdp.issueflow.ticket.TicketStatus;

public record TicketDependencyResponse(
        Long ticketId,
        Long blockerId,
        String blockerTitle,
        TicketStatus blockerStatus
) {

    public static TicketDependencyResponse from(TicketDependency dependency) {
        return new TicketDependencyResponse(
                dependency.getTicket().getId(),
                dependency.getBlocker().getId(),
                dependency.getBlocker().getTitle(),
                dependency.getBlocker().getStatus()
        );
    }
}
