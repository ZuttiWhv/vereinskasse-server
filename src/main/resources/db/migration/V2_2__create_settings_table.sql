CREATE TABLE app_settings (
                              id BIGINT PRIMARY KEY,
                              primary_color VARCHAR(10) NOT NULL,
                              secondary_color VARCHAR(10) NOT NULL,
                              logo_path VARCHAR(255),
                              verein_name VARCHAR(100) NOT NULL
);

-- Den ersten Datensatz direkt anlegen
INSERT INTO app_settings (id, primary_color, secondary_color, verein_name)
VALUES (1, '#2563eb', '#1e40af', 'Vereinskasse');