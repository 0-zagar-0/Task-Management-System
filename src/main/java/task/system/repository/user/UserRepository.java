package task.system.repository.user;

import java.util.Optional;
import task.system.model.User;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    void updateUser(User user);
}
