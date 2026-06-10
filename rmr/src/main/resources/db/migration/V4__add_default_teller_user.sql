-- Development teller account.
-- Username: teller
-- Password: teller123
-- Replace this password hash before production use.
INSERT INTO users (username, password_hash, role, active, created_at)
VALUES (
    'teller',
    '$2a$10$MJn/ipJQ/f9IVPZyLb10OuV7QjG3SFP6/TYe0jrlyFc1KevnCKSfu',
    'TELLER',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username)
DO UPDATE SET
    role = EXCLUDED.role,
    active = TRUE;
