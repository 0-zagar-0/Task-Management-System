package task.system.service.attachment;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Attachment;
import task.system.model.Task;
import task.system.repository.attachment.AttachmentRepository;
import task.system.service.task.TaskService;

@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final DbxClientV2 dbxClient;
    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService;

    public AttachmentServiceImpl(
            DbxClientV2 dbxClient,
            AttachmentRepository attachmentRepository,
            TaskService taskService
    ) {
        this.dbxClient = dbxClient;
        this.attachmentRepository = attachmentRepository;
        this.taskService = taskService;
    }

    @Override
    public Attachment upload(MultipartFile file, Long taskId) {
        FileMetadata fileMetadata;
        taskService.findById(taskId);

        try {
            fileMetadata = dbxClient.files()
                    .uploadBuilder("/" + file.getOriginalFilename())
                    .uploadAndFinish(file.getInputStream());
        } catch (DbxException | IOException e) {
            throw new DataProcessingException("Can't upload file: " + file.getOriginalFilename()
                    + ", to Dropbox service", e);
        }

        Attachment attachment = createAttachment(fileMetadata, taskId);
        return attachmentRepository.save(attachment);
    }

    @Override
    public ResponseEntity<InputStreamResource> download(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(
                () -> new EntityNotFoundException("Can't find attachment by id: " + attachmentId)
        );
        String dropboxFileId = attachment.getDropboxFileId();
        try {
            InputStream inputStream = dbxClient.files().download(dropboxFileId).getInputStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                            + attachment.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (DbxException e) {
            throw new RuntimeException("Error downloading file from Dropbox ", e);
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadAllByTaskId(Long taskId) {
        Task task = taskService.findById(taskId);
        List<Attachment> allByTaskId = attachmentRepository.findAllByTaskId(taskId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Attachment attachment : allByTaskId) {
                String dropboxFileId = attachment.getDropboxFileId();
                zipOutputStream.putNextEntry(new ZipEntry(attachment.getFilename()));

                try (InputStream inputStream =
                             dbxClient.files().download(dropboxFileId).getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = inputStream.read(buffer)) > 0) {
                        zipOutputStream.write(buffer, 0, length);
                    }
                } catch (DbxException e) {
                    throw new RuntimeException(e);
                }

                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new DataProcessingException("Can't create and download Zip archive");
        }

        InputStreamResource resource = new InputStreamResource(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + task.getName() + "-attachments.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private Attachment createAttachment(FileMetadata fileMetadata, Long taskId) {
        Attachment attachment = new Attachment();
        attachment.setTaskId(taskId);
        attachment.setFilename(fileMetadata.getName());
        attachment.setUploadDate(convertToLocalDateTime(fileMetadata.getClientModified()));
        attachment.setDropboxFileId(fileMetadata.getId());
        return attachment;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
