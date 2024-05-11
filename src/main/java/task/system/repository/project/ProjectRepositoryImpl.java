package task.system.repository.project;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Project;

@Repository
public class ProjectRepositoryImpl implements ProjectRepository {
    private final SessionFactory sessionFactory;

    public ProjectRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Project save(Project project) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(project);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't save project: " + project);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

        return project;
    }

    @Override
    public List<Project> findAllProjectsByUserId(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                    "SELECT p FROM Project p "
                            + "JOIN p.users u "
                            + "WHERE u.id = :userId "
                            + "AND p.isDeleted = FALSE",
                    Project.class)
                    .setParameter("userId", id)
                    .list();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find projects by user id" + id + " | "
                    + e.getMessage()
            );
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                    "FROM Project p "
                            + "LEFT JOIN FETCH p.mainUser "
                            + "LEFT JOIN FETCH p.users "
                            + "WHERE p.id = :id "
                            + "AND p.isDeleted = FALSE ", Project.class)
                    .setParameter("id", id)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find project by id: " + id);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Project update(Project project) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Project mergedProject = session.merge(project);
            transaction.commit();
            return mergedProject;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't update project: " + project);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            int rowsUpdated = session.createQuery(
                    "UPDATE Project p "
                            + " SET p.isDeleted = TRUE "
                            + " WHERE p.id = :projectId AND p.isDeleted = FALSE")
                    .setParameter("projectId", id)
                    .executeUpdate();

            if (rowsUpdated == 0) {
                throw new DataProcessingException("Project with id:" + id + " not found.");
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't delete project by id:" + id + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
