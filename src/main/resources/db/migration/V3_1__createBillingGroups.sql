-- 1. Tabelle für Abrechnungsgruppen erstellen
CREATE TABLE billing_groups (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                name VARCHAR(255) NOT NULL UNIQUE,
                                description TEXT,
                                allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE,
                                credit_limit BIGINT NOT NULL DEFAULT 0,
                                is_default BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. Eine Standard-Gruppe einfügen, damit bestehende User nicht im "Nichts" hängen
INSERT INTO billing_groups (name, description, allow_negative_balance, credit_limit, is_default)
VALUES ('Standard', 'Standard Abrechnungsgruppe ohne Dispo', FALSE, 0, TRUE);

-- 3. Die User-Tabelle um die Spalte billing_group_id erweitern
ALTER TABLE users
    ADD COLUMN billing_group_id BIGINT;

-- 4. Alle existierenden User der neuen Standard-Gruppe zuordnen
UPDATE users
SET billing_group_id = (SELECT id FROM billing_groups WHERE name = 'Standard');

-- 5. Den Foreign Key Constraint hinzufügen
ALTER TABLE users
    ADD CONSTRAINT fk_user_billing_group
        FOREIGN KEY (billing_group_id) REFERENCES billing_groups(id);

-- 6. Jeder Nutzer muss in einer Abrechnungsgruppe sein
 ALTER TABLE users ALTER COLUMN billing_group_id SET NOT NULL;