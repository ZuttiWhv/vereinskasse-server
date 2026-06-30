package de.tozwhv.vereinskasse.server.modell;

import lombok.Getter;

@Getter
public enum TransactionType {
    SALE("Verkauf"),
    REDEEM("Einlösung"),
    VOUCHER("Gutscheinkauf"),
    DEPOSIT("Einzahlung"),
    MONTHLY_CONTRIBUTION("Monatsbeitrag"),
    INITIATION_FEE("Aufnahmegebühr"),
    ONE_TIME_CHARGE("Einmalige Gebühr");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }
}