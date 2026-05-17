package de.tozwhv.vereinskasse.server.modell;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;


    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "anzeigename", unique = true)
    private String anzeigename;

    @Column(name = "vk", nullable = false)
    private int price;

    @Column(name = "imagePath")
    private String imagePath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_category_id", nullable = false)
    private Category category;

    /**
     * Steuert, ob das Produkt aktuell zum Verkauf angeboten wird.
     * Default: true
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Markiert das Produkt als gelöscht (Soft-Delete).
     * Produkte mit deleted = true sollten in keinem Select mehr auftauchen.
     * Default: false
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_barcodes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "barcode", unique = true) // Jeder Barcode darf systemweit nur 1x existieren!
    private Set<String> barcodes = new HashSet<>();


    public String getPriceString() {
        return String.format("%.2f", price / 100.0) + " €";
    }

}

