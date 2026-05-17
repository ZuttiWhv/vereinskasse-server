-- 1. Erweitere die Länge des Username-Feldes, um Platz für den Archivierungs-Suffix zu machen
ALTER TABLE users ALTER COLUMN username VARCHAR(100) NOT NULL;

-- 2. Füge die Spalte 'active' hinzu (Standardmäßig true für alle bestehenden User)
ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;

-- 3. Füge die Spalte 'deleted_at' für den Zeitstempel hinzu (Erlaubt NULL, da aktive User kein Löschdatum haben)
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

-- 4. Bestehende Datensätze explizit auf active = true setzen (Sicherheitshalber für H2)
UPDATE users SET active = TRUE WHERE active IS NULL;