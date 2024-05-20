package task.system.service.user;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
import task.system.telegram.TaskSystemBot;

@Service
public class UserServiceImpl implements UserService {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);
    private static final String PATTERN_OF_PASSWORD =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TaskSystemBot taskSystemBot;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository, TaskSystemBot taskSystemBot) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.taskSystemBot = taskSystemBot;
    }

    @Transactional
    @Override
    public UserResponseDto register(final UserRegisterRequestDto requestDto) {
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            LOGGER.warn("User with email: {} exists!, Unable complete registration!",
                    requestDto.getEmail());
            throw new RegistrationException("User with email: " + requestDto.getEmail()
                    + " exists!Unable complete registration!");
        }

        User user = userMapper.toEntity(requestDto);
        Role role = roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow(
                () -> new DataProcessingException("Can't find role")
        );
        user.getRoles().add(role);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        User savedUser = userRepository.save(user);
        String message = "User with email: " + user.getEmail() + " completed registration";
        LOGGER.info(message);
        taskSystemBot.sendMessage(message);
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponseDto getProfile() {
        User authenticatedUser = getAuthenticatedUser();
        return userMapper.toDto(authenticatedUser);
    }

    @Override
    public UserResponseDto updateRole(final Long id, final String roleName) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by id: " + id)
        );
        Role role = parseAndCheckValidRole(roleName);

        if (user.getRoles().contains(role)) {
            return userMapper.toDto(user);
        }

        user.getRoles().add(role);
        userRepository.updateRoleById(id, role.getName());
        LOGGER.info("User with email: {} updated role to: {}",
                user.getEmail(), role.getName().name()
        );
        return userMapper.toDto(user);
    }

    @Override
    public UserResponseDto updateProfile(final UserUpdateProfileRequest updateRequest) {
        User user = getAuthenticatedUser();
        Optional.ofNullable(updateRequest.getEmail())
                .filter(em -> !em.equals(user.getEmail()))
                .ifPresent(user::setEmail);
        Optional.ofNullable(updateRequest.getPassword())
                .filter(pwd -> !pwd.equals(user.getPassword())
                        && Pattern.compile(PATTERN_OF_PASSWORD)
                        .matcher(pwd)
                        .matches())
                .map(passwordEncoder::encode)
                .ifPresent(user::setPassword);
        Optional.ofNullable(updateRequest.getUsername())
                .filter(un -> !un.equals(user.getUsername()))
                .ifPresent(user::setUsername);
        Optional.ofNullable(updateRequest.getFirstName())
                .filter(fn -> !fn.equals(user.getFirstName()))
                .ifPresent(user::setFirstName);
        Optional.ofNullable(updateRequest.getLastName())
                .filter(ln -> !ln.equals(user.getLastName()))
                .ifPresent(user::setLastName);
        userRepository.updateUser(user);
        return userMapper.toDto(user);
    }

    @Override
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new DataProcessingException("Unable to find authenticated user");
        }

        return userRepository.findByUsername(authentication.getName()).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find user by username: " + authentication.getName()
                )
        );
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by id: " + id)
        );
    }

    private Role parseAndCheckValidRole(String roleName) {
        StringBuilder roleMessage = new StringBuilder();
        Role role = null;
        String requestRole = roleName.toUpperCase().contains(ROLE_PREFIX)
                ? roleName.trim().toUpperCase() : ROLE_PREFIX + roleName.trim().toUpperCase();

        for (Role.RoleName rol : Role.RoleName.values()) {
            roleMessage.append(", ")
                    .append(rol.name().substring(5))
                    .append(" or ")
                    .append(rol.name());

            if (requestRole.equals(rol.name())) {
                role = roleRepository.findByName(rol).get();
            }
        }

        if (role == null) {
            throw new DataProcessingException(
                    "Incorrect role name entered, please enter correct data:"
                            + roleMessage.substring(1)
            );
        }

        return role;
    }
}
