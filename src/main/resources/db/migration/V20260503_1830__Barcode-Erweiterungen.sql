-- Migration: Produkte und User um Barcode-Felder erweitern

ALTER TABLE product ADD COLUMN barcode VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN barcode VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN barcode_login_enabled BOOLEAN DEFAULT FALSE;

-- Indizes für schnelle Suche beim Scannen
CREATE INDEX idx_product_barcode ON product (barcode);
CREATE INDEX idx_user_barcode ON users (barcode);

ALTER TABLE app_settings ADD COLUMN allow_barcode_login BOOLEAN DEFAULT FALSE;

INSERT INTO PERMISSION (NAME) values ('EXPORT_USER_BARCODES');
