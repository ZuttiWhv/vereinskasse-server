CREATE TABLE settings (
                          setting_key VARCHAR(50) PRIMARY KEY,
                          setting_value VARCHAR(255)
);

INSERT INTO settings (setting_key, setting_value) VALUES ('primary_color', '#2563eb');
INSERT INTO settings (setting_key, setting_value) VALUES ('logo_path', '/uploads/logo_default.png');