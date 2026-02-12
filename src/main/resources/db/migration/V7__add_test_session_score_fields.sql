ALTER TABLE test_sessions
    ADD COLUMN total_items INT NOT NULL DEFAULT 0 AFTER completed_at,
    ADD COLUMN correct_count INT NOT NULL DEFAULT 0 AFTER total_items,
    ADD COLUMN wrong_count INT NOT NULL DEFAULT 0 AFTER correct_count,
    ADD COLUMN skipped_count INT NOT NULL DEFAULT 0 AFTER wrong_count,
    ADD COLUMN score INT NOT NULL DEFAULT 0 AFTER skipped_count;
