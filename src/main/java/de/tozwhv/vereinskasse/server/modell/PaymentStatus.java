package de.tozwhv.vereinskasse.server.modell;

public enum PaymentStatus {
    COMPLETED,          // Regulärer Kauf
    PENDING_GUEST,      // Offener Gast-Deckel
    SETTLED_GUEST       // Bezahlter Gast-Deckel
}