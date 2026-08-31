-- 1. user_id in sales optional machen
ALTER TABLE sales ALTER COLUMN user_id DROP NOT NULL;

-- 2. Neue Spalten für Gast-Verkäufe hinzufügen
ALTER TABLE sales ADD COLUMN guest_name VARCHAR(255);
ALTER TABLE sales ADD COLUMN host_id BIGINT;
ALTER TABLE sales ADD COLUMN payment_status VARCHAR(50) DEFAULT 'COMPLETED' NOT NULL;

-- Foreign Key für das bürgende Mitglied setzen
ALTER TABLE sales
    ADD CONSTRAINT fk_sales_host
        FOREIGN KEY (host_id) REFERENCES users(id);

