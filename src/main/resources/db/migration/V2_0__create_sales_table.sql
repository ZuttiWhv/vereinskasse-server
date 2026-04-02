CREATE TABLE sales (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       product_id BIGINT NOT NULL,
                       amount INT NOT NULL,
                       price INT NOT NULL, -- Preis in Cents (Menge * Einzelpreis zum Zeitpunkt des Kaufs)
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Fremdschlüssel-Constraints für die Datenintegrität
                       CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users(id),
                       CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- Indexe beschleunigen die Abfrage nach Verkäufen eines bestimmten Users
CREATE INDEX idx_sales_user_id ON sales(user_id);