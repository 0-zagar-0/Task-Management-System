ALTER SEQUENCE projects_id_seq RESTART WITH 2;

INSERT INTO projects
    (id, name, description, main_user_id, start_date, end_date, status, is_deleted)
VALUES (1, 'project1', 'description1', 1, '2025-06-08', '2025-07-08', 'INITIATED', false);
