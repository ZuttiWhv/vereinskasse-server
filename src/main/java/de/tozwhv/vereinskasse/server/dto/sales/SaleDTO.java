package de.tozwhv.vereinskasse.server.dto.sales;

import de.tozwhv.vereinskasse.server.modell.PaymentStatus;
import java.time.LocalDateTime;

public record SaleDTO(
        long id,
        int price,
        int amount,
        LocalDateTime createdAt,
        long productId,
        String productName,
        Long userId,
        String username,
        String guestName,
        Long hostId,
        String hostName,
        PaymentStatus paymentStatus,
        boolean useVoucher,
        boolean isVoucher
) {
}