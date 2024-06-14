INSERT INTO
    tasks (id, name, description, priority, status, due_date, project_id, assignee_id, is_deleted)
VALUES (1, 'task1', 'description1', 'LOW', 'IN_PROGRESS', '2025-07-02', 1, 4, false);

INSERT INTO
    tasks (id, name, description, priority, status, due_date, project_id, assignee_id, is_deleted)
VALUES (2, 'task2', 'description2', 'MEDIUM', 'IN_PROGRESS', '2025-07-02', 1, 3, false);

INSERT INTO
    tasks (id, name, description, priority, status, due_date, project_id, assignee_id, is_deleted)
VALUES (3, 'task3', 'description3', 'HIGH', 'IN_PROGRESS', '2025-07-02', 1, 4, false);

ALTER SEQUENCE tasks_id_seq RESTART WITH 4;
