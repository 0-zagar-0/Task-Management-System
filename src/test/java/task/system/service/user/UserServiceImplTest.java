package task.system.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.dto.user.UserUpdateProfileRequest;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.exception.RegistrationException;
import task.system.mapper.UserMapper;
import task.system.model.Role;
import task.system.model.User;
import task.system.repository.role.RoleRepository;
import task.system.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Register user by non exists email with valid data, should return UserResponseDto")
    void registerUserByNonExistsEmail_WithValidData_ShouldReturnUserResponseDto() {
        //Given
        UserRegisterRequestDto request = createRegisterRequest();
        User user = createUserFromRequest(11L, request);
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        UserResponseDto expected = createUserResponse(user);

        //When
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.register(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(userRepository, times(1)).save(user);
        verify(roleRepository, times(1)).findByName(role.getName());
        verify(userMapper, times(1)).toEntity(request);
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Register user by exists email, should return exception")
    void registerUser_ByExistsEmail_ShouldReturnException() {
        //Given
        UserRegisterRequestDto request = createRegisterRequest();
        User user = createUserFromRequest(11L, request);

        //When
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        Exception exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(request)
        );

        //Then
        String expected = "User with email: " + request.getEmail()
                + " exists!Unable complete registration!";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findByEmail(request.getEmail());
    }

    @Test
    @DisplayName("Register user by non exits role in database, should return exception")
    void registerUser_ByNonExistsRoleInDatabase_ShouldReturnException() {
        //Given
        UserRegisterRequestDto request = createRegisterRequest();
        User user = createUserFromRequest(11L, request);

        //When
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.register(request)
        );

        //Then
        String expected = "Can't find role";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(roleRepository, times(1))
                .findByName(Role.RoleName.ROLE_USER);
    }

    @Test
    @DisplayName("Get profile with authenticated user, should return UserResponseDto")
    void getProfile_WithAuthenticatedUser_ShouldReturnUserResponseDto() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUser();
        UserResponseDto expected = createUserResponse(user);

        //When
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.getProfile();
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1))
                .findByUsername(user.getUsername());
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Get profile with unauthenticated user, should return exception")
    void getProfile_WithUnauthenticatedUser_ShouldReturnException() {
        //Given
        SecurityContextHolder.getContext().setAuthentication(null);

        //When
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.getProfile()
        );

        //Then
        String expected = "Unable to find authenticated user";
        String actual = exception.getMessage();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Get profile with non exists username, should return exception")
    void getProfile_WithNonExistsUsername_ShouldReturnException() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = createUser();

        //When
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getProfile()
        );

        //Then
        String expected = "Can't find user by username: " + authentication.getName();
        String actual = exception.getMessage();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Update role by valid user id and valid role name, should return UserResponseDto")
    void updateRole_ByValidIdAndRoleName_ShouldReturnUserResponseDto() {
        //Given
        Role roleAdmin = new Role();
        roleAdmin.setName(Role.RoleName.ROLE_ADMIN);
        Role roleUser = new Role();
        roleUser.setName(Role.RoleName.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(roleAdmin);
        roles.add(roleUser);
        User updatedUser = createUser();
        updatedUser.setRoles(roles);
        UserResponseDto expected = createUserResponse(updatedUser);
        User user = createUser();
        String roleName = "ROLE_ADMIN";

        //When
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.updateRoleById(user.getId(), Role.RoleName.ROLE_ADMIN))
                .thenReturn(updatedUser);
        when(roleRepository.findByName(roleAdmin.getName())).thenReturn(Optional.of(roleAdmin));
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateRole(user.getId(), roleName);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findById(user.getId());
        verify(userRepository, times(1))
                .updateRoleById(user.getId(), Role.RoleName.ROLE_ADMIN);
        verify(roleRepository, times(1)).findByName(roleAdmin.getName());
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    @DisplayName("Update role by invalid user id, should return exception")
    void updateRole_ByInvalidUserId_ShouldReturnException() {
        //Given
        Long id = 999L;
        String roleName = "ADMIN";

        //When
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateRole(id, roleName)
        );

        //Then
        String expected = "Can't find user by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update role with exists user role, should return UserResponseDto")
    void updateRole_WithExistsUserRole_ShouldReturnUserResponseDto() {
        //Given
        User user = createUser();
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        UserResponseDto expected = createUserResponse(user);
        String roleName = "USER";

        //When
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateRole(user.getId(), roleName);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findById(user.getId());
        verify(roleRepository, times(1)).findByName(role.getName());
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Update role with invalid role name, should return exception")
    void updateRole_WithInvalidRoleName_ShouldReturnException() {
        //Given
        User user = createUser();
        String roleName = "INVALID";

        //When
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Exception exception = assertThrows(
                DataProcessingException.class,
                () -> userService.updateRole(user.getId(), roleName)
        );

        //Then
        String expected = "Incorrect role name entered, please enter correct data:";
        String actual = exception.getMessage();
        assertTrue(actual.contains(expected));
        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    @DisplayName("Update profile with authenticated user, should return UserResponseDto")
    void updateProfile_WithAuthenticatedUser_ShouldReturnUserResponseDto() {
        //Given
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserUpdateProfileRequest updateRequest = createUpdateRequest();
        User user = createUser();
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        user.setEmail(updateRequest.getEmail());
        user.setUsername(updateRequest.getUsername());
        user.setFirstName(updateRequest.getFirstName());
        user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        UserResponseDto expected = createUserResponse(user);

        //When
        when(userMapper.toDto(user)).thenReturn(expected);

        //Then
        UserResponseDto actual = userService.updateProfile(updateRequest);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(authentication, times(1)).getName();
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @DisplayName("Get by id with valid id should return user")
    void getById_WithValidId_ShouldReturnUser() {
        //Given
        User expected = createUser();
        Long id = 2L;

        //When
        when(userRepository.findById(id)).thenReturn(Optional.of(expected));

        //Then
        User actual = userService.getById(id);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get by id with invalid id should return exception")
    void getById_WithInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getById(id)
        );

        //Then
        String expected = "Can't find user by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Exists by id with valid id should return true")
    void existsById_WithValidId_ShouldReturnTrue() {
        //Given
        User user = createUser();
        Long id = 2L;

        //When
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        //Then
        assertTrue(userService.existsById(id));
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Exists by id with invalid id should return exception")
    void existsById_WithInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.existsById(id)
        );

        //Then
        String expected = "Can't find user by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(userRepository, times(1)).findById(id);
    }

    private UserUpdateProfileRequest createUpdateRequest() {
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setEmail("update@example.com");
        request.setUsername("update");
        request.setFirstName("update");
        request.setPassword("Update=123456789");
        request.setRepeatPassword("Update=123456789");
        return request;

    }

    private User createUser() {
        User user = new User();
        user.setId(2L);
        user.setEmail("user1@example.com");
        user.setPassword("User=123456789");
        user.setUsername("user1");
        user.setFirstName("user1");
        user.setLastName("user1");
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private UserResponseDto createUserResponse(User user) {
        UserResponseDto response = new UserResponseDto();
        response.setEmail(user.getEmail());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    private User createUserFromRequest(Long id, UserRegisterRequestDto request) {
        User user = new User();
        user.setId(id);
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        Role role = new Role();
        role.setName(Role.RoleName.ROLE_USER);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private UserRegisterRequestDto createRegisterRequest() {
        UserRegisterRequestDto request = new UserRegisterRequestDto();
        request.setEmail("newUser@example.com");
        request.setPassword("User=123456789");
        request.setRepeatPassword("User=123456789");
        request.setUsername("newUser");
        request.setFirstName("newUser");
        request.setLastName("newUser");
        return request;
    }
}
