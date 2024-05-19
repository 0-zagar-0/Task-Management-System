package task.system.dto.label;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Label;

@Getter
@Setter
public class LabelRequestDto {
    private String name;
    private Label.Color color;
    @NotNull
    private Long projectId;
}
