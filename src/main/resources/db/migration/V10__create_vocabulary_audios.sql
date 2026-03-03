CREATE TABLE IF NOT EXISTS vocabulary_audios (
    id CHAR(36) NOT NULL,
    vocabulary_id CHAR(36) NOT NULL,
    audio_url VARCHAR(1000) NOT NULL,
    accent VARCHAR(20) NULL,
    position INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocab_audio_vocab_id (vocabulary_id),
    KEY idx_vocab_audio_vocab_position (vocabulary_id, position),
    CONSTRAINT fk_vocab_audio_vocab FOREIGN KEY (vocabulary_id) REFERENCES vocabularies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
