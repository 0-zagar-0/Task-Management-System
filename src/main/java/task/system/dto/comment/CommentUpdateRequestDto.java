package task.system.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentUpdateRequestDto {
    @NotNull
    @Size(min = 1)
    private String text;
}
