-- Migration: Produkte und User um Barcode-Felder erweitern

ALTER TABLE app_settings ADD COLUMN allow_barcode_login BOOLEAN DEFAULT FALSE;