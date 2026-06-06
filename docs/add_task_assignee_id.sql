ALTER TABLE assignment2_tasks
    ADD COLUMN assignee_id BIGINT NULL AFTER owner;

UPDATE assignment2_tasks AS task
JOIN assignment2_members AS member
    ON member.workspace_id = task.workspace_id
    AND member.name = task.owner
SET task.assignee_id = member.id
WHERE task.assignee_id IS NULL;
