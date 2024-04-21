package task.system.service.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import task.system.model.User;
import task.system.repository.user.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(final UserRepository userRepository) {
        this.userRepository = userRepository;
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
        userRepository.findByEmail("email").orElseThrow(() -> new EntityNotFoundException("Can't "
                + "find user by email: "));
        return null;
    }
}
