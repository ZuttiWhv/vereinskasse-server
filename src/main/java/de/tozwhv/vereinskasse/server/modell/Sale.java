package de.tozwhv.vereinskasse.server.modell;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "sales")

public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    long id;

    @Getter
    @Column(name = "price", nullable = false)
    private int price;

    @Getter
    @Column(name = "amount", nullable = false)
    private int amount;

    @Getter
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Getter
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private Date timestamp;

    public Sale() {
    }

    public Sale(int price, int amount, Product product, User user) {
        this.price = price;
        this.amount = amount;
        this.product = product;
        this.user = user;
    }

}

