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
import java.util.List;
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
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.dto.task.TaskUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.model.Task;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskControllerTest {
    protected static final DateTimeFormatter DATE_TIME_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context, @Autowired DataSource dataSource) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teardown(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(connection, "add_three_projects_to_projects_table.sql");
            callSqlQueryFromFile(connection, "add_users_ids_to_projects_users_table.sql");
            callSqlQueryFromFile(connection, "add_users_ids_to_projects_administrators_table.sql");
            callSqlQueryFromFile(connection, "add_three_tasks_for_project1_to_tasks_table.sql");
        } catch (SQLException e) {
            throw new DataProcessingException("Cannot connect to the database", e);
        }
    }

    @AfterAll
    static void setDown(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(connection, "delete_all_task.sql");
            callSqlQueryFromFile(connection, "delete_all_users_from_projects_users_table.sql");
            callSqlQueryFromFile(
                    connection, "delete_all_users_from_projects_administrators_table.sql"
            );
            callSqlQueryFromFile(connection, "delete_all_project_from_projects_table.sql");
        }
    }

    @Test
    @DisplayName("Create task with valid data, should return TaskFullDetailsDto")
    @Sql(scripts = "classpath:database/task/delete_task_with_id_4_from_tasks_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithValidData_ShouldReturnTaskFullDetailsDto() throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(4, 4L, 1L, "2025-07-01");
        Task task = task(4L, request);
        TaskFullDetailsDto expected = taskFullDetailsDto(task);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        TaskFullDetailsDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), TaskFullDetailsDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Create task with invalid project id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithInvalidProjectId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, id, "2025-07-01");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create task with invalid dueDate before project startDate,"
            + " should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithInvalidDueDateBeforeProjectStartDate_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        String dueDate = "2023-07-01";
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, 1L, dueDate);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create task with invalid dueDate after project enDate,"
            + " should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithInvalidDueDateAfterProjectEndDate_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        String dueDate = "2023-07-01";
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, 1L, dueDate);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/tasks")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create task with null value task name, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithNullTaskName_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, 1L, "2025-07-01");
        request.setName(null);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create a task with a assignee ID that does not belong to the project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithInvalidAssigneeId_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(1, 9L, 1L, "2025-07-01");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create a task with non exists assignee ID, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void createTask_WithNonExistsAssigneeId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(1, 999L, 1L, "2025-07-01");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/tasks")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create task with unauthenticated user, should return FORBIDDEN(403) status")
    void createTask_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, 1L, "2025-07-01");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Create task with authenticated user that does not belong to the project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user9", roles = "USER")
    void createTask_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        TaskCreateRequestDto request = taskCreateRequestDto(1, 4L, 1L, "2025-07-01");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/tasks")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get all by valid project id should return all TaskLowDetailsDto")
    @WithMockUser(username = "user1", roles = "USER")
    void getAll_ByValidProjectId_ShouldReturnAllTaskLowDetailsDto() throws Exception {
        //Given
        List<TaskLowDetailsDto> expected = List.of(
                taskLowDetailsDto(1L), taskLowDetailsDto(2L), taskLowDetailsDto(3L)
        );
        Long projectId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/tasks/project/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<TaskLowDetailsDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<TaskLowDetailsDto>>() {}
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
    }

    @Test
    @DisplayName("Get all with authenticated user that does not belong to the project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAll_ByAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long projectId = 2L;

        //When
        MvcResult result = mockMvc.perform(
                        get("/tasks/project/{projectId}", projectId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get all by invalid project id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user2", roles = "USER")
    void getAll_ByInvalidProjectId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long projectId = 999L;

        //When
        MvcResult result = mockMvc.perform(
                        get("/tasks/project/{projectId}", projectId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get by valid id should return TaskFullDetailsDto")
    @WithMockUser(username = "user2", roles = "USER")
    void getById_ByValidId_ShouldReturnTaskFullDetailsDto() throws Exception {
        //Given
        Long id = 2L;
        TaskFullDetailsDto expected = taskFullDetailsDto(id);

        //When
        MvcResult result = mockMvc.perform(
                get("/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        TaskFullDetailsDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), TaskFullDetailsDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Get by valid id with authenticated user that does not belong to the project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user9", roles = "USER")
    void getByValidId_ByAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                        get("/tasks/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get by invalid id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void getById_WithInvalidId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                        get("/tasks/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by valid id , should return updated TaskFullDetailsDto")
    @Sql(scripts = "classpath:database/task/restore_task_with_id_2_to_last_state.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithValidIdAndValidUpdateData_ShouldReturnUpdatedTaskFullDetailsDto()
            throws Exception {
        //Given
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();
        TaskFullDetailsDto expected = taskFullDetailsDto(id, request);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/tasks/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        TaskFullDetailsDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), TaskFullDetailsDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Update by id with invalid id should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithInvalidIdAndValidUpdateData_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/tasks/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by valid id with invalid dueDate is before project start date,"
            + " should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithInvalidDueDateRequestBeforeStartDate_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();
        request.setDueDate(LocalDate.parse("2023-11-11", DATE_TIME_FORMATTER));

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/tasks/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by valid id with invalid dueDate is after project end date,"
            + " should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithInvalidDueDateRequestAfterEndDate_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();
        request.setDueDate(LocalDate.parse("2023-11-11", DATE_TIME_FORMATTER));

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/tasks/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with assignee id does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithInvalidAssigneeIdRequest_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        Long assigneeId = 6L;
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();
        request.setAssigneeId(assigneeId);

        //
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/tasks/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with non exists assignee id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithNonExistsAssigneeId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 2L;
        Long assigneeId = 999L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();
        request.setAssigneeId(assigneeId);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/tasks/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with unauthenticated user, should return FORBIDDEN(403) status")
    void updateById_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/tasks/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with authenticated user not admin, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user4", roles = "USER")
    void updateById_WithAuthenticatedUserNotAdmin_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        Long id = 2L;
        TaskUpdateRequestDto request = taskUpdateRequestDto();

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/tasks/{id}", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by valid id should return NO_CONTENT(204) status")
    @Sql(scripts = "classpath:database/task/restore_task_with_id_2_to_last_state.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void deleteById_WithValidId_ShouldReturnNoContentStatus() throws Exception {
        //Given
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/tasks/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        //Then
        int expectedErrorCode = 204;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by invalid id should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void deleteById_WithInvalidId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with user not admin, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user4", roles = "USER")
    void deleteById_WithUserNotAdmin_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        Long id = 2L;

        //WHne
        MvcResult result = mockMvc.perform(
                delete("/tasks/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user7", roles = "USER")
    void deleteById_WithAuthenticatedNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/tasks/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with unauthenticated user, should return FORBIDDEN(403) status")
    void deleteById_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        Long id = 3L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/tasks/{id}", id))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    private TaskUpdateRequestDto taskUpdateRequestDto() {
        TaskUpdateRequestDto taskUpdateRequestDto = new TaskUpdateRequestDto();
        taskUpdateRequestDto.setName("update");
        taskUpdateRequestDto.setDescription("update");
        taskUpdateRequestDto.setDueDate(LocalDate.parse("2025-07-04", DATE_TIME_FORMATTER));
        taskUpdateRequestDto.setPriority(Task.Priority.MEDIUM);
        taskUpdateRequestDto.setStatus(Task.Status.IN_PROGRESS);
        taskUpdateRequestDto.setAssigneeId(5L);
        return taskUpdateRequestDto;
    }

    private TaskLowDetailsDto taskLowDetailsDto(Long id) {
        TaskLowDetailsDto response = new TaskLowDetailsDto();
        response.setId(id);
        response.setName("task" + id);
        response.setDescription("description" + id);
        return response;
    }

    private Task task(Long id, TaskCreateRequestDto request) {
        Task task = new Task();
        task.setId(id);
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());
        task.setProjectId(request.getProjectId());
        task.setAssigneeId(request.getAssigneeId());
        return task;
    }

    private TaskFullDetailsDto taskFullDetailsDto(Task task) {
        TaskFullDetailsDto response = new TaskFullDetailsDto();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        response.setDueDate(task.getDueDate());
        response.setProjectId(task.getProjectId());
        response.setAssigneeId(task.getAssigneeId());
        return response;
    }

    private TaskFullDetailsDto taskFullDetailsDto(Long id) {
        TaskFullDetailsDto response = new TaskFullDetailsDto();
        response.setId(id);
        response.setName("task" + id);
        response.setDescription("description" + id);
        response.setPriority(Task.Priority.MEDIUM);
        response.setStatus(Task.Status.IN_PROGRESS);
        response.setProjectId(1L);
        response.setAssigneeId(3L);
        response.setDueDate(LocalDate.parse("2025-07-02", DATE_TIME_FORMATTER));
        return response;
    }

    private TaskFullDetailsDto taskFullDetailsDto(Long id, TaskUpdateRequestDto request) {
        TaskFullDetailsDto response = new TaskFullDetailsDto();
        response.setId(id);
        response.setName(request.getName());
        response.setDescription(request.getDescription());
        response.setStatus(request.getStatus());
        response.setPriority(request.getPriority());
        response.setAssigneeId(request.getAssigneeId());
        response.setDueDate(request.getDueDate());
        response.setProjectId(1L);
        return response;
    }

    private TaskCreateRequestDto taskCreateRequestDto(
            int number, Long assigneeId, Long projectId, String date
    ) {
        TaskCreateRequestDto request = new TaskCreateRequestDto();
        request.setName("task" + number);
        request.setDescription("task" + number);
        request.setPriority(Task.Priority.LOW);
        request.setStatus(Task.Status.NOT_STARTED);
        request.setDueDate(LocalDate.parse(date, DATE_TIME_FORMATTER));
        request.setProjectId(projectId);

        if (assigneeId != null) {
            request.setAssigneeId(assigneeId);
        }

        return request;
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection, new ClassPathResource("database/task/" + fileName)
        );
    }
}
