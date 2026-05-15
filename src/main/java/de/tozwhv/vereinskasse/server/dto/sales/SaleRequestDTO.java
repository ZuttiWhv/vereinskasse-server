package de.tozwhv.vereinskasse.server.dto.sales;

public record SaleRequestDTO(
        Long productId,
        Integer amount,
        Long userId,
        Boolean useVoucher,
        Boolean isVoucher
) {
    public SaleRequestDTO {
        if (useVoucher == null) useVoucher = false;
    }
}