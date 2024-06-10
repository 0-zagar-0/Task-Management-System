package task.system.service.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.ProjectMapper;
import task.system.model.Project;
import task.system.model.User;
import task.system.repository.project.ProjectRepository;
import task.system.service.user.UserService;
import task.system.telegram.TaskSystemBot;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final String ACCESS_USER = "user";
    private static final String ACCESS_ADMINISTRATOR = "administrator";

    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final TaskSystemBot taskSystemBot;

    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            UserService userService,
            ProjectRepository projectRepository, TaskSystemBot taskSystemBot) {
        this.projectMapper = projectMapper;
        this.userService = userService;
        this.projectRepository = projectRepository;
        this.taskSystemBot = taskSystemBot;
    }

    @Override
    public ProjectDetailsResponseDto create(ProjectRequestDto request) {
        getUsersFromDbFromRequest(request.getUsers());
        getUsersFromDbFromRequest(request.getAdministrators());
        checkValidDate(request.getStartDate(), request.getEndDate());

        Project project = projectMapper.toEntity(request);
        User user = userService.getAuthenticatedUser();
        project.setMainUser(user);

        Set<User> administratorsFromRequest =
                getUsersFromDbFromRequest(request.getAdministrators());
        Set<User> administrators = project.getAdministrators();
        administrators.add(user);
        administrators.addAll(administratorsFromRequest);
        Set<User> usersFromRequest = getUsersFromDbFromRequest(request.getUsers());
        Set<User> users = project.getUsers();
        users.addAll(administrators);
        users.addAll(usersFromRequest);

        Project savedProject = projectRepository.save(project);
        String telegramMessage = "You have been added to the project participants."
                + System.lineSeparator() + "Project: " + savedProject.getName();
        taskSystemBot.sendMessage(telegramMessage, savedProject.getUsers());
        return projectMapper.toDto(savedProject);
    }

    @Override
    public List<ProjectLowInfoResponse> getAllUserProjects() {
        Long id = userService.getAuthenticatedUser().getId();
        return projectRepository.findAllProjectsByUserId(id)
                .stream().map(projectMapper::toLowInfoDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDetailsResponseDto getById(Long id) {
        Project projectById = findProjectById(id);
        checkingUserAccess(ACCESS_USER, id, projectById.getUsers());
        return projectMapper.toDto(projectById);
    }

    @Override
    public ProjectDetailsResponseDto updateById(Long id, ProjectUpdateRequestDto request) {
        Project project = findProjectById(id);

        checkingUserAccess(ACCESS_USER, id, project.getUsers());
        checkingUserAccess(ACCESS_ADMINISTRATOR, id, project.getAdministrators());

        Optional.ofNullable(request.getName())
                .filter(name -> !name.equals(project.getName()))
                .ifPresent(project::setName);
        Optional.ofNullable(request.getDescription())
                .filter(desc -> !desc.equals(project.getDescription()))
                .ifPresent(project::setDescription);
        Optional.ofNullable(request.getStartDate())
                .filter(startDate -> !startDate.equals(project.getStartDate())
                        && request.getEndDate() == null
                        && checkValidDate(startDate, project.getEndDate())
                )
                .ifPresent(project::setStartDate);
        Optional.ofNullable(request.getStartDate())
                .filter(startDate -> !startDate.equals(project.getStartDate())
                        && request.getEndDate() != null
                        && checkValidDate(startDate, request.getEndDate())
                )
                .ifPresent(project::setStartDate);
        Optional.ofNullable(request.getEndDate())
                .filter(endDate -> !endDate.equals(project.getEndDate())
                        && checkValidDate(project.getStartDate(), endDate))
                .ifPresent(project::setEndDate);
        Optional.ofNullable(request.getStatus())
                .filter(status -> !status.equals(project.getStatus()))
                .ifPresent(project::setStatus);
        Optional.ofNullable(request.getAdministrators())
                .ifPresent(admins -> {
                    Set<User> usersFromDbFromRequest = getUsersFromDbFromRequest(admins);
                    project.getAdministrators().addAll(usersFromDbFromRequest);
                    project.getUsers().addAll(usersFromDbFromRequest);
                });
        Optional.ofNullable(request.getUsers())
                .ifPresent(users -> {
                    Set<User> usersFromDbFromRequest = getUsersFromDbFromRequest(users);
                    project.getUsers().addAll(usersFromDbFromRequest);
                });

        Project projectFromDb = findProjectById(id);
        Project updatedProject = projectRepository.update(project);
        ProjectDetailsResponseDto responseDto = projectMapper.toDto(updatedProject);
        createAndSendMessageToTelegramNewUser(projectFromDb, updatedProject);
        String updateMessage = "The details of the project have been updated"
                + System.lineSeparator() + "Project: " + project.getName();
        taskSystemBot.sendMessage(updateMessage, projectFromDb.getUsers());
        return responseDto;
    }

    @Override
    public void deleteById(Long id) {
        Project project = findProjectById(id);
        checkingUserAccess(ACCESS_USER, id, project.getUsers());

        if (!project.getMainUser().getId().equals(userService.getAuthenticatedUser().getId())) {
            throw new DataProcessingException(
                    "You cannot delete this project, only the main user can do that"
            );
        }

        String deleteMessage = "The project has been deleted" + System.lineSeparator()
                + "Project: " + project.getName();
        taskSystemBot.sendMessage(deleteMessage, project.getUsers());
        projectRepository.deleteById(id);
    }

    private Set<User> getUsersFromDbFromRequest(Set<Long> userIds) {
        return userIds.stream()
                .map(userService::getById)
                .collect(Collectors.toSet());
    }

    private void checkingUserAccess(String mainUserOrUser, Long id, Set<User> users) {
        User user = userService.getAuthenticatedUser();
        String message;

        if (users.stream().noneMatch(us -> us.equals(user))) {
            if (mainUserOrUser.equals(ACCESS_ADMINISTRATOR)) {
                message = "You are not the administrator of this project,"
                        + " you have no rights to update the project";
            } else {
                message = "You can only get your projects! " + "The project under this id: "
                        + id + " is not yours.";
            }

            throw new DataProcessingException(message);
        }
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find project by id: " + id)
        );
    }

    private void createAndSendMessageToTelegramNewUser(Project project, Project updatedProject) {
        Set<User> usersProject = project.getUsers();
        Set<User> usersUpdatedProjects = updatedProject.getUsers();
        String telegramMessage = "You have been added to the project participants."
                + System.lineSeparator() + "Project: " + updatedProject.getName();

        if (usersProject.isEmpty() && !usersUpdatedProjects.isEmpty()) {
            taskSystemBot.sendMessage(telegramMessage, usersUpdatedProjects);
        }

        if (!usersProject.isEmpty()) {
            usersUpdatedProjects.removeAll(usersProject);

            if (!usersUpdatedProjects.isEmpty()) {
                taskSystemBot.sendMessage(telegramMessage, usersUpdatedProjects);
            }
        }
    }

    private boolean checkValidDate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate) || startDate.isAfter(endDate)) {
            throw new DataProcessingException("Date incorrect please entry valid date");
        }

        return true;
    }
}
