package task.system.repository.project;

import java.util.List;
import java.util.Optional;
import task.system.model.Project;

public interface ProjectRepository {
    Project save(Project project);

    List<Project> findAllProjectsByUserId(Long id);

    Optional<Project> findById(Long id);

    Project update(Project project);

    void deleteById(Long id);
}
