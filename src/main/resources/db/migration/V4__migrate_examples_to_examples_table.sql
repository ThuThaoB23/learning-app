INSERT INTO vocabulary_examples (id, vocabulary_id, example, created_at, updated_at)
SELECT
    UUID(),
    v.id,
    v.example,
    v.created_at,
    v.updated_at
FROM vocabularies v
WHERE v.example IS NOT NULL AND v.example <> '';

ALTER TABLE vocabularies DROP COLUMN example;
