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
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.service.task.TaskService;

@RestController
@RequestMapping(value = "/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public TaskFullDetailsDto create(@Valid @RequestBody TaskCreateRequestDto request) {
        return taskService.create(request);
    }

    @GetMapping(value = "/project/{projectId}")
    public List<TaskLowDetailsDto> getAllByProjectId(@PathVariable Long projectId) {
        return taskService.getAll(projectId);
    }

    @GetMapping(value = "/{id}")
    public TaskFullDetailsDto getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PutMapping(value = "/{id}")
    public TaskFullDetailsDto updateById(@PathVariable Long id,
                                         @Valid @RequestBody TaskUpdateRequestDto request) {
        return taskService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteById(@PathVariable Long id) {
        taskService.deleteById(id);
    }
}
