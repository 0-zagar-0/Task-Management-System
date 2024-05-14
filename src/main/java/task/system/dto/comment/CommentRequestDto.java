package task.system.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequestDto {
    @NotNull
    @Positive
    private Long taskId;

    @NotNull
    @Size(min = 1)
    private String text;
}
