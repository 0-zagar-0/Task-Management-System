package task.system.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.TaskMapper;
import task.system.model.Project;
import task.system.model.Role;
import task.system.model.Task;
import task.system.model.User;
import task.system.repository.project.ProjectRepository;
import task.system.repository.task.TaskRepository;
import task.system.service.project.ProjectService;
import task.system.service.user.UserService;
import task.system.telegram.TaskSystemBot;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @InjectMocks
    private TaskServiceImpl taskService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private UserService userService;
    @Mock
    private ProjectService projectService;
    @Mock
    private TaskSystemBot taskSystemBot;
    @Mock
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Create task with valid data should return TaskFullDetailsDto")
    void create_WithValidRequestData_ShouldReturnTaskFullDetailsDto() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        TaskCreateRequestDto request = createRequest(1, project.getId(), 4L);
        Task task = createTask(1L, request);
        TaskFullDetailsDto expected = createTaskFullDetailsDto(task);

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(userService.existsById(request.getAssigneeId())).thenReturn(true);
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toFullDetailsDto(task)).thenReturn(expected);

        //Then
        TaskFullDetailsDto actual = taskService.create(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectService, times(3)).getById(project.getId());
        verify(userService, times(1)).existsById(request.getAssigneeId());
        verify(taskMapper, times(1)).toEntity(request);
        verify(taskRepository, times(1)).save(task);
        verify(taskMapper, times(1)).toFullDetailsDto(task);
    }

    @Test
    @DisplayName("Create task with non exists assignee Id, should throw an Exception")
    void create_WithNonExistentAssigneeId_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        TaskCreateRequestDto request = createRequest(1, project.getId(), 10L);

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(userService.existsById(request.getAssigneeId())).thenReturn(false);
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.create(request)
        );

        //Then
        String expected = "Can't find user by id: " + request.getAssigneeId();
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(request.getProjectId());
        verify(userService, times(1)).existsById(request.getAssigneeId());
    }

    @Test
    @DisplayName("Create task with assignee id does not belong to project, "
            + "should throw an Exception")
    void create_WithAssigneeIdNotBelongProject_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        TaskCreateRequestDto request = createRequest(1, project.getId(), 10L);

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(userService.existsById(request.getAssigneeId())).thenReturn(true);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.create(request)
        );

        //Then
        String expected = "The user with ID: " + request.getAssigneeId()
                + " cannot be assigned as assignee user "
                + "because it does not exist in the project with ID: " + project.getId()
                + ". Add this user to the project or choose another one.";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(request.getProjectId());
        verify(userService, times(1)).existsById(request.getAssigneeId());
    }

    @Test
    @DisplayName("Create task with non exists project id, should throw an Exception")
    void createTask_WithNonExistsProjectId_ShouldThrowException() {
        //Given
        Long projectId = 999L;
        TaskCreateRequestDto request = createRequest(1, projectId, 5L);
        String expected = "Can't find project by id: " + projectId;

        //When
        when(projectService.getById(projectId)).thenThrow(new EntityNotFoundException(expected));
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.create(request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(projectId);
    }

    @Test
    @DisplayName("Create task with dueDate before project startDate, should throw an Exception")
    void createTask_WithDueDateBeforeProjectStartDate_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        TaskCreateRequestDto request = createRequest(1, project.getId(), 5L);
        request.setDueDate(LocalDate.now().minusDays(2));

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(userService.existsById(request.getAssigneeId())).thenReturn(true);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.create(request)
        );

        //Then
        String expected = "The date is incorrect: " + request.getDueDate()
                + ", you entered a date before the start date of the project: "
                + projectDetails.getStartDate() + " or after the end date of the project: "
                + projectDetails.getEndDate();
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(2)).getById(request.getProjectId());
        verify(userService, times(1)).existsById(request.getAssigneeId());
    }

    @Test
    @DisplayName("Create task with dueDate after project endDate, should throw an Exception")
    void createTask_WithDueDateAfterProjectEndDate_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        TaskCreateRequestDto request = createRequest(1, project.getId(), 5L);
        request.setDueDate(LocalDate.now().plusMonths(10));

        //When
        when(projectService.getById(request.getProjectId())).thenReturn(projectDetails);
        when(userService.existsById(request.getAssigneeId())).thenReturn(true);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.create(request)
        );

        //Then
        String expected = "The date is incorrect: " + request.getDueDate()
                + ", you entered a date before the start date of the project: "
                + projectDetails.getStartDate() + " or after the end date of the project: "
                + projectDetails.getEndDate();
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(2)).getById(request.getProjectId());
        verify(userService, times(1)).existsById(request.getAssigneeId());
    }

    @Test
    @DisplayName("Get all tasks by valid project id should return all TaskLowDetailsDto")
    void getAllTasks_ByValidProjectId_ShouldReturnAllTasksDto() {
        //Given
        Long projectId = 1L;
        Project project = createProject(
                projectId, createUser(1L), createUsers(1, 6), createUsers(1, 3)
        );
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        List<Task> tasks = createTasks(projectId, 1, 3);

        //When
        when(projectService.getById(projectId)).thenReturn(projectDetails);
        when(taskRepository.findAllByProjectId(projectId)).thenReturn(tasks);

        for (Task task : tasks) {
            when(taskMapper.toLowDetailsDto(task)).thenReturn(createTaskLowDetailsDto(task));
        }

        //Then
        List<TaskLowDetailsDto> expected = createListTaskLowDetailsDto(tasks);
        List<TaskLowDetailsDto> actual = taskService.getAll(projectId);
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.get(1), actual.get(1));

        //Verify
        verify(projectService, times(1)).getById(projectId);
        verify(taskRepository, times(1)).findAllByProjectId(projectId);
        verify(taskMapper, times(tasks.size())).toLowDetailsDto(any(Task.class));
    }

    @Test
    @DisplayName("Get all by non exists project id, should throw an Exception")
    void getAll_ByNonExistentProjectId_ShouldThrowException() {
        //Given
        Long projectId = 999L;
        String expected = "Can't find project by id: " + projectId;

        //When
        when(projectService.getById(projectId)).thenThrow(new EntityNotFoundException(expected));
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.getAll(projectId)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectService, times(1)).getById(projectId);
    }

    @Test
    @DisplayName("Get by id with valid task id, should return TaskFullDetailsDto")
    void getById_ByValidTaskId_ShouldReturnTaskFullDetailsDto() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        Task task = createTask(1L, project.getId(), 5L);
        TaskFullDetailsDto expected = createTaskFullDetailsDto(task);

        //When
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectService.getById(task.getProjectId())).thenReturn(projectDetails);
        when(taskMapper.toFullDetailsDto(task)).thenReturn(expected);

        //Then
        TaskFullDetailsDto actual = taskService.getById(task.getId());
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(task.getId());
        verify(projectService, times(1)).getById(task.getProjectId());
        verify(taskMapper, times(1)).toFullDetailsDto(task);
    }

    @Test
    @DisplayName("Get by id with non exists task id, should throw an Exception")
    void getById_ByNonExistentTaskId_ShouldThrowException() {
        //Given
        Long taskId = 999L;

        //When
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.getById(taskId)
        );

        //Then
        String expected = "Can't find task by id: " + taskId;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("Update by valid id and valid request data, "
            + "should return updated TaskFullDetailsDto")
    void updateByValidId_WithValidRequestData_ShouldReturnUpdatedTaskFullDetailsDto() {
        //Given
        Long taskId = 1L;
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        Task task = createTask(taskId, project.getId(), 5L);
        Long assigneeId = 4L;
        TaskUpdateRequestDto request = createTaskUpdateRequest(assigneeId);
        Task updatedTask = createTask(task, request);
        TaskFullDetailsDto expected = createTaskFullDetailsDto(updatedTask);
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        User user = createUser(2L);

        //When
        when(taskMapper.toFullDetailsDto(updatedTask)).thenReturn(expected);
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userService.existsById(assigneeId)).thenReturn(true);
        when(taskRepository.update(any(Task.class))).thenReturn(updatedTask);

        //Then
        TaskFullDetailsDto actual = taskService.updateById(taskId, request);
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
        verify(projectService, times(5)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
        verify(userService, times(1)).existsById(assigneeId);
        verify(taskRepository, times(1)).update(any(Task.class));
        verify(taskMapper, times(1)).toFullDetailsDto(updatedTask);
    }

    @Test
    @DisplayName("Update task with non exists task id, should throw an Exception")
    void update_ByNonExistsTaskId_ShouldThrowException() {
        //Given
        Long taskId = 999L;
        TaskUpdateRequestDto request = createTaskUpdateRequest(4L);

        //When
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateById(taskId, request)
        );

        //Then
        String expected = "Can't find task by id: " + taskId;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("Update task with authenticated user not admin, should throw an Exception")
    void update_ByAuthenticatedUserDoesNotAdmin_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        Long taskId = 1L;
        Task task = createTask(taskId, project.getId(), 5L);
        User authenticatedUser = createUser(5L);
        TaskUpdateRequestDto request = createTaskUpdateRequest(4L);

        //WHen
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.updateById(taskId, request)
        );

        //Then
        String expected = "Only administrators have access to the update, the user with ID: "
                + authenticatedUser.getId() + " is not an administrator";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
        verify(projectService, times(2)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Update task with non exists assigneeId, should throw an Exception")
    void update_ByNonExistsAssigneeId_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        Long assigneeId = 999L;
        User authenticatedUser = createUser(2L);
        TaskUpdateRequestDto request = createTaskUpdateRequest(assigneeId);
        Task task = createTask(1L, project.getId(), 5L);
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);

        //When
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(userService.existsById(assigneeId)).thenReturn(false);

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateById(task.getId(), request)
        );

        //Then
        String expected = "Can't find user by id: " + assigneeId;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(task.getId());
        verify(projectService, times(4)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
        verify(userService, times(1)).existsById(assigneeId);
    }

    @Test
    @DisplayName("Update task by assignee Id does not belong to project, should throw an Exception")
    void update_ByAssigneeIdNotBelongProject_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        User authenticatedUser = createUser(2L);
        Long assigneeId = 9L;
        TaskUpdateRequestDto request = createTaskUpdateRequest(assigneeId);
        Task task = createTask(1L, project.getId(), 5L);

        //When
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(userService.existsById(assigneeId)).thenReturn(true);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.updateById(task.getId(), request)
        );

        //Then
        String expected = "The user with ID: " + assigneeId
                + " cannot be assigned as assignee user "
                + "because it does not exist in the project with ID: " + project.getId()
                + ". Add this user to the project or choose another one.";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(task.getId());
        verify(projectService, times(4)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
        verify(userService, times(1)).existsById(assigneeId);
    }

    @Test
    @DisplayName("Update task with invalid dueDate, should throw an Exception")
    void update_ByInvalidDueDate_ShouldThrowException() {
        //Given
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        TaskUpdateRequestDto request = createTaskUpdateRequest(4L);
        request.setDueDate(LocalDate.now().minusDays(5));
        Task task = createTask(1L, project.getId(), 5L);
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        User authenticatedUser = createUser(2L);

        //When
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.updateById(task.getId(), request)
        );

        //Then
        String expected = "The date is incorrect: " + request.getDueDate()
                + ", you entered a date before the start date of the project: "
                + projectDetails.getStartDate() + " or after the end date of the project: "
                + projectDetails.getEndDate();
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(task.getId());
        verify(projectService, times(3)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Delete by id with valid id, should delete Task")
    void delete_ByValidId_ShouldDeleteTask() {
        //Given
        Long taskId = 1L;
        Task task = createTask(taskId, 1L, 4L);
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        User authenticatedUser = createUser(2L);

        //When
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);

        //Then
        taskService.deleteById(taskId);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
        verify(projectService, times(2)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    @DisplayName("Delete by id with non exists task Id, should throw an Exception")
    void delete_ByNonExistsTask_ShouldThrowException() {
        //Given
        Long taskId = 999L;

        //When
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.deleteById(taskId)
        );

        //Then
        String expected = "Can't find task by id: " + taskId;
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("Delete by id with authenticated user not admin, should throw an Exception")
    void delete_ByAuthenticatedUserNotAdmin_ShouldThrowException() {
        //Given
        Long taskId = 1L;
        Task task = createTask(taskId, 1L, 4L);
        Project project = createProject(1L, createUser(1L), createUsers(1, 6), createUsers(1, 3));
        ProjectDetailsResponseDto projectDetails = createProjectDetailsResponseDto(project);
        User authenticatedUser = createUser(5L);

        //When
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectService.getById(project.getId())).thenReturn(projectDetails);
        when(userService.getAuthenticatedUser()).thenReturn(authenticatedUser);

        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> taskService.deleteById(taskId)
        );

        //Then
        String expected = "Only administrators have access to the update, the user with ID: "
                + authenticatedUser.getId() + " is not an administrator";
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(taskRepository, times(1)).findById(taskId);
        verify(projectService, times(2)).getById(project.getId());
        verify(userService, times(1)).getAuthenticatedUser();
    }

    private TaskUpdateRequestDto createTaskUpdateRequest(Long assigneeId) {
        TaskUpdateRequestDto taskUpdateRequestDto = new TaskUpdateRequestDto();
        taskUpdateRequestDto.setName("updated task name");
        taskUpdateRequestDto.setDescription("updated task description");
        taskUpdateRequestDto.setPriority(Task.Priority.HIGH);
        taskUpdateRequestDto.setStatus(Task.Status.COMPLETED);
        taskUpdateRequestDto.setAssigneeId(assigneeId);
        taskUpdateRequestDto.setDueDate(LocalDate.now().plusMonths(1));
        return taskUpdateRequestDto;
    }

    private List<TaskLowDetailsDto> createListTaskLowDetailsDto(List<Task> tasks) {
        List<TaskLowDetailsDto> tasksLowDetails = new ArrayList<>();

        for (Task task : tasks) {
            TaskLowDetailsDto taskLowDetailsDto = createTaskLowDetailsDto(task);
            tasksLowDetails.add(taskLowDetailsDto);
        }

        return tasksLowDetails;
    }

    private List<Task> createTasks(Long projectId, int startIndex, int endIndex) {
        List<Task> tasks = new ArrayList<>();

        for (int i = startIndex; i <= endIndex; i++) {
            Task task = createTask((long) i, projectId, (long) i);
            tasks.add(task);
        }

        return tasks;
    }

    private TaskLowDetailsDto createTaskLowDetailsDto(Task task) {
        TaskLowDetailsDto taskLowDetailsDto = new TaskLowDetailsDto();
        taskLowDetailsDto.setId(task.getId());
        taskLowDetailsDto.setName(task.getName());
        taskLowDetailsDto.setDescription(task.getDescription());
        return taskLowDetailsDto;
    }

    private TaskFullDetailsDto createTaskFullDetailsDto(Task task) {
        TaskFullDetailsDto taskFullDetailsDto = new TaskFullDetailsDto();
        taskFullDetailsDto.setId(task.getId());
        taskFullDetailsDto.setDescription(task.getDescription());
        taskFullDetailsDto.setName(task.getName());
        taskFullDetailsDto.setStatus(task.getStatus());
        taskFullDetailsDto.setProjectId(task.getProjectId());
        taskFullDetailsDto.setDueDate(task.getDueDate());
        taskFullDetailsDto.setPriority(task.getPriority());
        taskFullDetailsDto.setAssigneeId(task.getAssigneeId());
        return taskFullDetailsDto;
    }

    private Task createTask(Long id, Long projectId, Long assigneeId) {
        Task task = new Task();
        task.setId(id);
        task.setName("task" + id);
        task.setDescription("description" + id);
        task.setPriority(Task.Priority.MEDIUM);
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setDueDate(LocalDate.now().plusWeeks(2));
        task.setProjectId(projectId);
        task.setAssigneeId(assigneeId);
        return task;
    }

    private Task createTask(Long id, TaskCreateRequestDto request) {
        Task task = new Task();
        task.setId(id);
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setProjectId(request.getProjectId());
        task.setAssigneeId(request.getAssigneeId());
        task.setDeleted(false);
        return task;
    }

    private Task createTask(Task task, TaskUpdateRequestDto request) {
        Task updateTask = new Task();
        updateTask.setId(task.getId());
        updateTask.setName(request.getName() == null ? task.getName() : request.getName());
        updateTask.setDescription(request.getDescription() == null
                ? task.getDescription() : request.getDescription());
        updateTask.setPriority(updateTask.getPriority() == null
                ? task.getPriority() : updateTask.getPriority());
        updateTask.setStatus(request.getStatus() == null ? task.getStatus() : request.getStatus());
        updateTask.setDueDate(request.getDueDate() == null
                ? task.getDueDate() : request.getDueDate());
        updateTask.setAssigneeId(request.getAssigneeId() == null
                ? task.getAssigneeId() : request.getAssigneeId());
        updateTask.setProjectId(task.getProjectId());
        return updateTask;
    }

    private TaskCreateRequestDto createRequest(int index, Long projectId, Long assigneeId) {
        TaskCreateRequestDto request = new TaskCreateRequestDto();
        request.setName("task" + index);
        request.setDescription("task" + index);
        request.setDueDate(LocalDate.now().plusDays(50));
        request.setProjectId(projectId);
        request.setAssigneeId(assigneeId);
        request.setPriority(Task.Priority.MEDIUM);
        request.setStatus(Task.Status.IN_PROGRESS);
        return request;
    }

    private Project createProject(Long id, User mainUser, Set<User> users, Set<User> admins) {
        Project project = new Project();
        project.setId(id);
        project.setName("project" + id);
        project.setDescription("description" + id);
        project.setStatus(Project.Status.IN_PROGRESS);
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusDays(90));
        project.setMainUser(mainUser);
        project.setUsers(users);
        project.setAdministrators(admins);
        project.setDeleted(false);
        return project;
    }

    private ProjectDetailsResponseDto createProjectDetailsResponseDto(Project project) {
        ProjectDetailsResponseDto response = new ProjectDetailsResponseDto();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setStartDate(project.getStartDate());
        response.setEndDate(project.getEndDate());
        response.setMainUser(project.getMainUser().getId());
        response.setStatus(project.getStatus());
        response.setUserIds(project.getUsers().stream()
                .map(User::getId)
                .collect(Collectors.toSet()));
        response.setAdministratorIds(project.getAdministrators().stream()
                .map(User::getId)
                .collect(Collectors.toSet()));
        return response;
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("username" + id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("user" + id);
        user.setLastName("user" + id);
        user.setPassword("User" + id + "=3215987");
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        user.setRoles(Set.of(role));
        return user;
    }

    private Set<User> createUsers(int startIndex, int endIndex) {
        Set<User> users = new HashSet<>();

        for (int i = startIndex; i <= endIndex; i++) {
            users.add(createUser((long) i));
        }

        return users;
    }
}
