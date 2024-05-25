package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.dto.comment.CommentUpdateRequestDto;
import task.system.service.comment.CommentService;

@Tag(name = "Comment management", description = "Endpoints for comments action")
@RestController
@RequestMapping(value = "/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Create", description = "Create comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto create(@RequestBody @Valid CommentRequestDto request) {
        return commentService.create(request);
    }

    @GetMapping
    @Operation(summary = "Get all", description = "Get all comments by task id")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponseDto> getAllById(@RequestParam(name = "taskId") Long taskId) {
        return commentService.getAllByTaskId(taskId);
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Update by id", description = "Update comment by id")
    @ResponseStatus(HttpStatus.OK)
    public CommentResponseDto updateById(@PathVariable Long id,
                                         @Valid @RequestBody CommentUpdateRequestDto request) {
        return commentService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete by id", description = "Delete comment by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        commentService.deleteById(id);
    }

}
