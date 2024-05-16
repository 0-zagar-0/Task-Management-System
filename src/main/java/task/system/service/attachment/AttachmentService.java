package task.system.service.attachment;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import task.system.model.Attachment;

public interface AttachmentService {
    Attachment upload(MultipartFile file, Long taskId);

    ResponseEntity<InputStreamResource> download(Long attachmentId);

    ResponseEntity<InputStreamResource> downloadAllByTaskId(Long id);
}
