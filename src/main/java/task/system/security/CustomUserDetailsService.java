package task.system.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import task.system.exception.EntityNotFoundException;
import task.system.repository.user.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail).orElseThrow(
                    () -> new EntityNotFoundException("Can't find user by email: " + usernameOrEmail
            ));
        }

        return userRepository.findByUsername(usernameOrEmail).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by username: " + usernameOrEmail
        ));
    }
}
