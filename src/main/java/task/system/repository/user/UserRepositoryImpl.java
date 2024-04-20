package task.system.repository.user;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import task.system.model.User;

@Repository
public class UserRepositoryImpl implements UserRepository {
    @Override
    public User save(final User user) {
        return null;
    }

    @Override
    public Optional<User> findById(final Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return Optional.empty();
    }
}
