package task.system.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;
import task.system.service.project.ProjectService;

@RestController
@RequestMapping(value = "/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectDetailsResponseDto create(@RequestBody @Valid ProjectRequestDto request) {
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectLowInfoResponse> getAllUserProjects() {
        return projectService.getAllUserProjects();
    }

    @GetMapping(value = "/{id}")
    public ProjectDetailsResponseDto getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping(value = "/{id}")
    public ProjectDetailsResponseDto updateById(
            @PathVariable Long id, @RequestBody ProjectUpdateRequestDto request
    ) {
        return projectService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteById(@PathVariable Long id) {
        projectService.deleteById(id);
    }

}
