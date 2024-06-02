package task.system.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.user.UserResponseDto;
import task.system.dto.user.UserUpdateProfileRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Get user profile with authenticated user, should return UserResponseDto")
    @WithMockUser(username = "user1", roles = "USER")
    void getProfile_WithAuthenticatedUser_ShouldReturnUserDto() throws Exception {
        //Given
        UserResponseDto expected = createUserResponse();

        //When
        MvcResult result = mockMvc.perform(
                get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class
        );
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Get profile with unauthenticated user, should return 403 status")
    void getProfile_WithUnauthenticatedUser_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        int expected = 403;

        //When
        MvcResult result = mockMvc.perform(
                get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update role with user role USER to ADMIN, should return updated UserResponseDto")
    @WithMockUser(username = "admin", roles = "ADMIN")
    @Sql(scripts = "classpath:database/user/restore_user_role_with_user_id_2.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateRole_WithUserRoleAdmin_ShouldReturnUserResponseDto() throws Exception {
        //Given
        UserResponseDto expected = createUserResponse();
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                put("/users/{id}/role", id)
                        .param("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class
        );
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Update role with unauthenticated user, should return 403 status")
    void updateRole_WithUnauthenticatedUser_ShouldReturnStatusForbidden() throws Exception {
        //Given
        int expected = 403;
        Long id = 2L;

        //When
        MvcResult result = mockMvc.perform(
                        put("/users/{id}/role", id)
                                .param("role", "ADMIN")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update role with non exists user, should return 404 status")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateRole_WithUserRoleAdminForNonExistsUser_ShouldReturnNotFoundStatus()
            throws Exception {
        //Given
        int expected = 404;
        Long id = 999L;

        //When
        MvcResult result = mockMvc.perform(
                put("/users/{id}/role", id)
                        .param("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update role with user without role ADMIN, should return 403 status")
    @WithMockUser(username = "user2", roles = "USER")
    void updateRole_WithUserWithoutRoleAdmin_ShouldReturnForbiddenStatus() throws Exception {
        //Given
        int expected = 403;
        Long id = 4L;

        //When
        MvcResult result = mockMvc.perform(
                put("/users/{id}/role", id)
                        .param("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        //Then
        int actual = result.getResponse().getStatus();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update user profile with valid data, should return UserResponseDto")
    @WithMockUser(username = "user1", roles = "USER")
    @Sql(scripts = "classpath:database/user/restore_user_with_id_2_to_last_state.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateProfile_WithValidData_ShouldReturnUserResponseDto() throws Exception {
        //Given
        UserUpdateProfileRequest request = createUpdateRequest();
        UserResponseDto expected = createUserResponse(2L, request);

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                put("/users/me")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class
        );
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    private UserResponseDto createUserResponse() {
        UserResponseDto response = new UserResponseDto();
        response.setId(2L);
        response.setEmail("user1@example.com");
        response.setUsername("user1");
        response.setFirstName("user1");
        response.setLastName("user1");
        return response;
    }

    private UserResponseDto createUserResponse(Long id, UserUpdateProfileRequest request) {
        UserResponseDto response = new UserResponseDto();
        response.setEmail(request.getEmail());
        response.setId(id);
        response.setUsername(request.getUsername());
        response.setFirstName(request.getFirstName());
        response.setLastName(request.getLastName());
        return response;
    }

    private UserUpdateProfileRequest createUpdateRequest() {
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setEmail("update@example.com");
        request.setPassword("User=123456789");
        request.setRepeatPassword("User=123456789");
        request.setUsername("update");
        request.setFirstName("update");
        request.setLastName("update");
        return request;
    }
}
