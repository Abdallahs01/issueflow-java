package com.att.tdp.issueflow.dependency;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.dependency.dto.CreateTicketDependencyRequest;
import com.att.tdp.issueflow.dependency.dto.TicketDependencyResponse;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.ticket.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketDependencyService {

    private final TicketDependencyRepository ticketDependencyRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public TicketDependencyService(TicketDependencyRepository ticketDependencyRepository, TicketRepository ticketRepository, AuditLogService auditLogService) {
        this.ticketDependencyRepository = ticketDependencyRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TicketDependencyResponse addDependency(Long ticketId, CreateTicketDependencyRequest request) {
        Ticket ticket = findActiveTicketById(ticketId);
        Ticket blocker = findActiveTicketById(request.blockedBy());

        validateDependency(ticket, blocker);

        TicketDependency dependency = new TicketDependency(ticket, blocker);
        TicketDependency savedDependency = ticketDependencyRepository.save(dependency);
        auditLogService.recordCurrentUserAction("TICKET", ticket.getId(), "ADD_DEPENDENCY", "Ticket was blocked by ticket " + blocker.getId() + ".");

        return TicketDependencyResponse.from(savedDependency);
    }

    @Transactional(readOnly = true)
    public List<TicketDependencyResponse> getDependencies(Long ticketId) {
        findActiveTicketById(ticketId);

        return ticketDependencyRepository.findByTicketId(ticketId)
                .stream()
                .map(TicketDependencyResponse::from)
                .toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        findActiveTicketById(ticketId);

        TicketDependency dependency = ticketDependencyRepository.findByTicketIdAndBlockerId(ticketId, blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket dependency was not found."));

        ticketDependencyRepository.delete(dependency);
        auditLogService.recordCurrentUserAction("TICKET", ticketId, "REMOVE_DEPENDENCY", "Ticket dependency was removed.");
    }

    @Transactional(readOnly = true)
    public boolean hasUnresolvedBlockers(Long ticketId) {
        return ticketDependencyRepository.existsByTicketIdAndBlockerStatusNot(ticketId, TicketStatus.DONE);
    }

    private void validateDependency(Ticket ticket, Ticket blocker) {
        if (ticket.getId().equals(blocker.getId())) {
            throw new BadRequestException("A ticket cannot depend on itself.");
        }

        if (!ticket.getProject().getId().equals(blocker.getProject().getId())) {
            throw new BadRequestException("Both tickets must belong to the same project.");
        }

        if (ticketDependencyRepository.existsByTicketIdAndBlockerId(ticket.getId(), blocker.getId())) {
            throw new BadRequestException("Ticket dependency already exists.");
        }
    }

    private Ticket findActiveTicketById(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket was not found."));
    }
}
