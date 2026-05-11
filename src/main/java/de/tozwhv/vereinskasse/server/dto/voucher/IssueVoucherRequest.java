package de.tozwhv.vereinskasse.server.dto.voucher;

public record IssueVoucherRequest(
        Long productId,
        Integer quantity,
        String reason
) {}