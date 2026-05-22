package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.project.dto.WorkloadResponse;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class WorkloadService {

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;

    public WorkloadService(ProjectRepository projectRepository, TicketRepository ticketRepository) {
        this.projectRepository = projectRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkloadResponse> getWorkload(Long projectId) {
        Project project = findActiveProject(projectId);

        return project.getDevelopers()
                .stream()
                .filter(user -> user.getRole() == UserRole.DEVELOPER)
                .map(user -> new WorkloadResponse(user.getId(), user.getUsername(), countOpenTickets(projectId, user.getId())))
                .sorted(Comparator.comparingLong(WorkloadResponse::openTicketCount).thenComparing(WorkloadResponse::userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findLeastLoadedDeveloper(Long projectId) {
        Project project = findActiveProject(projectId);

        return project.getDevelopers()
                .stream()
                .filter(user -> user.getRole() == UserRole.DEVELOPER)
                .min(Comparator.comparingLong((User user) -> countOpenTickets(projectId, user.getId())).thenComparing(User::getCreatedAt));
    }

    private long countOpenTickets(Long projectId, Long userId) {
        return ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(projectId, userId, TicketStatus.DONE);
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
    }
}
