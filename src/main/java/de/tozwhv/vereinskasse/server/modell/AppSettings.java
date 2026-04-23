package de.tozwhv.vereinskasse.server.modell;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "app_settings")
@Data
public class AppSettings {

    @Id
    private Long id = 1L; // Wir nutzen immer die ID 1, um nur einen Datensatz zu haben

    private String primaryColor;
    private String secondaryColor;
    private String logoPath;
    private String vereinName;
    private String navTextColor;
    private boolean quickLogin;
    private boolean pinLogin;
    private boolean passwordlessLogin;

    // Standardwerte im Konstruktor
    public AppSettings() {
        this.primaryColor = "#2563eb"; // Standard Blau
        this.secondaryColor = "#1e40af";
        this.vereinName = "Vereinskasse";
        this.navTextColor = "#ffffff";
        this.pinLogin = false;
        this.quickLogin = false;
        this.passwordlessLogin = false;
    }
}