-- Migration: Produkte um Status-Felder erweitern

ALTER TABLE product
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE product
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Da wir im Service ständig nach 'deleted = false' filtern, hilft ein Index:
CREATE INDEX idx_product_deleted_active ON product (deleted, active);