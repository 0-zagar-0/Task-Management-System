package task.system.dto.label;

import lombok.Getter;
import task.system.model.Label;

@Getter
public class LabelUpdateRequestDto {
    private String name;
    private Label.Color color;
}
