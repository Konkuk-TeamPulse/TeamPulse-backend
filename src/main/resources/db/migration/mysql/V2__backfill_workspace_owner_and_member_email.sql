SET @workspace_owner_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment2_workspaces'
      AND COLUMN_NAME = 'owner_email'
);

SET @workspace_owner_sql = IF(
    @workspace_owner_column_exists = 0,
    'ALTER TABLE assignment2_workspaces ADD COLUMN owner_email VARCHAR(191) NOT NULL DEFAULT ''''',
    'SELECT 1'
);
PREPARE workspace_owner_stmt FROM @workspace_owner_sql;
EXECUTE workspace_owner_stmt;
DEALLOCATE PREPARE workspace_owner_stmt;

SET @member_email_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment2_members'
      AND COLUMN_NAME = 'email'
);

SET @member_email_sql = IF(
    @member_email_column_exists = 0,
    'ALTER TABLE assignment2_members ADD COLUMN email VARCHAR(191) NOT NULL DEFAULT ''''',
    'SELECT 1'
);
PREPARE member_email_stmt FROM @member_email_sql;
EXECUTE member_email_stmt;
DEALLOCATE PREPARE member_email_stmt;
