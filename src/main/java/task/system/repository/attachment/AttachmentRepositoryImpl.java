package task.system.repository.attachment;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Attachment;

@Repository
public class AttachmentRepositoryImpl implements AttachmentRepository {
    private final SessionFactory sessionFactory;

    public AttachmentRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Attachment save(Attachment attachment) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(attachment);
            transaction.commit();
            return attachment;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't insert attachment: " + attachment, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Attachment> findByFileName(String filename) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Attachment> query = session.createQuery(
                    "FROM Attachment a WHERE a.filename = :name", Attachment.class
            );
            return query.setParameter("name", filename).uniqueResultOptional();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find attachment by filename: " + filename);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Attachment> findById(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Attachment attachment = session.find(Attachment.class, id);
            return Optional.ofNullable(attachment);
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find attachment by id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Attachment> findAllByTaskId(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            List<Attachment> attachments = session.createQuery(
                    "FROM Attachment a WHERE a.taskId = :taskId", Attachment.class)
                    .setParameter("taskId", id)
                    .getResultList();
            return attachments;
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find attachments by task id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
