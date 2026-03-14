PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS user_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    account_status TEXT NOT NULL DEFAULT 'ACTIVE',
    profile_json TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_account_user_code
    ON user_account (user_code);

CREATE TABLE IF NOT EXISTS project_space (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_account_id INTEGER NOT NULL,
    space_code TEXT NOT NULL,
    space_name TEXT NOT NULL,
    memory_scope TEXT NOT NULL,
    description TEXT,
    settings_json TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_account_id) REFERENCES user_account (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_space_user_space_code
    ON project_space (user_account_id, space_code);

CREATE INDEX IF NOT EXISTS idx_project_space_memory_scope
    ON project_space (memory_scope);

CREATE TABLE IF NOT EXISTS chat_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_account_id INTEGER NOT NULL,
    project_space_id INTEGER NOT NULL,
    session_code TEXT NOT NULL,
    session_title TEXT,
    session_status TEXT NOT NULL DEFAULT 'ACTIVE',
    started_at INTEGER NOT NULL,
    ended_at INTEGER,
    last_message_at INTEGER,
    metadata_json TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    FOREIGN KEY (project_space_id) REFERENCES project_space (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_chat_session_session_code
    ON chat_session (session_code);

CREATE INDEX IF NOT EXISTS idx_chat_session_project_status
    ON chat_session (project_space_id, session_status);

CREATE INDEX IF NOT EXISTS idx_chat_session_user_last_message
    ON chat_session (user_account_id, last_message_at DESC);

CREATE TABLE IF NOT EXISTS message_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    project_space_id INTEGER NOT NULL,
    user_account_id INTEGER NOT NULL,
    sequence_no INTEGER NOT NULL,
    role TEXT NOT NULL,
    event_type TEXT NOT NULL,
    content_text TEXT,
    content_json TEXT,
    provider TEXT,
    model_name TEXT,
    request_id TEXT,
    token_usage_input INTEGER,
    token_usage_output INTEGER,
    event_time INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_session (id),
    FOREIGN KEY (project_space_id) REFERENCES project_space (id),
    FOREIGN KEY (user_account_id) REFERENCES user_account (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_message_event_session_sequence
    ON message_event (session_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_message_event_project_event_time
    ON message_event (project_space_id, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_message_event_role_type
    ON message_event (role, event_type);

CREATE TABLE IF NOT EXISTS memory_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_account_id INTEGER NOT NULL,
    project_space_id INTEGER NOT NULL,
    session_id INTEGER,
    source_message_event_id INTEGER,
    memory_scope TEXT NOT NULL,
    memory_type TEXT NOT NULL,
    title TEXT,
    summary TEXT NOT NULL,
    detail_text TEXT,
    tags_json TEXT,
    metadata_json TEXT,
    importance_score REAL NOT NULL DEFAULT 0,
    confidence_score REAL NOT NULL DEFAULT 0,
    last_accessed_at INTEGER,
    expires_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    FOREIGN KEY (project_space_id) REFERENCES project_space (id),
    FOREIGN KEY (session_id) REFERENCES chat_session (id),
    FOREIGN KEY (source_message_event_id) REFERENCES message_event (id)
);

CREATE INDEX IF NOT EXISTS idx_memory_record_scope_type
    ON memory_record (project_space_id, memory_scope, memory_type);

CREATE INDEX IF NOT EXISTS idx_memory_record_importance
    ON memory_record (project_space_id, importance_score DESC, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_memory_record_source_message
    ON memory_record (source_message_event_id);

CREATE TABLE IF NOT EXISTS conversation_summary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    project_space_id INTEGER NOT NULL,
    summary_version INTEGER NOT NULL,
    source_start_sequence INTEGER NOT NULL,
    source_end_sequence INTEGER NOT NULL,
    summary_text TEXT NOT NULL,
    provider TEXT,
    model_name TEXT,
    metadata_json TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_session (id),
    FOREIGN KEY (project_space_id) REFERENCES project_space (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_conversation_summary_session_version
    ON conversation_summary (session_id, summary_version);

CREATE INDEX IF NOT EXISTS idx_conversation_summary_session_range
    ON conversation_summary (session_id, source_end_sequence DESC);
