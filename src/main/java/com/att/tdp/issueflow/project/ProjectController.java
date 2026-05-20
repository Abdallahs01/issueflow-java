package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.project.dto.WorkloadResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final WorkloadService workloadService;

    public ProjectController(ProjectService projectService, WorkloadService workloadService) {
        this.projectService = projectService;
        this.workloadService = workloadService;
    }

    @GetMapping
    public List<ProjectResponse> getActiveProjects() {
        return projectService.getActiveProjects();
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectResponse> getDeletedProjects() {
        return projectService.getDeletedProjects();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProjectById(@PathVariable Long projectId) {
        return projectService.getProjectById(projectId);
    }

    @GetMapping("/{projectId}/workload")
    public List<WorkloadResponse> getProjectWorkload(@PathVariable Long projectId) {
        return workloadService.getWorkload(projectId);
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @PostMapping("/update/{projectId}")
    public void updateProject(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
        projectService.updateProject(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
    }

    @PostMapping("/{projectId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public void restoreProject(@PathVariable Long projectId) {
        projectService.restoreProject(projectId);
    }
}
