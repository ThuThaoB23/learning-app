CREATE TABLE vocabulary_contributions (
    id CHAR(36) NOT NULL,
    contributor_user_id CHAR(36) NOT NULL,
    term VARCHAR(255) NOT NULL,
    term_normalized VARCHAR(255) NOT NULL,
    definition TEXT NOT NULL,
    definition_vi TEXT NULL,
    phonetic VARCHAR(100) NULL,
    part_of_speech VARCHAR(50) NULL,
    language VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by CHAR(36) NULL,
    reviewed_at DATETIME NULL,
    review_note TEXT NULL,
    reject_reason VARCHAR(50) NULL,
    approved_vocabulary_id CHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocab_contrib_status (status),
    KEY idx_vocab_contrib_created_at (created_at),
    KEY idx_vocab_contrib_contributor (contributor_user_id),
    KEY idx_vocab_contrib_term_lang (term_normalized, language),
    KEY idx_vocab_contrib_reviewed_by (reviewed_by),
    KEY idx_vocab_contrib_approved_vocab (approved_vocabulary_id),
    CONSTRAINT fk_vocab_contrib_user FOREIGN KEY (contributor_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_contrib_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_vocab_contrib_vocab FOREIGN KEY (approved_vocabulary_id) REFERENCES vocabularies(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE vocabulary_contribution_examples (
    id CHAR(36) NOT NULL,
    contribution_id CHAR(36) NOT NULL,
    example TEXT NOT NULL,
    position INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocab_contrib_example_contrib_id (contribution_id),
    CONSTRAINT fk_vocab_contrib_examples_contrib FOREIGN KEY (contribution_id)
        REFERENCES vocabulary_contributions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE vocabulary_contribution_topics (
    contribution_id CHAR(36) NOT NULL,
    topic_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contribution_id, topic_id),
    KEY idx_vocab_contrib_topic_contrib_id (contribution_id),
    KEY idx_vocab_contrib_topic_topic_id (topic_id),
    CONSTRAINT fk_vocab_contrib_topics_contrib FOREIGN KEY (contribution_id)
        REFERENCES vocabulary_contributions(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_contrib_topics_topic FOREIGN KEY (topic_id)
        REFERENCES topics(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE vocabulary_contribution_review_logs (
    id CHAR(36) NOT NULL,
    contribution_id CHAR(36) NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_user_id CHAR(36) NOT NULL,
    note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocab_contrib_review_log_contrib_id (contribution_id),
    KEY idx_vocab_contrib_review_log_actor_id (actor_user_id),
    CONSTRAINT fk_vocab_contrib_review_logs_contrib FOREIGN KEY (contribution_id)
        REFERENCES vocabulary_contributions(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_contrib_review_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
