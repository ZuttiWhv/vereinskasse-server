-- Flyway Script: V2__create_deposit_table.sql

CREATE TABLE deposit (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         created_by_id BIGINT NOT NULL,
                         amount INT NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Fremdschlüssel-Beziehungen
                         CONSTRAINT fk_deposit_user
                             FOREIGN KEY (user_id)
                                 REFERENCES USERS(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT fk_deposit_creator
                             FOREIGN KEY (created_by_id)
                                 REFERENCES USERS(id)
);

-- Indexe für schnellere Abfragen (wichtig für die Historie)
CREATE INDEX idx_deposit_user_id ON deposit(user_id);
CREATE INDEX idx_deposit_created_at ON deposit(created_at);