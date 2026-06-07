-- Development admin account.
-- Username: admin
-- Password: admin123
-- Replace this password hash before production use.
INSERT INTO users (username, password_hash, role, active, created_at)
VALUES (
    'admin',
    '$2a$10$n/vX8ybpgEfSwftlJ1hXBO2aPHxurf5AitPmZ/x69BOxN1TXmZhwS',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username)
DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    active = TRUE;
