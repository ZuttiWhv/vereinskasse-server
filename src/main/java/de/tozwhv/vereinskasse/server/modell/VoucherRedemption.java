package de.tozwhv.vereinskasse.server.modell;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_redemptions")
@Getter
@Setter
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voucher_id")
    private PrepaidVoucher voucher;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consumer_id")
    private User consumer;

    @Column(nullable = false)
    private LocalDateTime redeemedAt = LocalDateTime.now();
}