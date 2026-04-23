package de.tozwhv.vereinskasse.server.modell;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_groups")
public class BillingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    // Erlaubt negatives Guthaben?
    private boolean allowNegativeBalance;

    // Wie weit darf man ins Minus (in Cent)?
    // Beispiel: 5000 für -50,00 €
    private long creditLimit;

    // Optional: Ein Standardwert für neue User in dieser Gruppe
    private boolean isDefault;
}