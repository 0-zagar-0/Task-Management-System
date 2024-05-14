package task.system.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.dto.comment.CommentUpdateRequestDto;
import task.system.service.comment.CommentService;

@RestController
@RequestMapping(value = "/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public CommentResponseDto create(@RequestBody @Valid CommentRequestDto request) {
        return commentService.create(request);
    }

    @GetMapping
    public List<CommentResponseDto> getAllById(@RequestParam(name = "taskId") Long taskId) {
        return commentService.getAllByTaskId(taskId);
    }

    @PutMapping(value = "/{id}")
    public CommentResponseDto updateById(@PathVariable Long id,
                                         @Valid @RequestBody CommentUpdateRequestDto request) {
        return commentService.updateById(id, request);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteById(@PathVariable Long id) {
        commentService.deleteById(id);
    }

}
