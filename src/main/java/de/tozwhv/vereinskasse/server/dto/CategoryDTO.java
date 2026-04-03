package de.tozwhv.vereinskasse.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDTO {

    // Beim Anlegen ist die ID null, beim Update ist sie gefüllt
    private Long id;

    @NotBlank(message = "Der Name der Kategorie darf nicht leer sein")
    @Size(min = 2, max = 30, message = "Name muss zwischen 2 und 50 Zeichen lang sein")
    private String name;

    // Hier erlauben wir null, falls noch kein Bild da ist
    private String imagePath;
}