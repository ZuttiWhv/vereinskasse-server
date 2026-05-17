-- 1. Neue Hilfstabelle für die Barcodes erstellen
CREATE TABLE product_barcodes (
                                  product_id BIGINT NOT NULL,
                                  barcode VARCHAR(255) NOT NULL,
                                  CONSTRAINT fk_product_barcodes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
                                  CONSTRAINT uq_product_barcode UNIQUE (barcode)
);

-- 2. Daten-Migration: Bestehende Barcodes aus der Haupttabelle in die neue Tabelle retten
INSERT INTO product_barcodes (product_id, barcode)
SELECT id, barcode
FROM products
WHERE barcode IS NOT NULL AND TRIM(barcode) != '';

-- 3. Die alte, verwaiste Spalte aus der Haupttabelle entfernen
ALTER TABLE products DROP COLUMN barcode;