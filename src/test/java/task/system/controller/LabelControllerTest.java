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
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.label.LabelRequestDto;
import task.system.dto.label.LabelResponseDto;
import task.system.dto.label.LabelUpdateRequestDto;
import task.system.exception.DataProcessingException;
import task.system.model.Label;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabelControllerTest {
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context, @Autowired DataSource dataSource) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        tearDown(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(connection, "insert_two_project_to_projects_table.sql");
            callSqlQueryFromFile(connection, "insert_users_ids_to_projects_users_table.sql");
            callSqlQueryFromFile(
                    connection, "insert_users_ids_to_projects_administrators_table.sql"
            );
        } catch (SQLException e) {
            throw new DataProcessingException("Can't execute SQL query", e);
        }
    }

    @AfterAll
    static void setDown(@Autowired DataSource dataSource) {
        tearDown(dataSource);
    }

    static void tearDown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            callSqlQueryFromFile(
                    connection, "delete_users_ids_from_projects_administrators_table.sql"
            );
            callSqlQueryFromFile(connection, "delete_user_ids_from_projects_users_table.sql");
            callSqlQueryFromFile(connection, "delete_projects_from_projects_table.sql");
        } catch (SQLException e) {
            throw new DataProcessingException("Can't execute SQL query", e);
        }
    }

    @Test
    @DisplayName("Create label with valid data should return LabelResponseDto")
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void create_WithValidData_ShouldReturnLabelResponseDto() throws Exception {
        //Given
        LabelRequestDto request = createRequestDto(1, 1L, Label.Color.YELLOW);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        LabelResponseDto expected = createResponseDto(7L, request);
        LabelResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), LabelResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Create label with null value color and null value name,"
            + " should return default label with color GREY")
    @WithMockUser(username = "user1", roles = "USER")
    void create_WithNullValueColorAndName_ShouldReturnDefaultLabel() throws Exception {
        //Given
        LabelRequestDto request = new LabelRequestDto();
        request.setProjectId(1L);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        LabelResponseDto expected = new LabelResponseDto();
        expected.setId(1L);
        expected.setColor(Label.Color.GRAY);
        LabelResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), LabelResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Create label with null project id, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void create_WithNullProjectId_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        LabelRequestDto request = createRequestDto(1, null, Label.Color.YELLOW);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
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
    @DisplayName("Create label with non exists project id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void create_WithNonExistentProjectId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        LabelRequestDto request = createRequestDto(1, 999L, Label.Color.YELLOW);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
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
    @DisplayName("Create label with unauthenticated user, should return FORBIDDEN(403) status")
    void create_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        LabelRequestDto request = createRequestDto(1, 1L, Label.Color.YELLOW);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
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
    @DisplayName("Create label with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user9", roles = "USER")
    void create_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        LabelRequestDto request = createRequestDto(1, 1L, Label.Color.YELLOW);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/labels")
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
    @DisplayName("Get all by valid project id, should return all LabelResponseDto")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void getAll_WithValidProjectId_ShouldReturnAllLabelResponseDto() throws Exception {
        //Given
        Long projectId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/project/{projectId}", projectId))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<LabelResponseDto> expected = new ArrayList<>();
        expected.add(createResponseDto(1L, null, Label.Color.GRAY, null));
        expected.add(createResponseDto(2L, null, Label.Color.RED, null));
        expected.add(createResponseDto(3L, null, Label.Color.BLUE, null));
        expected.add(createResponseDto(4L, null, Label.Color.GREEN, null));
        expected.add(createResponseDto(5L, null, Label.Color.BROWN, null));
        expected.add(createResponseDto(6L, null, Label.Color.PURPLE, null));
        expected.add(createResponseDto(7L, "label1", Label.Color.WHITE, 1L));
        List<LabelResponseDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<LabelResponseDto>>() {}
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(1), actual.get(1)));
    }

    @Test
    @DisplayName("Get all by invalid project id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void getAll_WithInvalidProjectId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long projectId = 999L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/project/{projectId}", projectId))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get all by authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user8", roles = "USER")
    void getAll_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long projectId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/project/{projectId}", projectId))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get all with unauthenticated user, should return FORBIDDEN(403) status")
    void getAll_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        Long projectId = 1L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/project/{projectId}", projectId))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get by id with valid id, should return LabelResponseDto")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void getById_WithValidId_ShouldReturnLabelResponseDto() throws Exception {
        //Given
        Long id = 7L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        LabelResponseDto expected = createResponseDto(id, "label1", Label.Color.WHITE, 1L);
        LabelResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), LabelResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Get by id with invalid id, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void getById_WithInvalidId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Get by id with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user9", roles = "USER")
    void getById_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 7L;

        //When
        MvcResult result = mockMvc.perform(
                get("/labels/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Get by id with unauthenticated user, should return FORBIDDEN(403) status")
    void getById_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        Long id = 7L;

        //When
        MvcResult result = mockMvc.perform(
                        get("/labels/{id}", id))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Update by id with valid data, should return updated LabelResponseDto")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithValidData_ShouldReturnUpdatedLabelResponseDto() throws Exception {
        //Given
        LabelUpdateRequestDto request = new LabelUpdateRequestDto();
        request.setName("update");
        request.setColor(Label.Color.MAGENTA);
        Long id = 7L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/labels/{id}", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        LabelResponseDto expected = createResponseDto(id, "update", Label.Color.MAGENTA, 1L);
        LabelResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), LabelResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Update by id with id non exists, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithNonExistsId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        LabelUpdateRequestDto request = new LabelUpdateRequestDto();
        request.setName("update");
        request.setColor(Label.Color.MAGENTA);
        Long id = 999L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/labels/{id}", id)
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
    @DisplayName("Update by id with id default labels, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void updateById_WithValidIdDefaultLabel_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        LabelUpdateRequestDto request = new LabelUpdateRequestDto();
        request.setName("update");
        request.setColor(Label.Color.MAGENTA);
        Long id = 1L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/labels/{id}", id)
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
    @DisplayName("Update by id with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user8", roles = "USER")
    void updateById_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        LabelUpdateRequestDto request = new LabelUpdateRequestDto();
        request.setName("update");
        request.setColor(Label.Color.MAGENTA);
        Long id = 7L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/labels/{id}", id)
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
    @DisplayName("Update by id with unauthenticated user, should return FORBIDDEN(403) status")
    void updateById_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);
        LabelUpdateRequestDto request = new LabelUpdateRequestDto();
        request.setName("update");
        request.setColor(Label.Color.MAGENTA);
        Long id = 6L;

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        put("/labels/{id}", id)
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
    @DisplayName("Delete by id with valid id, should return NO_CONTENT(204) status")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user1", roles = "USER")
    void deleteById_WithValidId_ShouldReturnNoContentStatus() throws Exception {
        //Given
        Long id = 7L;

        //When
        MvcResult result = mockMvc.perform(
                delete("/labels/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        //Then
        int expectedErrorCode = 204;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with id non exists, should return NOT_FOUND(404) status")
    @WithMockUser(username = "user1", roles = "USER")
    void deleteById_WithNonExistsId_ShouldReturnNotFoundStatus() throws Exception {
        //Given
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/labels/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int expectedErrorCode = 404;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with authenticated user does not belong to project, "
            + "should return BAD_REQUEST(400) status")
    @Sql(scripts = "classpath:database/label/insert_label_to_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/label/delete_label_with_id_7_from_labels_table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "user9", roles = "USER")
    void deleteById_WithAuthenticatedUserNotBelongProject_ShouldReturnBadRequestStatus()
            throws Exception {
        //Given
        Long id = 7L;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/labels/{id}", id))
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
        SecurityContextHolder.getContext().setAuthentication(null);
        Long id = 5L;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/labels/{id}", id))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int expectedErrorCode = 403;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    @Test
    @DisplayName("Delete by id with id default label, should return BAD_REQUEST(400) status")
    @WithMockUser(username = "user1", roles = "USER")
    void deleteById_WithIdDefaultLabel_ShouldReturnBadRequestStatus() throws Exception {
        //Given
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                        delete("/labels/{id}", id))
                .andExpect(status().isBadRequest())
                .andReturn();

        //Then
        int expectedErrorCode = 400;
        int actualErrorCode = result.getResponse().getStatus();
        assertEquals(expectedErrorCode, actualErrorCode);
    }

    private LabelRequestDto createRequestDto(int number, Long projectId, Label.Color color) {
        LabelRequestDto request = new LabelRequestDto();
        request.setName("label" + number);
        request.setProjectId(projectId);
        request.setColor(color);
        return request;
    }

    private LabelResponseDto createResponseDto(
            Long id, String name, Label.Color color, Long projectId
    ) {
        LabelResponseDto response = new LabelResponseDto();
        response.setId(id);
        response.setName(name);
        response.setColor(color);
        response.setProjectId(projectId);
        return response;
    }

    private LabelResponseDto createResponseDto(Long id, LabelRequestDto request) {
        LabelResponseDto response = new LabelResponseDto();
        response.setId(id);
        response.setName(request.getName());
        response.setColor(request.getColor());
        response.setProjectId(request.getProjectId());
        return response;
    }

    private static void callSqlQueryFromFile(Connection connection, String filename) {
        ScriptUtils.executeSqlScript(
                connection, new ClassPathResource("database/label/" + filename)
        );
    }
}
