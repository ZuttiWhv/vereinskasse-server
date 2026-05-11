package de.tozwhv.vereinskasse.server.modell;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prepaid_vouchers")
@Getter
@Setter
public class PrepaidVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giver_id")
    private User giver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    private String reason; // Bemerkung / Anlass

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoucherRedemption> redemptions = new ArrayList<>();
}