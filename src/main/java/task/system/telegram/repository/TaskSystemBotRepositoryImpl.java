package task.system.telegram.repository;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.telegram.model.TaskSystemBotChat;

@Repository
public class TaskSystemBotRepositoryImpl implements TaskSystemBotRepository {
    private final SessionFactory sessionFactory;

    public TaskSystemBotRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(TaskSystemBotChat botChat) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(botChat);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't insert TaskSystemBotChat: " + botChat);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<TaskSystemBotChat> findByChatId(Long chatId) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<TaskSystemBotChat> findQuery = session.createQuery("FROM TaskSystemBotChat bc "
                    + "WHERE bc.chatId = :chatId AND bc.isDeleted = FALSE",
                    TaskSystemBotChat.class);
            findQuery.setParameter("chatId", chatId);
            return findQuery.uniqueResultOptional();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find TaskSystemBotChat by id:" + chatId, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<TaskSystemBotChat> findAll() {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<TaskSystemBotChat> findAllQuery = session.createQuery(
                    "FROM TaskSystemBotChat bc "
                            + "WHERE bc.isDeleted = FALSE", TaskSystemBotChat.class
            );
            return findAllQuery.getResultList();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find any TaskSystemBotChat");
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
