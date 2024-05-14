package task.system.repository.comment;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Comment;

@Repository
public class CommentRepositoryImpl implements CommentRepository {
    private final SessionFactory sessionFactory;

    public CommentRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Comment save(Comment comment) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(comment);
            transaction.commit();
            return comment;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't insert comment: " + comment + " to database");
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Comment> findAllByTaskId(Long taskId) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Comment> findQuery = session.createQuery(
                    "FROM Comment c "
                            + "WHERE c.taskId = :taskId AND c.isDeleted = FALSE", Comment.class)
                    .setParameter("taskId", taskId);
            return findQuery.getResultList();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find comments by task ID: " + taskId);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Comment> findById(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Comment comment = session.find(Comment.class, id);
            return Optional.ofNullable(comment);
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find comment by id: " + id);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Comment update(Comment comment) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Comment mergedComment = session.merge(comment);
            transaction.commit();
            return mergedComment;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't update comment by id: " + comment.getId());
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
            int updatedRows = session.createQuery(
                    "UPDATE Comment c "
                            + "SET c.isDeleted = TRUE "
                            + "WHERE c.id = :id AND c.isDeleted = FALSE ")
                    .setParameter("id", id)
                    .executeUpdate();

            if (updatedRows == 0) {
                throw new EntityNotFoundException("Can't find comment by id: " + id);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't delete comment by id: " + id);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
