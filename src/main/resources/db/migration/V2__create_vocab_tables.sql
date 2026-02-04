CREATE TABLE topics (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topics_slug (slug),
    KEY idx_topics_status (status),
    KEY idx_topics_deleted_at (deleted_at)
) ENGINE=InnoDB;

CREATE TABLE vocabularies (
    id CHAR(36) NOT NULL,
    term VARCHAR(255) NOT NULL,
    term_normalized VARCHAR(255) NOT NULL,
    definition TEXT NOT NULL,
    example TEXT NULL,
    phonetic VARCHAR(100) NULL,
    part_of_speech VARCHAR(50) NULL,
    language VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_by CHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vocab_term_language (term_normalized, language),
    KEY idx_vocab_term (term_normalized),
    KEY idx_vocab_language (language),
    KEY idx_vocab_status (status),
    KEY idx_vocab_deleted_at (deleted_at),
    CONSTRAINT fk_vocab_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE topic_vocabularies (
    topic_id CHAR(36) NOT NULL,
    vocabulary_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (topic_id, vocabulary_id),
    KEY idx_topic_vocab_topic_id (topic_id),
    KEY idx_topic_vocab_vocab_id (vocabulary_id),
    CONSTRAINT fk_topic_vocab_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_vocab_vocab FOREIGN KEY (vocabulary_id) REFERENCES vocabularies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_vocabularies (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    vocabulary_id CHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    progress INT NOT NULL DEFAULT 0,
    last_reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_vocab_user_vocab (user_id, vocabulary_id),
    KEY idx_user_vocab_user_id (user_id),
    KEY idx_user_vocab_vocab_id (vocabulary_id),
    KEY idx_user_vocab_status (status),
    KEY idx_user_vocab_last_reviewed (last_reviewed_at),
    CONSTRAINT fk_user_vocab_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_vocab_vocab FOREIGN KEY (vocabulary_id) REFERENCES vocabularies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
