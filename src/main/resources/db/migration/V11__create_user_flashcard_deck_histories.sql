CREATE TABLE IF NOT EXISTS user_flashcard_deck_histories (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    served_vocabulary_ids JSON NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_flashcard_hist_user_created (user_id, created_at),
    CONSTRAINT fk_flashcard_hist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
