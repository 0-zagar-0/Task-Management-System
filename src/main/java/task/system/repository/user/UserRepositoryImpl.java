package task.system.repository.user;

import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import task.system.model.User;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public UserRepositoryImpl(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public User save(User user) {
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
