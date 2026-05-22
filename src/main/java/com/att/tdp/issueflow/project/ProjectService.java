package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserService;
import com.att.tdp.issueflow.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, AuditLogService auditLogService, UserService userService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getActiveProjects() {
        return projectRepository.findByDeletedFalse()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getDeletedProjects() {
        return projectRepository.findByDeletedTrue()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        return ProjectResponse.from(findActiveProjectById(projectId));
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User owner = userService.findUserById(request.ownerId());
        Project project = new Project(request.name(), request.description(), owner);
        project.setDevelopers(resolveProjectDevelopers(owner, request.developerIds()));

        Project savedProject = projectRepository.save(project);
        auditLogService.recordCurrentUserAction("PROJECT", savedProject.getId(), "CREATE", "Project was created.");

        return ProjectResponse.from(savedProject);
    }

    @Transactional
    public void updateProject(Long projectId, UpdateProjectRequest request) {
        Project project = findActiveProjectById(projectId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Project name cannot be blank.");
            }
            project.setName(request.name());
        }

        if (request.description() != null) {
            project.setDescription(request.description());
        }

        if (request.developerIds() != null) {
            project.setDevelopers(resolveProjectDevelopers(project.getOwner(), request.developerIds()));
        }

        auditLogService.recordCurrentUserAction("PROJECT", project.getId(), "UPDATE", "Project was updated.");
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = findActiveProjectById(projectId);
        project.setDeleted(true);
        auditLogService.recordCurrentUserAction("PROJECT", project.getId(), "DELETE", "Project was soft-deleted.");
    }

    @Transactional
    public void restoreProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project was not found."));

        project.setDeleted(false);
        auditLogService.recordCurrentUserAction("PROJECT", project.getId(), "RESTORE", "Project was restored.");
    }

    @Transactional(readOnly = true)
    public Project findActiveProjectById(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
    }

    private Set<User> resolveProjectDevelopers(User owner, Set<Long> developerIds) {
        Set<User> developers = new HashSet<>();

        if (owner != null && owner.getRole() == UserRole.DEVELOPER) {
            developers.add(owner);
        }

        if (developerIds == null) {
            return developers;
        }

        for (Long developerId : developerIds) {
            User developer = userService.findUserById(developerId);

            if (developer.getRole() != UserRole.DEVELOPER) {
                throw new BadRequestException("Project members must have DEVELOPER role.");
            }

            developers.add(developer);
        }

        return developers;
    }
}
