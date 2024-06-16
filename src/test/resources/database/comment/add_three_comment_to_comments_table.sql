INSERT INTO comments (id, task_id, user_id, text, timestamp, is_deleted)
VALUES (1, 1, 3, 'comment1', '2025-06-20 15:57:16.822872', false);

INSERT INTO comments (id, task_id, user_id, text, timestamp, is_deleted)
VALUES (2, 1, 4, 'comment2', '2025-06-21 15:57:16.822872', false);

INSERT INTO comments (id, task_id, user_id, text, timestamp, is_deleted)
VALUES (3, 2, 2, 'comment3', '2025-06-23 15:57:16.822872', false);

ALTER SEQUENCE comments_id_seq RESTART WITH 4;
