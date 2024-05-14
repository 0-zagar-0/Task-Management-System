package task.system.service.task;

import java.util.List;
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.model.Task;

public interface TaskService {
    TaskFullDetailsDto create(TaskCreateRequestDto request);

    List<TaskLowDetailsDto> getAll(Long projectId);

    TaskFullDetailsDto getById(Long id);

    TaskFullDetailsDto updateById(Long id, TaskUpdateRequestDto request);

    void deleteById(Long id);

    Task findById(Long id);
}
