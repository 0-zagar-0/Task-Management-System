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
import task.system.model.User;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOGGER = LogManager.getLogger(UserRepositoryImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public UserRepositoryImpl(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
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
            return Optional.ofNullable(session.get(User.class, id));
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
                    "FROM User u WHERE u.email = :email", User.class
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
}
