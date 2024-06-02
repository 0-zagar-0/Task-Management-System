package task.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.user.UserLoginRequestDto;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.exception.EntityNotFoundException;
import task.system.security.CustomUserDetailsService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationControllerTest {
    private static final String TOKEN_REGEX = ".*\"token\":\".*\\..*\\..*\".*";
    private static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CustomUserDetailsService detailsService;

    @BeforeAll
    static void setUp(@Autowired WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Register user with valid data, should return UserResponseDto")
    @Sql(scripts = {
            "classpath:database/authentication/delete_user_with_users_roles_table_with_id_11.sql",
            "classpath:database/authentication/delete_user_with_id_11.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void registerUser_WithValidData_ShouldReturnUser() throws Exception {
        //Given
        UserRegisterRequestDto userRequest = createUserRequest();
        UserResponseDto expected = createUserResponse(userRequest);

        //When
        String jsonRequest = objectMapper.writeValueAsString(userRequest);
        MvcResult mvcResult = mockMvc.perform(
                        post("/auth/register")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(), UserResponseDto.class
        );
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Login exists user with valid email, should return JsonWebToken")
    public void loginExistsUser_WithValidEmail_ShouldReturnJwtToken() throws Exception {
        //Given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setUsernameOrEmail("user1@example.com");
        request.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/auth/login")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        String actual = result.getResponse().getContentAsString();
        Assertions.assertTrue(actual.matches(TOKEN_REGEX));
    }

    @Test
    @DisplayName("Login exists user with valid username, should return JsonWebToken")
    public void loginExistsUser_WithValidUsername_ShouldReturnJwtToken() throws Exception {
        //Given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setUsernameOrEmail("user4");
        request.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        String actual = result.getResponse().getContentAsString();
        Assertions.assertTrue(actual.matches(TOKEN_REGEX));
    }

    @Test
    @DisplayName("Login non exists user with email, should return exception")
    public void loginNonExistsUser_WithEmail_ShouldReturnException() throws Exception {
        //Given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setUsernameOrEmail("nonexists@example.com");
        request.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                post("/auth/login")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> detailsService.loadUserByUsername(request.getUsernameOrEmail())
        );

        //Then
        String expected = "Can't find user by email: " + request.getUsernameOrEmail();
        String actual = result.getResponse().getContentAsString();
        assertTrue(actual.contains(expected));
        assertEquals(expected, exception.getMessage());
    }

    @Test
    @DisplayName("Login non exists user with username, should return exception")
    public void loginNonExistsUser_WithUsername_ShouldReturnException() throws Exception {
        //Given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setUsernameOrEmail("nonexists");
        request.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(request);
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> detailsService.loadUserByUsername(request.getUsernameOrEmail())
        );

        //Then
        String expected = "Can't find user by username: " + request.getUsernameOrEmail();
        String actual = result.getResponse().getContentAsString();
        assertTrue(actual.contains(expected));
        assertEquals(expected, exception.getMessage());
    }

    private UserRegisterRequestDto createUserRequest() {
        UserRegisterRequestDto user = new UserRegisterRequestDto();
        user.setEmail("register@example.com");
        user.setPassword("User=123456789");
        user.setRepeatPassword("User=123456789");
        user.setUsername("register");
        user.setFirstName("register");
        user.setLastName("register");
        return user;
    }

    private UserResponseDto createUserResponse(UserRegisterRequestDto request) {
        UserResponseDto user = new UserResponseDto();
        user.setId(11L);
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        return user;
    }
}
