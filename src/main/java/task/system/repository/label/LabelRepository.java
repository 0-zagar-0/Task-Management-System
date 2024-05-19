package task.system.repository.label;

import java.util.List;
import java.util.Optional;
import task.system.model.Label;

public interface LabelRepository {
    Label save(Label label);

    Optional<Label> findByNameAndColorAndProjectId(Label.Color color, String name, Long projectId);

    List<Label> findAllByProjectId(Long projectId);

    Optional<Label> findById(Long id);

    Label update(Label labelById);

    void deleteById(Long id);

    List<Label> findDefaultLabels();

    Label findDefaultGreyLabel();
}
