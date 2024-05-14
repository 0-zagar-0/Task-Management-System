package task.system.service.comment;

import java.util.List;
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.dto.comment.CommentUpdateRequestDto;
import task.system.model.Comment;

public interface CommentService {
    CommentResponseDto create(CommentRequestDto request);

    List<CommentResponseDto> getAllByTaskId(Long taskId);

    CommentResponseDto updateById(Long id, CommentUpdateRequestDto request);

    Comment getById(Long id);

    void deleteById(Long id);
}
