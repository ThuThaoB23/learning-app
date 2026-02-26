CREATE TABLE user_activity_logs (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id CHAR(36) NULL,
    metadata JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_activity_logs_user_id (user_id),
    KEY idx_user_activity_logs_activity_type (activity_type),
    KEY idx_user_activity_logs_created_at (created_at),
    KEY idx_user_activity_logs_user_created (user_id, created_at),
    KEY idx_user_activity_logs_target (target_type, target_id),
    CONSTRAINT fk_user_activity_logs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
