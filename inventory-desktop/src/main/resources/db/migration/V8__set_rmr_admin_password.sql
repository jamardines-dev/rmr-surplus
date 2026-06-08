-- RMR SURPLUS admin password.
-- Username: rmr
-- Password: @rmradmin
UPDATE users
SET password_hash = '$2a$10$DLxx0dnszCd27iwwjxaLYeQa1/4zWuDLfZ4QBVnQC/rDQDQGgzBXe'
WHERE username = 'rmr'
  AND role = 'ADMIN';
