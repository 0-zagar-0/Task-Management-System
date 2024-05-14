package task.system.repository.task;

import java.util.List;
import java.util.Optional;
import task.system.model.Task;

public interface TaskRepository {
    Task save(Task task);

    List<Task> findAllByProjectId(Long id);

    Optional<Task> findById(Long id);

    Task update(Task taskFromDb);

    void deleteById(Long id);
}
