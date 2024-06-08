ALTER SEQUENCE projects_id_seq RESTART WITH 2;

INSERT INTO projects
(id, name, description, main_user_id, start_date, end_date, status, is_deleted)
VALUES (2, 'project2', 'description2', 6, '2025-10-10', '2025-11-10', 'IN_PROGRESS', false);