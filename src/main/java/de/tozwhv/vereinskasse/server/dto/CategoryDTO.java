package de.tozwhv.vereinskasse.server.dto;

import de.tozwhv.vereinskasse.server.modell.Category;
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

    public static CategoryDTO fromEntity(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setImagePath(entity.getImagePath());
        return dto;
    }

    // Die Mapper-Methode
    public Category toEntity() {
        Category category = new Category();
        category.setId(this.id);
        category.setName(this.name);
        // Standardwert-Logik hier zentralisieren
        category.setImagePath(this.imagePath != null ? this.imagePath : "default-cat.png");
        return category;
    }

}