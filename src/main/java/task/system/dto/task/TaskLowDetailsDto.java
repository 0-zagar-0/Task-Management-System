package task.system.dto.task;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class TaskLowDetailsDto {
    private Long id;
    private String name;
    private String description;
}
