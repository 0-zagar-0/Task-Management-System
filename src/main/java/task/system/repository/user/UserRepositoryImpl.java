package task.system.repository.user;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Role;
import task.system.model.User;
import task.system.repository.role.RoleRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOGGER = LogManager.getLogger(UserRepositoryImpl.class);

    private final SessionFactory sessionFactory;
    private final RoleRepository roleRepository;

    @Autowired
    public UserRepositoryImpl(SessionFactory sessionFactory, RoleRepository roleRepository) {
        this.sessionFactory = sessionFactory;
        this.roleRepository = roleRepository;
    }

    @Override
    public User save(User user) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.info("Can't save user by email: {}", user.getEmail());
            throw new DataProcessingException("Can't save user: " + user, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<User> query = session.createQuery(
                    "FROM User u "
                            + "LEFT JOIN FETCH u.roles "
                            + "WHERE u.id = :id "
                            + "AND u.isDeleted = FALSE", User.class
            );
            query.setParameter("id", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            LOGGER.info("Can't find user by id: {}", id);
            throw new EntityNotFoundException("Can't find user by id: " + id);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<User> query = session.createQuery(
                    "FROM User u "
                            + "LEFT JOIN FETCH u.roles "
                            + "WHERE u.email = :email "
                            + "AND u.isDeleted = FALSE ", User.class
            );
            query.setParameter("email", email);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            LOGGER.info("Can't find user by email: {}", email);
            throw new DataProcessingException("Can't find user by email: " + email, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<User> query = session.createQuery(
                    "FROM User u "
                            + "LEFT JOIN FETCH u.roles "
                            + "WHERE u.username = :name "
                            + "AND u.isDeleted = FALSE ", User.class
            );
            query.setParameter("name", username);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            LOGGER.info("Can't find user by username: {}", username);
            throw new DataProcessingException("Can't find user by username: " + username, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public void updateUser(User user) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.info("Can't update user by email: {}", user.getEmail());
            throw new DataProcessingException(
                    "Can't update user by email: " + user.getEmail(), e
            );
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public User updateRoleById(Long id, Role.RoleName roleName) {
        User user = findById(id).get();
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Role role = roleRepository.findByName(roleName).get();
            user.getRoles().add(role);
            User mergedUser = session.merge(user);
            transaction.commit();
            return mergedUser;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.info("Can't update role: {}, for user with email:{}",
                    roleName.name(), user.getEmail())
            ;
            throw new DataProcessingException("Can't update role: " + roleName.name()
                    + ", for user with email:" + user.getEmail());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
