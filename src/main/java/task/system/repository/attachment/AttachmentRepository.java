package task.system.repository.attachment;

import java.util.List;
import java.util.Optional;
import task.system.model.Attachment;

public interface AttachmentRepository {
    Attachment save(Attachment attachment);

    Optional<Attachment> findByFileName(String filename);

    Optional<Attachment> findById(Long id);

    List<Attachment> findAllByTaskId(Long id);
}
