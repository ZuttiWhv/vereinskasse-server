package de.tozwhv.vereinskasse.server.modell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "org_units")
public class OrganisationalUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Die Selbst-Referenz für die Hierarchie
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"subUnits", "users"})
    private OrganisationalUnit parent;

    // Liste der Unterabteilungen (für einfaches Tree-Mapping)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("parent")
    private List<OrganisationalUnit> subUnits = new ArrayList<>();

    // Die Benutzer, die direkt dieser Abteilung zugeordnet sind
    @OneToMany(mappedBy = "orgUnit")
    private List<User> users = new ArrayList<>();
}