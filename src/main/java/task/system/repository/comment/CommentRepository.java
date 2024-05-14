package task.system.repository.comment;

import java.util.List;
import java.util.Optional;
import task.system.model.Comment;

public interface CommentRepository {
    Comment save(Comment comment);

    List<Comment> findAllByTaskId(Long taskId);

    Optional<Comment> findById(Long id);

    Comment update(Comment comment);

    void deleteById(Long id);
}
