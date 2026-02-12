ALTER TABLE user_vocabularies
    CHANGE COLUMN progress process INT NOT NULL DEFAULT 0;

ALTER TABLE user_vocabularies
    ADD COLUMN next_due_at DATETIME NULL AFTER last_reviewed_at,
    ADD COLUMN streak INT NOT NULL DEFAULT 0 AFTER next_due_at,
    ADD COLUMN right_count INT NOT NULL DEFAULT 0 AFTER streak,
    ADD COLUMN wrong_count INT NOT NULL DEFAULT 0 AFTER right_count;

CREATE INDEX idx_user_vocab_next_due_at ON user_vocabularies(next_due_at);
CREATE INDEX idx_user_vocab_process ON user_vocabularies(process);
CREATE INDEX idx_user_vocab_user_due ON user_vocabularies(user_id, next_due_at);

CREATE TABLE test_sessions (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    title VARCHAR(255) NULL,
    schedule_date DATE NULL,
    source_type VARCHAR(30) NOT NULL,
    source_ref_id CHAR(36) NULL,
    source_params JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_test_sessions_user_id (user_id),
    KEY idx_test_sessions_type (type),
    KEY idx_test_sessions_status (status),
    KEY idx_test_sessions_schedule_date (schedule_date),
    CONSTRAINT fk_test_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE test_items (
    id CHAR(36) NOT NULL,
    test_session_id CHAR(36) NOT NULL,
    user_vocab_id CHAR(36) NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    question_payload JSON NOT NULL,
    position INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_answer TEXT NULL,
    answered_at DATETIME NULL,
    time_ms INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_items_session_position (test_session_id, position),
    KEY idx_test_items_session_id (test_session_id),
    KEY idx_test_items_status (status),
    KEY idx_test_items_user_vocab_id (user_vocab_id),
    CONSTRAINT fk_test_items_session FOREIGN KEY (test_session_id) REFERENCES test_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_test_items_user_vocab FOREIGN KEY (user_vocab_id) REFERENCES user_vocabularies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
