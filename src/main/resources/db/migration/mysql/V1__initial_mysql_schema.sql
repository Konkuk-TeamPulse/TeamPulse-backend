CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(191) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(80) NOT NULL,
    university VARCHAR(80) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    access_token VARCHAR(512) NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    revoked BIT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_sessions_access_token (access_token),
    UNIQUE KEY uk_auth_sessions_refresh_token (refresh_token),
    KEY idx_auth_sessions_user_id (user_id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_workspaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    initialized BIT NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    user_email VARCHAR(191) NOT NULL,
    user_university VARCHAR(80) NOT NULL,
    user_phone VARCHAR(30) NOT NULL,
    owner_email VARCHAR(191) NOT NULL DEFAULT '',
    team_name VARCHAR(80) NOT NULL,
    course_name VARCHAR(80) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    due_date VARCHAR(10) NOT NULL,
    description VARCHAR(500),
    start_date VARCHAR(10) NOT NULL,
    invite_code VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_workspaces_owner_email (owner_email),
    KEY idx_assignment2_workspaces_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(191) NOT NULL DEFAULT '',
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_members_workspace_id (workspace_id),
    KEY idx_assignment2_members_email (email),
    CONSTRAINT fk_assignment2_members_workspace FOREIGN KEY (workspace_id) REFERENCES assignment2_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    owner VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    due_date VARCHAR(10) NOT NULL,
    blockers TEXT NOT NULL,
    next_steps TEXT NOT NULL,
    note VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_tasks_workspace_id (workspace_id),
    CONSTRAINT fk_assignment2_tasks_workspace FOREIGN KEY (workspace_id) REFERENCES assignment2_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_meetings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    time VARCHAR(16) NOT NULL,
    agenda TEXT NOT NULL,
    content TEXT NOT NULL,
    decisions TEXT NOT NULL,
    actions TEXT NOT NULL,
    attendee_ids TEXT NOT NULL,
    action_items TEXT NOT NULL,
    created_at VARCHAR(32) NOT NULL,
    updated_at VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_meetings_workspace_id (workspace_id),
    CONSTRAINT fk_assignment2_meetings_workspace FOREIGN KEY (workspace_id) REFERENCES assignment2_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    actor VARCHAR(50) NOT NULL,
    logged_at VARCHAR(16) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_activities_workspace_id (workspace_id),
    CONSTRAINT fk_assignment2_activities_workspace FOREIGN KEY (workspace_id) REFERENCES assignment2_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment2_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    label VARCHAR(180) NOT NULL,
    range_value VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_assignment2_reports_workspace_id (workspace_id),
    CONSTRAINT fk_assignment2_reports_workspace FOREIGN KEY (workspace_id) REFERENCES assignment2_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
