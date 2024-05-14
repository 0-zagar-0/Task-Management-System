package task.system.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Task;

@Getter
@Setter
public class TaskCreateRequestDto {
    @NotNull
    @Size(min = 1, max = 30)
    private String name;

    @NotNull
    @Size(min = 1)
    private String description;

    @NotNull
    private Task.Priority priority;

    @NotNull
    private Task.Status status;

    @NotNull
    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @NotNull
    @Positive
    private Long projectId;

    @Positive
    private Long assigneeId;
}
