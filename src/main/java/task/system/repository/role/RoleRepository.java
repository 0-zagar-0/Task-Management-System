package task.system.repository.role;

import java.util.Optional;
import task.system.model.Role;

public interface RoleRepository {
    Optional<Role> findByName(Role.RoleName name);
}
