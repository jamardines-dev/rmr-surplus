UPDATE users
SET role = 'EMPLOYEE'
WHERE role = 'TELLER';

INSERT INTO users (username, password_hash, role, active, created_at)
VALUES (
    'employee',
    '$2a$10$MJn/ipJQ/f9IVPZyLb10OuV7QjG3SFP6/TYe0jrlyFc1KevnCKSfu',
    'EMPLOYEE',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username)
DO UPDATE SET
    role = EXCLUDED.role,
    active = TRUE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS model_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER NOT NULL DEFAULT 0;

UPDATE products
SET model_code = 'MODEL-' || id
WHERE model_code IS NULL OR TRIM(model_code) = '';

ALTER TABLE products
    ALTER COLUMN model_code SET NOT NULL;

DROP INDEX IF EXISTS idx_products_model_code;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_model_code ON products(model_code);
