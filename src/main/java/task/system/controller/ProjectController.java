package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;
import task.system.service.project.ProjectService;

@Tag(name = "Project management", description = "Endpoints for projects action")
@RestController
@RequestMapping(value = "/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create", description = "Create project")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailsResponseDto create(@RequestBody @Valid ProjectRequestDto request) {
        return projectService.create(request);
    }

    @GetMapping
    @Operation(summary = "Get all", description = "Get all projects by user")
    @ResponseStatus(HttpStatus.OK)
    public List<ProjectLowInfoResponse> getAllUserProjects() {
        return projectService.getAllUserProjects();
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Get by id", description = "Get project by id")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailsResponseDto getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Update by id", description = "Update project by id")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailsResponseDto updateById(
            @PathVariable Long id, @RequestBody ProjectUpdateRequestDto request
    ) {
        return projectService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete by id", description = "Delete project by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        projectService.deleteById(id);
    }

}
