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
import java.time.LocalDateTime;
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
import task.system.dto.comment.CommentRequestDto;
import task.system.dto.comment.CommentResponseDto;
import task.system.dto.comment.CommentUpdateRequestDto;
import task.system.exception.DataProcessingException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommentControllerTest {
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context, @Autowired DataSource dataSource)
            throws SQLException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teardown(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(connection, "add_project_to_projects_table.sql");
            callSqlQueryFromFile(connection, "add_users_ids_to_projects_users_table.sql");
            callSqlQueryFromFile(connection, "add_users_ids_to_projects_administrators_table.sql");
            callSqlQueryFromFile(connection, "add_three_tasks_for_project1_to_tasks_table.sql");
            callSqlQueryFromFile(connection, "add_three_comment_to_comments_table.sql");
        } catch (SQLException e) {
            throw new DataProcessingException("Can't connect to database", e);
        }
    }

    @AfterAll
    static void setDown(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(connection, "delete_comments_from_comments_table.sql");
            callSqlQueryFromFile(connection, "delete_tasks_from_tasks_table.sql");
            callSqlQueryFromFile(connection, "delete_users_from_projects_administrators_table.sql");
            callSqlQueryFromFile(connection, "delete_users_from_projects_users_table.sql");
            callSqlQueryFromFile(connection, "delete_projects_from_projects_table.sql");
        } catch (SQLException e) {
            throw new DataProcessingException("Can't connect to database", e);
        }
    }

    @Test
    @DisplayName("Create comment with valid data, should return CommentResponseDto")
    @Sql(scripts = "classpath:database/comment/delete_comment_with_id_4_from_comments.table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user4", roles = "USER")
    void create_WithValidData_ShouldReturnCommentResponseDto() throws Exception {
        //Given
        CommentRequestDto request = createCommentRequestDto(4, 1L);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/comments")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        CommentResponseDto expected = createCommentResponseDto(4L, 5L, request);
        CommentResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), CommentResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual, "timestamp"));
    }

    @Test
    @DisplayName("Create comment with non exists task id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user4", roles = "USER")
    void create_WithNonExistsTaskId_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        CommentRequestDto request = createCommentRequestDto(4, 4L);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/comments")
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
    @DisplayName("Create comment with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user7", roles = "USER")
    void create_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        CommentRequestDto request = createCommentRequestDto(4, 2L);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/comments")
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
    @DisplayName("Get all by task id with valid task id should return List CommentResponseDto")
    @WithMockUser(username = "user2", roles = "USER")
    void getAll_WithValidTaskId_ShouldReturnListTaskResponseDto() throws Exception {
        //Given
        Long taskId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/comments")
                        .param("taskId", taskId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<CommentResponseDto> expected = List.of(
                createCommentResponseDto(2L, 1L, 4L),
                createCommentResponseDto(1L, 1L, 3L)
        );
        List<CommentResponseDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<CommentResponseDto>>() {}
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(
                (expected.get(0).getId().equals(1L) ? expected.get(0) : expected.get(1)),
                (actual.get(0).getId().equals(1L) ? actual.get(0) : actual.get(1)),
                "timestamp")
        );
    }

    @Test
    @DisplayName("Get all with non exists task id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user3", roles = "USER")
    void getAll_WithNonExistsTaskId_ShouldReturnNonExistsStatus() throws Exception {
        //Given
        Long taskId = 999L;

        //When
        MvcResult result = mockMvc.perform(
                get("/comments")
                        .param("taskId", taskId.toString()))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get all with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user8", roles = "USER")
    void getAll_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long taskId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/comments")
                        .param("taskId", taskId.toString()))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with valid id and request data, "
            + "should return updated CommentResponseDto")
    @Sql(scripts = "classpath:database/comment/restore_comment_with_id_1_to_last_state.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user2", roles = "USER")
    void update_WithValidIdAndRequestData_ShouldReturnUpdatedCommentResponseDto() throws Exception {
        //Given
        Long id = 1L;
        CommentUpdateRequestDto request = new CommentUpdateRequestDto();
        request.setText("update");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/comments/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        CommentResponseDto expected = createCommentResponseDto(id, 1L, 3L);
        expected.setText("update");
        CommentResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), CommentResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual, "timestamp"));
    }

    @Test
    @DisplayName("Update by id with non exists comment id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user2", roles = "USER")
    void update_WithNonExistsId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;
        CommentUpdateRequestDto request = new CommentUpdateRequestDto();
        request.setText("update");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/comments/{id}", id)
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
    @DisplayName("Update by id with authenticated user does not belong comment id, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user4", roles = "USER")
    void update_WithAuthenticatedUserNotBelongCommentId_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 1L;
        CommentUpdateRequestDto request = new CommentUpdateRequestDto();
        request.setText("update");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/comments/{id}", id)
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
    @DisplayName("Delete by id with valid comment id, should return NO_CONTENT(204) status")
    @Sql(scripts = "classpath:database/comment/restore_comment_with_id_1_to_last_state.sql")
    @WithMockUser(username = "user2", roles = "USER")
    void delete_WithValidId_ShouldReturnNoContentStatus() throws Exception {
        //Given
        Long id = 1L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/comments/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        //Then
        int expectedErrorCode = 204;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with non exists comment id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user4", roles = "USER")
    void delete_WithNonExistsId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/comments/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with authenticated user does not belong to comment, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user3", roles = "USER")
    void delete_WithAuthenticatedUserNotBelongCommentId_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 1L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/comments/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    private CommentRequestDto createCommentRequestDto(int index, Long taskId) {
        CommentRequestDto request = new CommentRequestDto();
        request.setText("comment" + index);
        request.setTaskId(taskId);
        return request;
    }

    private CommentResponseDto createCommentResponseDto(Long id, Long taskId, Long userId) {
        CommentResponseDto response = new CommentResponseDto();
        response.setId(id);
        response.setText("comment" + id);
        response.setTimestamp(LocalDateTime.now());
        response.setUserId(userId);
        response.setTaskId(taskId);
        return response;
    }

    private CommentResponseDto createCommentResponseDto(
            Long id, Long userId, CommentRequestDto request
    ) {
        CommentResponseDto response = new CommentResponseDto();
        response.setId(id);
        response.setTimestamp(LocalDateTime.now());
        response.setUserId(userId);
        response.setText(request.getText());
        response.setTaskId(request.getTaskId());
        return response;
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection, new ClassPathResource("database/comment/" + fileName)
        );
    }
}
