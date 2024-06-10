package task.system.service.project;

import static java.util.Collections.EMPTY_SET;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.mapper.ProjectMapper;
import task.system.model.Project;
import task.system.model.Role;
import task.system.model.User;
import task.system.repository.project.ProjectRepository;
import task.system.service.user.UserService;
import task.system.telegram.TaskSystemBot;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @InjectMocks
    private ProjectServiceImpl projectService;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private UserService userService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskSystemBot taskSystemBot;

    @Test
    @DisplayName("Create with valid data, should return ProjectDetailsResponseDto")
    void create_WithValidData_ShouldReturnProjectDetailsResponseDto() {
        //Given
        User user = createUser(1);
        Set<User> users = createUsers(1, 6);
        Set<User> admins = createUsers(1, 3);
        ProjectRequestDto request = createRequest(users, admins);
        Project project = createProject(user, request);
        ProjectDetailsResponseDto expected = createProjectDetailsResponseDto(project);

        //When
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(expected);

        for (long i = 1L; i <= 3L; i++) {
            Long id = i;
            when(userService.getById(i)).thenReturn(
                    admins.stream()
                            .filter(u -> u.getId().equals(id))
                            .findFirst().get()
            );
        }

        for (long i = 1L; i <= 6L; i++) {
            Long id = i;
            when(userService.getById(i)).thenReturn(
                    users.stream()
                            .filter(u -> u.getId().equals(id))
                            .findFirst().get()
            );
        }

        //Then
        ProjectDetailsResponseDto actual = projectService.create(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(userService, times(18)).getById(anyLong());
        verify(userService, times(1)).getAuthenticatedUser();
        verify(projectMapper, times(1)).toEntity(request);
        verify(projectRepository, times(1)).save(project);
        verify(projectMapper, times(1)).toDto(project);
    }

    @Test
    @DisplayName("Create with a user id that does not exist should throw an Exception")
    void create_WithInvalidUserId_ShouldThrowsException() {
        //Given
        User user = createUser(1);
        Set<User> users = createUsers(1, 6);
        Set<User> admins = createUsers(1, 3);
        ProjectRequestDto request = createRequest(users, admins);
        String expected = "Can't find user by id: " + 6;

        //When
        for (long i = 1L; i <= 5L; i++) {
            Long id = i;
            when(userService.getById(id)).thenReturn(
                    users.stream()
                            .filter(u -> u.getId().equals(id))
                            .findFirst().get()
            );
        }
        when(userService.getById(6L)).thenThrow(
                new EntityNotFoundException(expected)
        );

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.create(request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(userService, times(6)).getById(anyLong());
    }

    @Test
    @DisplayName("Create with unauthenticated user, should throw an Exception")
    void create_WithUnauthenticatedUser_ShouldThrowsException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        ProjectRequestDto request = createRequest(EMPTY_SET, EMPTY_SET);
        String expected = "Unable to find authenticated user";

        //When
        when(userService.getAuthenticatedUser()).thenThrow(
                new DataProcessingException(expected)
        );
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.create(request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Get all with authenticated user should return all ProjectDetailsResponseDto")
    void getAll_WithAuthenticatedUser_ShouldReturnAllProjectDetailsResponseDto() {
        //Given
        User user = createUser(2);
        Project project1 = createProject(1L, user, "project1", "description1");
        Project project2 = createProject(2L, user, "project2", "description2");
        List<Project> projects = List.of(project1, project2);
        ProjectLowInfoResponse response1 = createProjectLowInfoResponse(project1);
        ProjectLowInfoResponse response2 = createProjectLowInfoResponse(project2);

        //When
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(projectRepository.findAllProjectsByUserId(user.getId())).thenReturn(projects);
        when(projectMapper.toLowInfoDto(project1)).thenReturn(response1);
        when(projectMapper.toLowInfoDto(project2)).thenReturn(response2);

        //Then
        List<ProjectLowInfoResponse> expected = List.of(response1, response2);

        List<ProjectLowInfoResponse> actual = projectService.getAllUserProjects();
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(1), actual.get(1)));

        //Verify
        verify(userService, times(1)).getAuthenticatedUser();
        verify(projectRepository, times(1))
                .findAllProjectsByUserId(user.getId());
        verify(projectMapper, times(2)).toLowInfoDto(any(Project.class));
    }

    @Test
    @DisplayName("Get all with unauthenticated user, should throw an Exception")
    void getAll_WithUnauthenticatedUser_ShouldThrowsException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        String expected = "Unable to find authenticated user";

        //When
        when(userService.getAuthenticatedUser()).thenThrow(
                new DataProcessingException(expected)
        );
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.getAllUserProjects()
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Get by id with valid id should return ProjectDetailsResponseDto")
    void getById_WithValidId_ShouldReturnProjectDetailsResponseDto() {
        //Given
        User user = createUser(2);
        Long id = 2L;
        Project project = createProject(id, user, "project2", "description2");
        ProjectDetailsResponseDto expected = createProjectDetailsResponseDto(project);

        //When
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectMapper.toDto(project)).thenReturn(expected);

        //Then
        ProjectDetailsResponseDto actual = projectService.getById(id);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(userService, times(1)).getAuthenticatedUser();
        verify(projectRepository, times(1)).findById(id);
        verify(projectMapper, times(1)).toDto(project);
    }

    @Test
    @DisplayName("Get by id with unauthenticated user, should throw an Exception")
    void getById_WithUnauthenticatedUser_ShouldThrowsException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        User user = createUser(2);
        Long id = 2L;
        Project project = createProject(id, user, "project2", "description2");
        String expected = "Unable to find authenticated user";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenThrow(new DataProcessingException(expected));
        Exception exception = assertThrows(
                DataProcessingException.class, () -> projectService.getById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Get by id that not contain a user in the project, should throw an Exception")
    void getById_WithInvalidAuthenticatedUser_ShouldThrowsException() {
        //Given
        User user = createUser(8);
        User mainUser = createUser(1);
        Long id = 2L;
        Project project = createProject(id, mainUser, "project1", "description1");
        String expected = "You can only get your projects! The project under this id: " + id
                + " is not yours.";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.getById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Get by id by invalid project id, should throw an Exception")
    void getById_WithInvalidProjectId_ShouldThrowsException() {
        //Given
        Long id = 999L;
        String expected = "Can't find project by id: " + id;

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.getById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update by id with valid data should return updated ProjectDetailsResponseDto")
    void updateById_WithValidData_ShouldReturnUpdatedProjectDetailsResponseDto() {
        //Given
        Long id = 2L;
        User user = createUser(1);
        Project project = createProject(id, user, "project1", "description1");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        createProject(request, project);
        ProjectDetailsResponseDto expected = createProjectDetailsResponseDto(project);

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(userService.getById(7L)).thenReturn(createUser(7));
        when(projectRepository.update(project)).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(expected);

        //Then
        ProjectDetailsResponseDto actual = projectService.updateById(id, request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectRepository, times(2)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
        verify(userService, times(1)).getById(7L);
        verify(projectRepository, times(1)).update(project);
        verify(projectMapper, times(1)).toDto(project);
    }

    @Test
    @DisplayName("Update by id with null endDate and invalid start day, should throw an Exception")
    void updateById_WithValidDataWithNullEndDateAndInvalidStartDay_ShouldThrowException() {
        //Given
        Long id = 2L;
        User user = createUser(1);
        Project project = createProject(id, user, "project1", "description1");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        request.setEndDate(null);
        String expected = "Date incorrect please entry valid date";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.updateById(id, request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Update by id with null enDate, should return updated ProjectDetailsResponseDto")
    void updateById_WithValidDataWithNullEnDate_ShouldReturnUpdatedProjectDetailsResponseDto() {
        //Given
        Long id = 2L;
        User user = createUser(1);
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        request.setStartDate(LocalDate.parse("2026-01-01", DATE_FORMATTER));
        request.setEndDate(null);
        Project updatedProject = createProject(id, user, "project1", "description1");
        createProject(request, updatedProject);
        ProjectDetailsResponseDto expected = createProjectDetailsResponseDto(updatedProject);
        Project project = createProject(id, user, "project1", "description1");

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(userService.getById(7L)).thenReturn(createUser(7));
        when(projectRepository.update(any(Project.class))).thenReturn(updatedProject);
        when(projectMapper.toDto(any(Project.class))).thenReturn(expected);

        //Then
        ProjectDetailsResponseDto actual = projectService.updateById(id, request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        //Verify
        verify(projectRepository, times(2)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
        verify(userService, times(1)).getById(7L);
        verify(projectRepository, times(1)).update(any(Project.class));
        verify(projectMapper, times(1)).toDto(any(Project.class));
    }

    @Test
    @DisplayName("Update by id with invalid endDate and exists startDay, should throw an Exception")
    void updateById_WithValidDataWithInvalidEndDateAndExistsStartDay_ShouldThrowException() {
        //Given
        Long id = 2L;
        User user = createUser(1);
        Project project = createProject(id, user, "project1", "description1");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        request.setEndDate(LocalDate.parse("2024-01-01", DATE_FORMATTER));
        String expected = "Date incorrect please entry valid date";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.updateById(id, request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Update by id with invalid endDate end non exists startDay, "
            + "Should throw an Exception")
    void updateById_WithValidDataWithInvalidEndDateAndNonExistsStartDate_ShouldThrowException() {
        //Given
        Long id = 2L;
        User user = createUser(1);
        Project project = createProject(id, user, "project1", "description1");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        request.setStartDate(null);
        request.setEndDate(LocalDate.parse("2024-01-01", DATE_FORMATTER));
        String expected = "Date incorrect please entry valid date";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.updateById(id, request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Update by id with not admin user, should throw an Exception")
    void updateById_WithValidDataWithNotAdminUser_ShouldThrowException() {
        //Given
        Long id = 3L;
        User user = createUser(5);
        User mainUser = createUser(1);
        Project project = createProject(id, mainUser, "project3", "description3");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        String expected = "You are not the administrator of this project,"
                + " you have no rights to update the project";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.updateById(id, request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Update by id without access to project, should throw an Exception")
    void updateById_WithValidDataWithoutAccessToProject_ShouldThrowException() {
        //Given
        Long id = 3L;
        User user = createUser(7);
        User mainUser = createUser(1);
        Project project = createProject(id, mainUser, "project3", "description3");
        ProjectUpdateRequestDto request = createProjectUpdateRequest(Set.of(7L));
        String expected = "You can only get your projects! " + "The project under this id: " + id
                + " is not yours.";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.updateById(id, request)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Delete by valid id with main user authenticated should delete project")
    void deleteById_WithValidDataWithValidUser_ShouldDeleteProject() {
        //Given
        Long id = 3L;
        User user = createUser(7);
        Project project = createProject(id, user, "project3", "description3");

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);

        //Then
        assertDoesNotThrow(() -> projectService.deleteById(id));

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Delete by valid id with not main user, should throw an Exception")
    void deleteByValidId_WithNotMainUser_ShouldThrowException() {
        //Given
        Long id = 4L;
        User user = createUser(5);
        User mainUser = createUser(7);
        Project project = createProject(id, mainUser, "project5", "description5");
        String expected = "You cannot delete this project, only the main user can do that";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.deleteById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(2)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Delete by valid id with an authenticated user without access to the project,"
            + " should throw an Exception")
    void deleteByValidId_WithoutAccessToProject_ShouldThrowException() {
        //Given
        Long id = 4L;
        User user = createUser(9);
        User mainUser = createUser(7);
        Project project = createProject(id, mainUser, "project5", "description5");
        String expected = "You can only get your projects! " + "The project under this id: " + id
                + " is not yours.";

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> projectService.deleteById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
        verify(userService, times(1)).getAuthenticatedUser();
    }

    @Test
    @DisplayName("Delete by invalid id, should throw an Exception")
    void delete_ByInvalidId_ShouldThrowException() {
        //Given
        Long id = 999L;
        String expected = "Can't find project by id: " + id;

        //When
        when(projectRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.deleteById(id)
        );

        //Then
        String actual = exception.getMessage();
        assertEquals(expected, actual);

        //Verify
        verify(projectRepository, times(1)).findById(id);
    }

    private ProjectUpdateRequestDto createProjectUpdateRequest(Set<Long> userIds) {
        ProjectUpdateRequestDto request = new ProjectUpdateRequestDto();
        request.setName("update");
        request.setDescription("update");
        request.setUsers(userIds);
        request.setStartDate(LocalDate.parse("2027-02-03", DATE_FORMATTER));
        request.setEndDate(LocalDate.parse("2027-03-04", DATE_FORMATTER));
        request.setStatus(Project.Status.COMPLETED);
        return request;
    }

    private ProjectRequestDto createRequest(Set<User> usersIds, Set<User> administratorsIds) {
        ProjectRequestDto request = new ProjectRequestDto();
        request.setName("project 3");
        request.setDescription("description 3");
        request.setStatus(Project.Status.INITIATED);
        request.setUsers(new HashSet<>(usersIds.stream()
                .map(User::getId)
                .collect(Collectors.toSet()))
        );
        request.setAdministrators(new HashSet<>(administratorsIds.stream()
                .map(User::getId)
                .collect(Collectors.toSet()))
        );
        request.setStartDate(LocalDate.parse("2025-12-12", DATE_FORMATTER));
        request.setEndDate(LocalDate.parse("2026-03-03", DATE_FORMATTER));
        return request;
    }

    private Project createProject(User mainUser, ProjectRequestDto request) {
        Project project = new Project();
        project.setId(1L);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setMainUser(mainUser);
        project.setUsers(createUsers(1, request.getUsers().size()));
        project.setAdministrators(createUsers(1, request.getAdministrators().size()));
        return project;
    }

    private Project createProject(ProjectUpdateRequestDto request, Project project) {
        Optional.ofNullable(request.getName())
                .ifPresent(project::setName);
        Optional.ofNullable(request.getDescription())
                .ifPresent(project::setDescription);
        Optional.ofNullable(request.getStartDate())
                .ifPresent(project::setStartDate);
        Optional.ofNullable(request.getStartDate())
                .ifPresent(project::setStartDate);
        Optional.ofNullable(request.getEndDate())
                .ifPresent(project::setEndDate);
        Optional.ofNullable(request.getStatus())
                .ifPresent(project::setStatus);
        Optional.ofNullable(request.getAdministrators())
                .ifPresent(admins -> {
                    Set<User> adminsSet = new HashSet<>();

                    for (Long id : admins) {
                        adminsSet.add(createUser(id.intValue()));
                    }

                    project.getAdministrators().addAll(adminsSet);
                });
        Optional.ofNullable(request.getUsers())
                .ifPresent(users -> {
                    Set<User> usersSet = new HashSet<>();

                    for (Long id : users) {
                        usersSet.add(createUser(id.intValue()));
                    }

                    project.getUsers().addAll(usersSet);
                });
        return project;
    }

    private Project createProject(Long id, User mainUser, String name, String description) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription(description);
        project.setStatus(Project.Status.IN_PROGRESS);
        project.setStartDate(LocalDate.parse("2025-12-12", DATE_FORMATTER));
        project.setEndDate(LocalDate.parse("2026-03-03", DATE_FORMATTER));
        Set<User> users = createUsers(1, 6);
        users.add(mainUser);
        project.setUsers(users);
        Set<User> admins = createUsers(1, 3);
        admins.add(mainUser);
        project.setAdministrators(admins);
        project.setMainUser(mainUser);
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

    private ProjectLowInfoResponse createProjectLowInfoResponse(Project project) {
        ProjectLowInfoResponse response = new ProjectLowInfoResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        return response;
    }

    private Set<User> createUsers(int startIndex, int endIndex) {
        Set<User> users = new HashSet<>();

        for (int i = startIndex; i <= endIndex; i++) {
            User user = createUser(i);
            users.add(user);
        }

        return users;
    }

    private User createUser(int i) {
        User user = new User();
        user.setId((long) i);
        user.setEmail("user" + i + "@example.com");
        user.setPassword("User=123456789");
        user.setUsername("user" + i);
        user.setFirstName("user" + i);
        user.setLastName("user" + i);
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        user.getRoles().add(role);
        return user;
    }
}
