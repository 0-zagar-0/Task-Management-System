package task.system.service.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.dto.comment.CommentUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.CommentMapper;
import task.system.model.Comment;
import task.system.model.Task;
import task.system.model.User;
import task.system.repository.comment.CommentRepository;
import task.system.service.task.TaskService;
import task.system.service.user.UserService;
import task.system.telegram.TaskSystemBot;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskService taskService;
    private final UserService userService;
    private final TaskSystemBot taskSystemBot;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            CommentMapper commentMapper,
            TaskService taskService,
            UserService userService,
            TaskSystemBot taskSystemBot
    ) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.taskService = taskService;
        this.userService = userService;
        this.taskSystemBot = taskSystemBot;
    }

    @Override
    public CommentResponseDto create(CommentRequestDto request) {
        Task taskFromDb = taskService.findById(request.getTaskId());
        Comment comment = commentMapper.toEntity(request);
        comment.setUserId(userService.getAuthenticatedUser().getId());
        Comment savedComment = commentRepository.save(comment);
        String telegramMessage =
                "User with id: " + savedComment.getUserId() + ", left a message to task."
                + System.lineSeparator() + "Task: " + taskFromDb.getName()
                + System.lineSeparator() + "Message: " + comment.getText();
        taskSystemBot.sendMessage(telegramMessage, taskFromDb.getAssigneeId());
        return commentMapper.toDto(savedComment);
    }

    @Override
    public List<CommentResponseDto> getAllByTaskId(Long taskId) {
        taskService.findById(taskId);
        return commentRepository.findAllByTaskId(taskId).stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDto updateById(Long id, CommentUpdateRequestDto request) {
        Comment comment = getById(id);
        checkingUserAccess(comment.getUserId());
        comment.setText(request.getText());
        comment.setTimestamp(LocalDateTime.now());
        return commentMapper.toDto(commentRepository.update(comment));
    }

    @Override
    public Comment getById(Long id) {
        return commentRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find comment by id: " + id)
        );
    }

    @Override
    public void deleteById(Long id) {
        Comment comment = getById(id);
        checkingUserAccess(comment.getUserId());
        commentRepository.deleteById(id);
    }

    private void checkingUserAccess(Long userId) {
        User user = userService.getAuthenticatedUser();

        if (!user.getId().equals(userId)) {
            throw new DataProcessingException(
                    "It isn't your comment, you cannot update this comment"
            );
        }
    }
}
