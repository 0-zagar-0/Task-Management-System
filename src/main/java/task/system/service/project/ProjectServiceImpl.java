package task.system.service.project;

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

@Service
public class ProjectServiceImpl implements ProjectService {
    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            UserService userService,
            ProjectRepository projectRepository
    ) {
        this.projectMapper = projectMapper;
        this.userService = userService;
        this.projectRepository = projectRepository;
    }

    @Override
    public ProjectDetailsResponseDto create(ProjectRequestDto request) {
        Project project = projectMapper.toEntity(request);
        Set<User> users = request.getUsers().stream()
                .map(userService::getById)
                .collect(Collectors.toSet());
        User user = userService.getAuthenticatedUser();
        project.setMainUser(user);
        users.add(user);
        project.getUsers().addAll(users);
        Project savedProject = projectRepository.save(project);
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
        checkingUserAccess(id, projectById);
        return projectMapper.toDto(projectById);
    }

    @Override
    public ProjectDetailsResponseDto updateById(Long id, ProjectUpdateRequestDto request) {
        Project project = findProjectById(id);
        checkingUserAccess(id, project);
        checkingMainUserAccess(project.getMainUser().getId());
        Optional.ofNullable(request.getName())
                .filter(name -> !name.equals(project.getName()))
                .ifPresent(project::setName);
        Optional.ofNullable(request.getDescription())
                .filter(desc -> !desc.equals(project.getDescription()))
                .ifPresent(project::setDescription);
        Optional.ofNullable(request.getStartDate())
                .filter(startDate -> !startDate.equals(project.getStartDate()))
                .ifPresent(project::setStartDate);
        Optional.ofNullable(request.getEndDate())
                .filter(endDate -> !endDate.equals(project.getEndDate()))
                .ifPresent(project::setEndDate);
        Optional.ofNullable(request.getStatus())
                .filter(status -> !status.equals(project.getStatus()))
                .ifPresent(project::setStatus);
        Optional.ofNullable(request.getUsers())
                .ifPresent(users -> {
                    project.getUsers().addAll(
                            users.stream()
                                    .map(userService::getById)
                                    .collect(Collectors.toSet()));
                });

        return projectMapper.toDto(projectRepository.update(project));
    }

    @Override
    public void deleteById(Long id) {
        Project project = findProjectById(id);
        checkingUserAccess(id, project);
        checkingMainUserAccess(project.getMainUser().getId());
        projectRepository.deleteById(id);
    }

    private void checkingUserAccess(Long id, Project project) {
        User user = userService.getAuthenticatedUser();

        if (project.getUsers().stream().noneMatch(us -> us.equals(user))) {
            throw new DataProcessingException("You can only get your projects! "
                    + "The project under this id: " + id + " is not yours.");
        }
    }

    private void checkingMainUserAccess(Long projectUserId) {
        if (!userService.getAuthenticatedUser().getId().equals(projectUserId)) {
            throw new DataProcessingException("You are not the main user of this project,"
                    + " you have no rights to update the project");
        }
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find project by id: " + id)
        );
    }
}
