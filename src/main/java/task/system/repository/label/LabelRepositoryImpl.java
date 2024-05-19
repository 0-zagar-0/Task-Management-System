package task.system.repository.label;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import task.system.exception.DataProcessingException;
import task.system.exception.EntityNotFoundException;
import task.system.model.Label;

@Repository
public class LabelRepositoryImpl implements LabelRepository {
    private final SessionFactory sessionFactory;

    public LabelRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Label save(Label label) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(label);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't insert label: " + label, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

        return label;
    }

    @Override
    public Optional<Label> findByNameAndColorAndProjectId(
            Label.Color color, String name, Long projectId
    ) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Label> findQuery = session.createQuery("FROM Label l "
                    + "WHERE l.color = :color AND l.name = :name AND l.projectId = :projectId "
                            + "AND l.isDeleted = FALSE", Label.class
            );
            findQuery.setParameter("color", color);
            findQuery.setParameter("name", name);
            findQuery.setParameter("projectId", projectId);
            return findQuery.uniqueResultOptional();
        } catch (Exception e) {
            throw new EntityNotFoundException(
                    "Can't find label by projectId: " + projectId + ", color: " + color
                            + ", name: " + name, e
            );
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Label> findAllByProjectId(Long projectId) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Label> labels = session.createQuery("FROM Label l "
                    + "WHERE l.projectId = :projectId OR l.projectId = NULL "
                            + "AND l.isDeleted = FALSE", Label.class
            );
            labels.setParameter("projectId", projectId);
            return labels.getResultList();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find labels by project ID: " + projectId, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Label> findById(Long id) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Label label = session.find(Label.class, id);
            return Optional.ofNullable(label);
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find label by id: " + id, e);
        } finally {
            if (session != null & session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Label update(Label label) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Label mergedLabel = session.merge(label);
            transaction.commit();
            return mergedLabel;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't update label by id: " + label, e);
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
            Query deleteQuery = session.createQuery("UPDATE Label l "
                    + "SET l.isDeleted = TRUE "
                    + "WHERE l.id = :id AND l.isDeleted= FALSE ");
            deleteQuery.setParameter("id", id);
            int updatedRows = deleteQuery.executeUpdate();

            if (updatedRows == 0) {
                throw new EntityNotFoundException("Can't find label by id: " + id);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            throw new DataProcessingException("Can't delete label by id: " + id, e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Label> findDefaultLabels() {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Label> findDefaultQuery = session.createQuery("FROM Label l "
                            + "WHERE l.projectId = NULL AND l.isDeleted = FALSE", Label.class
            );
            return findDefaultQuery.getResultList();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find default labels", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Label findDefaultGreyLabel() {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query<Label> findByColorQuery = session.createQuery("FROM Label l "
                            + "WHERE l.color = Color.GRAY AND l.name = NULL", Label.class
            );
            return findByColorQuery.uniqueResult();
        } catch (Exception e) {
            throw new EntityNotFoundException("Can't find default GREY label", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
