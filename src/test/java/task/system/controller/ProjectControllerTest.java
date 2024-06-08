package task.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.model.Project;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectControllerTest {
    protected static final DateTimeFormatter TIME_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context, @Autowired DataSource dataSource) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teardown(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            callSqlQueryFromFile(connection, "add_project_to_projects_table.sql");
            callSqlQueryFromFile(connection, "add_users_ids_to_projects_users_table.sql");
            callSqlQueryFromFile(
                    connection, "add_users_ids_to_projects_administrators_table.sql"
            );
        } catch (SQLException e) {
            throw new DataProcessingException("Can't connect to database", e);
        }
    }

    @AfterAll
    static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(@Autowired DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            callSqlQueryFromFile(
                    connection, "delete_user_ids_from_projects_administrators_table.sql"
            );
            callSqlQueryFromFile(connection, "delete_user_ids_from_projects_users_table.sql");
            callSqlQueryFromFile(connection, "delete_projects_projects_table.sql");
        } catch (SQLException e) {
            throw new DataProcessingException("Can't connect to database", e);
        }
    }

    @Test
    @DisplayName("Create project with valid data, should return ProjectDetailsResponseDto")
    @Sql(scripts = "classpath:database/project/change_project_id_sequence.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void createProject_WithValidData_ShouldReturnProjectDetailsResponseDto() throws Exception {
        //Given
        ProjectRequestDto request = createProjectRequest();
        ProjectDetailsResponseDto expected = createProjectDetailsResponse(request);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/projects")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        ProjectDetailsResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProjectDetailsResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Create project with exists project name, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createProject_WithExistsProjectName_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectRequestDto request = createProjectRequest();
        request.setName("project1");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/projects")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expected = 400;
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Create project with empty name, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createProject_WithEmptyProjectName_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectRequestDto request = createProjectRequest();
        request.setName(null);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/projects")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expected = 400;
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Create project with invalid start day, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createProject_WithInvalidStartDate_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectRequestDto request = createProjectRequest();
        request.setStartDate(LocalDate.now().minusDays(1));

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/projects")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expected = 400;
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Create project with invalid end day, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createProject_WithInvalidEndDate_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectRequestDto request = createProjectRequest();
        request.setStartDate(LocalDate.now().minusDays(1));

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/projects")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expected = 400;
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Get all user projects, should return all user project")
    @WithMockUser(username = "user2", roles = "USER")
    void getAll_WithAuthenticatedUser_ShouldReturnAllProjects() throws Exception {
        //Given
        List<ProjectLowInfoResponse> expected = List.of(createProjectLowDetailsResponse());

        //When
        MvcResult result = mockMvc.perform(
                get("/projects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<ProjectLowInfoResponse> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<ProjectLowInfoResponse>>() {});
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Get all user project with unauthenticated user, "
            + "should return Forbidden(403) status")
    void getAllProjects_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        int expected = 403;

        //When
        MvcResult result = mockMvc.perform(
                        get("/projects")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(actual, expected);
    }

    @Test
    @DisplayName("Get by id with valid user, should return ProjectDetailsResponseDto")
    @WithMockUser(username = "user2", roles = "USER")
    void getById_WithAuthenticatedUser_ShouldReturnProjectDetailsResponseDto() throws Exception {
        //Given
        ProjectDetailsResponseDto expected = createProjectDetailsFromDb();
        Long id = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/projects/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        ProjectDetailsResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProjectDetailsResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Get by id with invalid user, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user7", roles = "USER")
    void getById_WithInvalidUser_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 1L;
        int expected = 404;

        //When
        MvcResult result = mockMvc.perform(
                get("/project/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update by id with valid data, should return updated ProjectDetailsResponseDto")
    @Sql(scripts = {"classpath:database/project/add_project_for_update_method.sql",
            "classpath:database/project/add_user_ids_to_projects_users_table_for_update_method.sql",
            "classpath:database/project/"
                    + "add_users_to_projects_administrators_table_for_update_method.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user6", roles = "USER")
    void updateById_WithValidData_ShouldReturnUpdatedProjectDetailsResponseDto() throws Exception {
        //Given
        ProjectUpdateRequestDto request = createProjectUpdateRequest();
        ProjectDetailsResponseDto expected = createProjectDetailsForUpdateMethod(request);
        Long id = 2L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/projects/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        ProjectDetailsResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProjectDetailsResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Update by id with invalid data, should return BAD_REQUEST(400) status")
    @Sql(scripts = {"classpath:database/project/add_project_for_update_method.sql",
            "classpath:database/project/add_user_ids_to_projects_users_table_for_update_method.sql",
            "classpath:database/project/"
                    + "add_users_to_projects_administrators_table_for_update_method.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user6", roles = "USER")
    void updateById_WithInvalidData_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectUpdateRequestDto request = createProjectUpdateRequest();
        request.setStartDate(LocalDate.now().minusDays(1));
        Long id = 2L;
        int expected = 400;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/projects/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update by id with exists project name, should return BAD_REQUEST(400) status")
    @Sql(scripts = {"classpath:database/project/add_project_for_update_method.sql",
            "classpath:database/project/add_user_ids_to_projects_users_table_for_update_method.sql",
            "classpath:database/project/"
                    + "add_users_to_projects_administrators_table_for_update_method.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user6", roles = "USER")
    void updateById_WithExistsProjectName_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectUpdateRequestDto request = createProjectUpdateRequest();
        request.setName("project1");
        Long id = 2L;
        int expected = 400;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/projects/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update by invalid id, should return NOT_FOUND status")
    @WithMockUser(username = "user1", roles = "USER")
    void update_ByInvalidId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        ProjectUpdateRequestDto request = createProjectUpdateRequest();
        Long id = 999L;
        int expected = 404;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/projects/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update by not administrator user, should return BAD_REQUEST(400)")
    @WithMockUser(username = "user4", roles = "USER")
    void update_ByNotAdminUser_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        ProjectUpdateRequestDto request = createProjectUpdateRequest();
        Long id = 1L;
        int expected = 400;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/projects/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Delete by valid id with main user from project, "
            + "should return NO_CONTENT(204) status")
    @Sql(scripts = {"classpath:database/project/add_project_for_update_method.sql",
            "classpath:database/project/add_user_ids_to_projects_users_table_for_update_method.sql",
            "classpath:database/project/"
                    + "add_users_to_projects_administrators_table_for_update_method.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user5", roles = "USER")
    void deleteById_WithValidIdAndMainUserFromProject_ShouldReturnNoContentStatus()
            throws Exception {
        //Given
        Long id = 2L;
        int expected = 204;

        //When
        MvcResult result = mockMvc.perform(
                delete("/projects/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Delete by valid id with usually user from project, "
            + "should return BAD_REQUEST(400) status")
    @Sql(scripts = {"classpath:database/project/add_project_for_update_method.sql",
            "classpath:database/project/add_user_ids_to_projects_users_table_for_update_method.sql",
            "classpath:database/project/"
                    + "add_users_to_projects_administrators_table_for_update_method.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"classpath:database/project/"
            + "delete_users_from_projects_administrators_table_with_project_id_2.sql",
            "classpath:database/project/"
                    + "delete_users_from_projects_users_table_with_project_id_2.sql",
            "classpath:database/project/delete_project_with_id_2_from_projects_table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user6", roles = "USER")
    void deleteById_WithValidIdAndUsuallyUserFromProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 2L;
        int expected = 400;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/projects/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Delete by invalid id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteById_WithInvalidId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;
        int expected = 404;

        //When
        MvcResult result = mockMvc.perform(
                delete("/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        assertEquals(expected, actual);
    }

    private ProjectUpdateRequestDto createProjectUpdateRequest() {
        ProjectUpdateRequestDto request = new ProjectUpdateRequestDto();
        request.setName("update");
        request.setDescription("update");
        request.setAdministrators(Set.of(7L));
        request.setStatus(Project.Status.COMPLETED);
        request.setEndDate(LocalDate.parse("2025-12-11"));
        return request;
    }

    private ProjectDetailsResponseDto createProjectDetailsForUpdateMethod(
            ProjectUpdateRequestDto request) {
        ProjectDetailsResponseDto response = new ProjectDetailsResponseDto();
        response.setId(2L);
        response.setName(request.getName());
        response.setDescription(request.getDescription());
        response.setMainUser(6L);
        response.setAdministratorIds(Set.of(6L, 7L));
        response.setUserIds(Set.of(6L, 7L, 8L, 9L));
        response.setStatus(request.getStatus());
        response.setEndDate(request.getEndDate());
        response.setStartDate(LocalDate.parse("2025-10-10", TIME_FORMATTER));
        return response;
    }

    private ProjectDetailsResponseDto createProjectDetailsFromDb() {
        ProjectDetailsResponseDto project = new ProjectDetailsResponseDto();
        project.setId(1L);
        project.setName("project1");
        project.setDescription("description1");
        project.setUserIds(Set.of(1L, 2L, 3L, 4L, 5L));
        project.setMainUser(1L);
        project.setAdministratorIds(Set.of(1L, 2L));
        project.setStartDate(LocalDate.parse("2025-06-08", TIME_FORMATTER));
        project.setEndDate(LocalDate.parse("2025-07-08", TIME_FORMATTER));
        project.setStatus(Project.Status.INITIATED);
        return project;
    }

    private ProjectRequestDto createProjectRequest() {
        ProjectRequestDto projectRequestDto = new ProjectRequestDto();
        projectRequestDto.setName("project2");
        projectRequestDto.setDescription("description2");
        Set<Long> adminIds = new HashSet<>();
        adminIds.add(2L);
        adminIds.add(5L);
        adminIds.add(6L);
        projectRequestDto.setAdministrators(adminIds);
        Set<Long> userIds = new HashSet<>();
        userIds.add(3L);
        userIds.add(4L);
        userIds.addAll(adminIds);
        projectRequestDto.setUsers(userIds);
        LocalDate startDate = LocalDate.parse("2025-07-15", TIME_FORMATTER);
        projectRequestDto.setStartDate(startDate);
        LocalDate endDate = LocalDate.parse("2025-09-15", TIME_FORMATTER);
        projectRequestDto.setEndDate(endDate);
        projectRequestDto.setStatus(Project.Status.IN_PROGRESS);
        return projectRequestDto;
    }

    private ProjectDetailsResponseDto createProjectDetailsResponse(ProjectRequestDto request) {
        ProjectDetailsResponseDto responseDto = new ProjectDetailsResponseDto();
        responseDto.setId(2L);
        responseDto.setName(request.getName());
        responseDto.setDescription(request.getDescription());
        responseDto.setMainUser(2L);
        responseDto.setAdministratorIds(request.getAdministrators());
        responseDto.setUserIds(request.getUsers());
        responseDto.setStartDate(request.getStartDate());
        responseDto.setEndDate(request.getEndDate());
        responseDto.setStatus(request.getStatus());
        return responseDto;
    }

    private ProjectLowInfoResponse createProjectLowDetailsResponse() {
        ProjectLowInfoResponse project = new ProjectLowInfoResponse();
        project.setId(1L);
        project.setName("project1");
        project.setDescription("description1");
        return project;
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection, new ClassPathResource("database/project/" + fileName)
        );
    }
}
