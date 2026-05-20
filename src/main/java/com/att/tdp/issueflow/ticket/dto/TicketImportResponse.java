package com.att.tdp.issueflow.ticket.dto;

import java.util.List;

public record TicketImportResponse(
        int created,
        int failed,
        List<String> errors
) {
}
