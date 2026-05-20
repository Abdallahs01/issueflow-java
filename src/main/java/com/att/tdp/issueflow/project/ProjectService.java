package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository, AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
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
        Project project = new Project(request.name(), request.description());
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
}
