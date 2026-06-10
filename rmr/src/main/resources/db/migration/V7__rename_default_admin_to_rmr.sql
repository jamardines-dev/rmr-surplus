-- RMR SURPLUS default admin account.
-- Username: rmr
-- Password remains: admin123
UPDATE users
SET username = 'rmr'
WHERE username = 'admin'
  AND role = 'ADMIN';
