package task.system.service.user;

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
import task.system.model.User;
import task.system.repository.user.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);
    private static final String PATTERN_OF_PASSWORD =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDto register(final UserRegisterRequestDto requestDto) {
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            LOGGER.warn("User with email: {} exists!, Unable complete registration!",
                    requestDto.getEmail());
            throw new RegistrationException("User with email: " + requestDto.getEmail()
                    + " exists!Unable complete registration!");
        }

        User user = userMapper.toEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        User savedUser = userRepository.save(user);
        LOGGER.info("User with email: {} completed registration", user.getEmail());
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
        User.Role role = parseAndCheckValidRole(roleName);
        user.setRole(role);
        userRepository.updateUser(user);
        LOGGER.info("User with email: {} updated role to: {}", user.getEmail(), role.name());
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

        return userRepository.findByEmail(authentication.getName()).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find user by email: " + authentication.getName()
                )
        );
    }

    private User.Role parseAndCheckValidRole(String roleName) {
        StringBuilder roleMessage = new StringBuilder();
        User.Role role = null;
        String requestRole = roleName.toUpperCase().contains(ROLE_PREFIX)
                ? roleName.trim().toUpperCase() : ROLE_PREFIX + roleName.trim().toUpperCase();

        for (User.Role rol : User.Role.values()) {
            roleMessage
                    .append(", ")
                    .append(rol.name().substring(5))
                    .append(" or ")
                    .append(rol.name());

            if (requestRole.equals(rol.name())) {
                role = rol;
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
