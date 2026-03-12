-- 1. Kategorien anlegen
INSERT INTO category (name, imagePath)
VALUES ('Softdrinks', 'path');
INSERT INTO category (name, imagePath)
VALUES ('Schokoriegel', 'path');

-- 2. Produkte für Softdrinks hinzufügen
INSERT INTO product (name, anzeigename, vk, imagePath, fk_category_id)
VALUES ('Coca-Cola', 'Cola 0,33l', 250, 'assets/images/cola.jpg',
        (SELECT id FROM category WHERE name = 'Softdrinks'));

INSERT INTO product (name, anzeigename, vk, imagePath, fk_category_id)
VALUES ('Fanta', 'Fanta Dose 0,33l', 220, 'assets/images/fanta.jpg',
        (SELECT id FROM category WHERE name = 'Softdrinks'));

-- 3. Produkte für Schokoriegel hinzufügen
INSERT INTO product (name, anzeigename, vk, imagePath, fk_category_id)
VALUES ('Mars', 'Mars Riegel', 120, 'assets/images/mars.jpg',
        (SELECT id FROM category WHERE name = 'Schokoriegel'));

INSERT INTO product (name, anzeigename, vk, imagePath, fk_category_id)
VALUES ('Twix', 'Twix Doppelpack', 120, 'assets/images/twix.jpg',
        (SELECT id FROM category WHERE name = 'Schokoriegel'));

INSERT INTO product (name, anzeigename, vk, imagePath, fk_category_id)
VALUES ('Snickers', 'Snickers Riegel', 120, 'assets/images/snickers.jpg',
        (SELECT id FROM category WHERE name = 'Schokoriegel'));
