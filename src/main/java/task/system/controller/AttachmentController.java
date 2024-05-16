package task.system.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import task.system.model.Attachment;
import task.system.service.attachment.AttachmentService;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public Attachment upload(
            @RequestParam(name = "file") MultipartFile file,
            @RequestParam(name = "taskId") Long taskId
    ) {
        return attachmentService.upload(file, taskId);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long attachmentId) {
        return attachmentService.download(attachmentId);
    }

    @GetMapping(value = "/task/{id}")
    public ResponseEntity<InputStreamResource> downloadAllByTaskId(@PathVariable Long id) {
        return attachmentService.downloadAllByTaskId(id);
    }

}
