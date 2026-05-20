package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.dependency.TicketDependencyService;
import com.att.tdp.issueflow.project.Project;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.project.WorkloadService;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final TicketDependencyService ticketDependencyService;
    private final WorkloadService workloadService;

    public TicketService(TicketRepository ticketRepository, ProjectService projectService, UserService userService, AuditLogService auditLogService, TicketDependencyService ticketDependencyService, WorkloadService workloadService) {
        this.ticketRepository = ticketRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.ticketDependencyService = ticketDependencyService;
        this.workloadService = workloadService;
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        return TicketResponse.from(findActiveTicketById(ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByProject(Long projectId) {
        projectService.findActiveProjectById(projectId);

        return ticketRepository.findByProjectIdAndDeletedFalse(projectId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getDeletedTicketsByProject(Long projectId) {
        projectService.findActiveProjectById(projectId);

        return ticketRepository.findByProjectIdAndDeletedTrue(projectId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Project project = projectService.findActiveProjectById(request.projectId());
        User assignee = resolveAssignee(request);

        Ticket ticket = new Ticket(
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.type(),
                project,
                assignee,
                request.dueDate()
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.recordCurrentUserAction("TICKET", savedTicket.getId(), "CREATE", "Ticket was created.");
        recordAutoAssignmentIfNeeded(request, savedTicket);

        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public void updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = findActiveTicketById(ticketId);

        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new BadRequestException("A DONE ticket cannot be updated.");
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Ticket title cannot be blank.");
            }
            ticket.setTitle(request.title());
        }

        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw new BadRequestException("Ticket description cannot be blank.");
            }
            ticket.setDescription(request.description());
        }

        if (request.status() != null) {
            validateForwardStatusChange(ticket.getStatus(), request.status());
            validateDoneTransition(ticket, request.status());
            ticket.setStatus(request.status());
        }

        if (request.priority() != null) {
            ticket.setPriority(request.priority());
            ticket.setOverdue(false);
            ticket.setLastAutoEscalatedAt(null);
        }

        if (request.type() != null) {
            ticket.setType(request.type());
        }

        if (request.assigneeId() != null) {
            ticket.setAssignee(userService.findUserById(request.assigneeId()));
        }

        if (request.dueDate() != null) {
            ticket.setDueDate(request.dueDate());
        }

        auditLogService.recordCurrentUserAction("TICKET", ticket.getId(), "UPDATE", "Ticket was updated.");
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = findActiveTicketById(ticketId);
        ticket.setDeleted(true);
        auditLogService.recordCurrentUserAction("TICKET", ticket.getId(), "DELETE", "Ticket was soft-deleted.");
    }

    @Transactional
    public void restoreTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket was not found."));

        ticket.setDeleted(false);
        auditLogService.recordCurrentUserAction("TICKET", ticket.getId(), "RESTORE", "Ticket was restored.");
    }

    @Transactional(readOnly = true)
    public Ticket findActiveTicketById(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket was not found."));
    }

    private void validateForwardStatusChange(TicketStatus currentStatus, TicketStatus requestedStatus) {
        if (requestedStatus.ordinal() < currentStatus.ordinal()) {
            throw new BadRequestException("Ticket status cannot move backward.");
        }
    }

    private void validateDoneTransition(Ticket ticket, TicketStatus requestedStatus) {
        if (requestedStatus == TicketStatus.DONE && ticketDependencyService.hasUnresolvedBlockers(ticket.getId())) {
            throw new BadRequestException("Ticket cannot move to DONE while it has unresolved blockers.");
        }
    }

    private User resolveAssignee(CreateTicketRequest request) {
        if (request.assigneeId() != null) {
            return userService.findUserById(request.assigneeId());
        }

        return workloadService.findLeastLoadedDeveloper(request.projectId()).orElse(null);
    }

    private void recordAutoAssignmentIfNeeded(CreateTicketRequest request, Ticket ticket) {
        if (request.assigneeId() == null && ticket.getAssignee() != null) {
            auditLogService.recordSystemAction(
                    "TICKET",
                    ticket.getId(),
                    "AUTO_ASSIGN",
                    "Ticket was automatically assigned to " + ticket.getAssignee().getUsername() + "."
            );
        }
    }
}
