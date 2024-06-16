DELETE FROM comments WHERE id = 4;

ALTER SEQUENCE comments_id_seq RESTART WITH 4;
