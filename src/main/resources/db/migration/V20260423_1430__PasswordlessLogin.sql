-- Spalte hinzufügen mit Default-Wert 'false'
ALTER TABLE users
    ADD COLUMN passwordless_login_enabled BOOLEAN NOT NULL DEFAULT FALSE;
