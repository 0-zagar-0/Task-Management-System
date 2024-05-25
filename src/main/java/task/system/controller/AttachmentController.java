package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import task.system.model.Attachment;
import task.system.service.attachment.AttachmentService;

@Tag(name = "Attachment management", description = "Endpoints for attachments action")
@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    @Operation(summary = "Upload file", description = "Upload file to Dropbox")
    @ResponseStatus(HttpStatus.CREATED)
    public Attachment upload(
            @RequestParam(name = "file") MultipartFile file,
            @RequestParam(name = "taskId") Long taskId
    ) {
        return attachmentService.upload(file, taskId);
    }

    @GetMapping("/{attachmentId}")
    @Operation(summary = "Download file", description = "Download file from Dropbox")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<InputStreamResource> download(@PathVariable Long attachmentId) {
        return attachmentService.download(attachmentId);
    }

    @GetMapping(value = "/task/{id}")
    @Operation(summary = "Download all file",
            description = "Download all file from Dropbox from task"
    )
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<InputStreamResource> downloadAllByTaskId(@PathVariable Long id) {
        return attachmentService.downloadAllByTaskId(id);
    }

}
