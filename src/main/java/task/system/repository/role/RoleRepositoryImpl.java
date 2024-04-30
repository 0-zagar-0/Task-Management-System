package task.system.repository.role;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.EntityNotFoundException;
import task.system.model.Role;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    private final SessionFactory sessionFactory;

    public RoleRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<Role> findByName(Role.RoleName name) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Role> query = session.createQuery("FROM Role r WHERE r.name = :name ",
                    Role.class);
            query.setParameter("name", name);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find role by name: " + name);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
