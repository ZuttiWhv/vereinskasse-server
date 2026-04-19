-- 1. Erstellen der Tabelle für Organisationseinheiten (Abteilungen)
CREATE TABLE org_units (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           parent_id BIGINT,
                           CONSTRAINT fk_org_units_parent FOREIGN KEY (parent_id) REFERENCES org_units(id) ON DELETE SET NULL
);

-- 2. Hinzufügen der Verknüpfung zur User-Tabelle
-- Wir gehen davon aus, dass die Tabelle 'users' bereits existiert.
ALTER TABLE users ADD COLUMN org_unit_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_units(id) ON DELETE SET NULL;

-- 3. Indizes für bessere Performance bei der Baum-Navigation
CREATE INDEX idx_org_units_parent ON org_units(parent_id);
CREATE INDEX idx_users_org_unit ON users(org_unit_id);
