package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class TicketEscalationService {

    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public TicketEscalationService(TicketRepository ticketRepository, AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedDelayString = "${issueflow.escalation.fixed-delay-ms}")
    @Transactional
    public void escalateOverdueTickets() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Ticket> overdueTickets = ticketRepository.findByDeletedFalseAndStatusNotAndDueDateBefore(TicketStatus.DONE, now);

        for (Ticket ticket : overdueTickets) {
            escalateTicketIfNeeded(ticket, today);
        }
    }

    private void escalateTicketIfNeeded(Ticket ticket, LocalDate today) {
        if (wasEscalatedToday(ticket, today)) {
            return;
        }

        TicketPriority previousPriority = ticket.getPriority();

        if (ticket.getPriority() == TicketPriority.CRITICAL) {
            ticket.setOverdue(true);
        } else {
            ticket.setPriority(nextPriority(ticket.getPriority()));
            ticket.setOverdue(false);
        }

        ticket.setLastAutoEscalatedAt(Instant.now());
        auditLogService.recordSystemAction(
                "TICKET",
                ticket.getId(),
                "AUTO_ESCALATE",
                "Ticket priority changed from " + previousPriority + " to " + ticket.getPriority() + "."
        );
    }

    private boolean wasEscalatedToday(Ticket ticket, LocalDate today) {
        if (ticket.getLastAutoEscalatedAt() == null) {
            return false;
        }

        LocalDate lastEscalationDate = ticket.getLastAutoEscalatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return lastEscalationDate.equals(today);
    }

    private TicketPriority nextPriority(TicketPriority priority) {
        return switch (priority) {
            case LOW -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH, CRITICAL -> TicketPriority.CRITICAL;
        };
    }
}
