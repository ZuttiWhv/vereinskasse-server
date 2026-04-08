package de.tozwhv.vereinskasse.server.modell;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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


    public String getPriceString() {
        return String.format("%.2f", price / 100.0) + " €";
    }

}

