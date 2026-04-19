CREATE TABLE app_settings (
                              id BIGINT PRIMARY KEY,
                              primary_color VARCHAR(10) NOT NULL,
                              secondary_color VARCHAR(10) NOT NULL,
                              nav_text_color VARCHAR(10) NOT NULL,
                              logo_path VARCHAR(255),
                              verein_name VARCHAR(100) NOT NULL,
                            quick_login boolean NOT NULL DEFAULT FALSE,
                            pin_login boolean NOT NULL DEFAULT FALSE
);

-- Den ersten Datensatz direkt anlegen
INSERT INTO app_settings (id, primary_color, secondary_color, nav_text_color,verein_name, quick_login,pin_login)
VALUES (1, '#2563eb', '#1e40af', '#ffffff','Vereinskasse',false,false);