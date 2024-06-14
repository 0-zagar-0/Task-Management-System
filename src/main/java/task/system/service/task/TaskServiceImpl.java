package task.system.service.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.TaskMapper;
import task.system.model.Task;
import task.system.model.User;
import task.system.repository.task.TaskRepository;
import task.system.service.project.ProjectService;
import task.system.service.user.UserService;
import task.system.telegram.TaskSystemBot;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final TaskSystemBot taskSystemBot;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            TaskMapper taskMapper,
            UserService userService,
            ProjectService projectService,
            TaskSystemBot taskSystemBot
    ) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.userService = userService;
        this.projectService = projectService;
        this.taskSystemBot = taskSystemBot;
    }

    @Override
    public TaskFullDetailsDto create(TaskCreateRequestDto request) {
        checkAssigneeUserId(request.getProjectId(), request.getAssigneeId());
        checkDueDate(request.getDueDate(), request.getProjectId());
        Task task = taskMapper.toEntity(request);
        generateTaskAssignmentMessageAndSend(task);
        return taskMapper.toFullDetailsDto(taskRepository.save(task));
    }

    @Override
    public List<TaskLowDetailsDto> getAll(Long projectId) {
        ProjectDetailsResponseDto projectDetails = projectService.getById(projectId);
        return taskRepository.findAllByProjectId(projectDetails.getId()).stream()
                .map(taskMapper::toLowDetailsDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskFullDetailsDto getById(Long id) {
        return taskMapper.toFullDetailsDto(findById(id));
    }

    @Override
    public TaskFullDetailsDto updateById(Long id, TaskUpdateRequestDto request) {
        Task taskFromDb = findById(id);
        checkingAdministratorAccess(taskFromDb.getProjectId());
        checkDueDate(request.getDueDate(), taskFromDb.getProjectId());
        Optional.ofNullable(request.getName())
                .filter(name -> !name.equals(taskFromDb.getName()))
                .ifPresent(taskFromDb::setName);
        Optional.ofNullable(request.getDescription())
                .filter(description -> !description.equals(taskFromDb.getDescription()))
                .ifPresent(taskFromDb::setDescription);
        Optional.ofNullable(request.getPriority())
                .filter(priority -> !priority.equals(taskFromDb.getPriority()))
                .ifPresent(taskFromDb::setPriority);
        Optional.ofNullable(request.getStatus())
                .filter(status -> !status.equals(taskFromDb.getStatus()))
                .ifPresent(taskFromDb::setStatus);
        Optional.ofNullable(request.getDueDate())
                .filter(duDate -> !duDate.equals(taskFromDb.getDueDate()))
                .ifPresent(taskFromDb::setDueDate);
        Optional.ofNullable(request.getAssigneeId())
                .filter(assignee -> !assignee.equals(taskFromDb.getAssigneeId()))
                .ifPresent(assignee -> {
                    checkAssigneeUserId(taskFromDb.getProjectId(), assignee);
                    taskFromDb.setAssigneeId(assignee);
                });
        Task updatedTask = taskRepository.update(taskFromDb);

        if (taskFromDb.getAssigneeId() == null && updatedTask.getAssigneeId() != null) {
            generateTaskAssignmentMessageAndSend(updatedTask);
        }

        if (updatedTask.getAssigneeId() != null) {
            ProjectDetailsResponseDto project = projectService.getById(updatedTask.getProjectId());
            String telegramMessage =
                    "The details of the task have been updated" + System.lineSeparator()
                            + "Task: " + updatedTask.getName() + System.lineSeparator()
                            + "Project: " + project.getName() + System.lineSeparator()
                            + "Please check for updates!";
            taskSystemBot.sendMessage(telegramMessage, updatedTask.getAssigneeId());
        }

        return taskMapper.toFullDetailsDto(updatedTask);
    }

    @Override
    public void deleteById(Long id) {
        Task taskFromDb = findById(id);
        checkingAdministratorAccess(taskFromDb.getProjectId());
        taskRepository.deleteById(id);
    }

    @Override
    public Task findById(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find task by id: " + id)
        );
        projectService.getById(task.getProjectId());
        return task;
    }

    private void generateTaskAssignmentMessageAndSend(Task task) {
        if (task.getAssigneeId() != null) {
            ProjectDetailsResponseDto project = projectService.getById(
                    task.getProjectId()
            );
            String telegramMessage =
                    "You are assigned a task " + System.lineSeparator()
                            + "Task: " + task.getName() + System.lineSeparator()
                            + "Description: " + task.getDescription() + System.lineSeparator()
                            + "Project: " + project.getName();
            taskSystemBot.sendMessage(telegramMessage, task.getAssigneeId());
        }
    }

    private void checkingAdministratorAccess(Long projectId) {
        ProjectDetailsResponseDto projectDetails = projectService.getById(projectId);
        User user = userService.getAuthenticatedUser();
        Set<Long> administratorIds = projectDetails.getAdministratorIds();

        if (administratorIds.stream().noneMatch(id -> id.equals(user.getId()))) {
            throw new DataProcessingException(
                    "Only administrators have access to the update, the user with ID: "
                            + user.getId() + " is not an administrator"
            );
        }
    }

    private void checkAssigneeUserId(Long projectId, Long assigneeId) {
        ProjectDetailsResponseDto projectDetails = projectService.getById(projectId);

        if (assigneeId != null && !userService.existsById(assigneeId)) {
            throw new EntityNotFoundException("Can't find user by id: " + assigneeId);
        }

        if (assigneeId != null
                && projectDetails.getUserIds().stream().noneMatch(id -> id.equals(assigneeId))) {
            throw new DataProcessingException(
                    "The user with ID: " + assigneeId + " cannot be assigned as assignee user "
                            + "because it does not exist in the project with ID: " + projectId
                            + ". Add this user to the project or choose another one.");
        }
    }

    private void checkDueDate(LocalDate dueDate, Long projectId) {
        ProjectDetailsResponseDto projectDetails = projectService.getById(projectId);

        if (dueDate.isAfter(projectDetails.getEndDate())
                || dueDate.isBefore(projectDetails.getStartDate())
        ) {
            throw new DataProcessingException("The date is incorrect: " + dueDate
                    + ", you entered a date before the start date of the project: "
                    + projectDetails.getStartDate() + " or after the end date of the project: "
                    + projectDetails.getEndDate());
        }
    }

}
