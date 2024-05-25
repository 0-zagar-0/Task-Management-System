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
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.service.task.TaskService;

@Tag(name = "Task management", description = "Endpoints for tasks action")
@RestController
@RequestMapping(value = "/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create", description = "Create task")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskFullDetailsDto create(@Valid @RequestBody TaskCreateRequestDto request) {
        return taskService.create(request);
    }

    @GetMapping(value = "/project/{projectId}")
    @Operation(summary = "Get all", description = "Get all tasks by project id")
    @ResponseStatus(HttpStatus.OK)
    public List<TaskLowDetailsDto> getAllByProjectId(@PathVariable Long projectId) {
        return taskService.getAll(projectId);
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Get by id", description = "Get task by id")
    @ResponseStatus(HttpStatus.OK)
    public TaskFullDetailsDto getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Update by id", description = "Update task by id")
    @ResponseStatus(HttpStatus.OK)
    public TaskFullDetailsDto updateById(@PathVariable Long id,
                                         @Valid @RequestBody TaskUpdateRequestDto request) {
        return taskService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete by id", description = "Delete task by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        taskService.deleteById(id);
    }
}
