package task.system.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.exception.RegistrationException;
import task.system.mapper.UserMapper;
import task.system.model.User;
import task.system.repository.user.UserRepository;

@Service
public class UserServiceImpl implements UserService {
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
            throw new RegistrationException("Unable complete registration!");
        }

        User user = userMapper.toEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public Object getProfile() {
        return null;
    }

    @Override
    public Object updateRole(final Long id, final Object role) {
        userRepository.findById(id);
        return null;
    }

    @Override
    public Object updateProfile(final Object updateRequest) {
        return null;
    }

    @Override
    public User getAuthenticatedUser() {
        return null;
    }
}
