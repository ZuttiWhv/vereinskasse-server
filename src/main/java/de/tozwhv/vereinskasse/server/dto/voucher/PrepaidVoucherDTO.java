package de.tozwhv.vereinskasse.server.dto.voucher;

/**
 * DTO für die Darstellung von verfügbaren Freigetränken/Produkten.
 */
public record PrepaidVoucherDTO(
        Long productId,          // ID des Produkts (z.B. für den Kauf-Request)
        String productName,      // Anzeigename (z.B. "Bier 0,33l")
        Integer remainingCount,  // Wie viele Einheiten sind in diesem Kontingent noch da?
        String giverName,        // Wer hat die Runde ausgegeben?
        String reason            // Anlass (z.B. "Geburtstagsrunde von Michael")
) {}