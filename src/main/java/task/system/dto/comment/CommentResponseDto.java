package task.system.dto.comment;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponseDto {
    private Long id;
    private Long taskId;
    private Long userId;
    private String text;
    private LocalDateTime timestamp;
}
