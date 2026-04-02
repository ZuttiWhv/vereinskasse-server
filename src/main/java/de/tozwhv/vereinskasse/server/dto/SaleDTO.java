package de.tozwhv.vereinskasse.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleDTO {
    private Long productId;
    private Integer amount;
    private Long userId; // Nur gefüllt, wenn WRITE_ALL_SALES vorhanden ist
}