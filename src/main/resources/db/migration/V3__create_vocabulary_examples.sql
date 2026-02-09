CREATE TABLE vocabulary_examples (
    id CHAR(36) NOT NULL,
    vocabulary_id CHAR(36) NOT NULL,
    example TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocab_example_vocab_id (vocabulary_id),
    CONSTRAINT fk_vocab_example_vocab FOREIGN KEY (vocabulary_id) REFERENCES vocabularies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
