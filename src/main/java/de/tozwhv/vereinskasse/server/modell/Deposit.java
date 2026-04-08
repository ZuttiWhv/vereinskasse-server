package de.tozwhv.vereinskasse.server.modell;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Der User, dessen Konto gefüllt wurde

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy; // Der Kassenwart/Admin

    private int amount; // Betrag in Cent
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

