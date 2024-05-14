package task.system.dto.task;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Task;

@Getter
@Setter
public class TaskFullDetailsDto {
    private Long id;
    private String name;
    private String description;
    private Task.Priority priority;
    private Task.Status status;
    private LocalDate dueDate;
    private Long projectId;
    private Long assigneeId;
}
