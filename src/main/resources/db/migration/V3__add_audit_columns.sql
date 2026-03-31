-- ============================================================
-- V3: Thêm audit columns
-- Minh họa: schema evolution không cần sửa V1, V2
-- ============================================================

ALTER TABLE items
    ADD COLUMN created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE categories
    ADD COLUMN created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

COMMENT ON COLUMN items.version IS 'Optimistic locking version counter';
