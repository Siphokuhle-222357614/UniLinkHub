-- Give order_items a primary key on databases that were adopted at V1 instead of running it (they
-- were created by Hibernate's old ddl-auto=update, which made the table without one). Databases
-- created by V1 already have the key, so this only acts when the column is missing.
SET @needs_key := (
    SELECT COUNT(*) = 0 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'order_items' AND column_name = 'id'
);
SET @ddl := IF(@needs_key,
    'ALTER TABLE order_items ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
