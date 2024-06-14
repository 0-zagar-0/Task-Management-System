UPDATE tasks
SET name = 'task2', description = 'description2', priority = 'MEDIUM', status = 'IN_PROGRESS',
    due_date = '2025-07-02', project_id = 1, assignee_id = 3, is_deleted = FALSE
WHERE id = 2;
