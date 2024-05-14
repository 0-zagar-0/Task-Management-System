package task.system.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Task;

@Getter
@Setter
public class TaskUpdateRequestDto {
    private String name;

    private String description;

    private Task.Priority priority;

    private Task.Status status;

    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @Positive
    private Long assigneeId;
}
