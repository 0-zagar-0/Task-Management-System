package task.system.repository.task;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Task;

@Repository
public class TaskRepositoryImpl implements TaskRepository {
    private final SessionFactory sessionFactory;

    public TaskRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Task save(Task task) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(task);
            transaction.commit();
            return task;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't save task: " + task);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Task> findAllByProjectId(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Task> getAllQuery = session.createQuery(
                    "FROM Task t "
                            + "WHERE t.projectId = :projectId AND t.isDeleted = FALSE", Task.class);
            getAllQuery.setParameter("projectId", id);
            return getAllQuery.getResultList();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find tasks from project by id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Task> findById(Long id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Task task = session.find(Task.class, id);
            return Optional.ofNullable(task);
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find task by id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Task update(Task taskFromDb) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Task updatedTask = session.merge(taskFromDb);
            transaction.commit();;
            return updatedTask;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't update task: " + taskFromDb);
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
                    "UPDATE Task t "
                            + "SET t.isDeleted = TRUE "
                            + "WHERE t.id = :taskId AND t.isDeleted = FALSE "
                    )
                    .setParameter("taskId", id)
                    .executeUpdate();

            if (rowsUpdated == 0) {
                throw new EntityNotFoundException("Task with id: " + id + " not found.");
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't delete task by id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

    }
}
