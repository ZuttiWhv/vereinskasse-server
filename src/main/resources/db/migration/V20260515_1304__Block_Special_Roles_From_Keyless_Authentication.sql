ALTER TABLE role
    ADD COLUMN force_password_login BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE role SET force_password_login = TRUE WHERE name = 'ADMIN';