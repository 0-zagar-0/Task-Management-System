package task.system.dto.label;

import lombok.Getter;
import lombok.Setter;
import task.system.model.Label;

@Getter
@Setter
public class LabelResponseDto {
    private Long id;
    private String name;
    private Label.Color color;
    private Long projectId;
}
