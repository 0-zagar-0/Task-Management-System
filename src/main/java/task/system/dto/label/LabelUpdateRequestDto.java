package task.system.dto.label;

import lombok.Getter;
import lombok.Setter;
import task.system.model.Label;

@Getter
@Setter
public class LabelUpdateRequestDto {
    private String name;
    private Label.Color color;
}
