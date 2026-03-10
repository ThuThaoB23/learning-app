CREATE TABLE user_feedbacks (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(120) NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    source_screen VARCHAR(120) NULL,
    app_version VARCHAR(50) NULL,
    device_info VARCHAR(500) NULL,
    locale VARCHAR(20) NULL,
    read_by CHAR(36) NULL,
    read_at DATETIME NULL,
    archived_by CHAR(36) NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_feedback_user_id (user_id),
    KEY idx_user_feedback_status (status),
    KEY idx_user_feedback_category (category),
    KEY idx_user_feedback_created_at (created_at),
    CONSTRAINT fk_user_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_feedback_read_by FOREIGN KEY (read_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_user_feedback_archived_by FOREIGN KEY (archived_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE user_feedback_attachments (
    id CHAR(36) NOT NULL,
    feedback_id CHAR(36) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    position INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_feedback_attachment_feedback_id (feedback_id),
    CONSTRAINT fk_user_feedback_attachment_feedback FOREIGN KEY (feedback_id)
        REFERENCES user_feedbacks(id) ON DELETE CASCADE
) ENGINE=InnoDB;
