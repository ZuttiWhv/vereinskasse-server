package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.SelfUpdateRequestDTO;
import de.tozwhv.vereinskasse.server.dto.user.UserDTO;
import de.tozwhv.vereinskasse.server.dto.user.UserRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // Erzeugt den Konstruktor für Dependency Injection (Lombok)
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // Alle Benutzer abrufen
    @PreAuthorize("hasAuthority('READ_USER')")
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    // Einzelnen Benutzer per ID finden
    @PreAuthorize("hasAuthority('READ_USER')")
    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // Neuen Benutzer erstellen
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public ResponseEntity<User> createUser(@RequestBody UserRequestDTO dto) {
        User createdUser = userService.createUser(dto);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public ResponseEntity<Deposit> deposit(
            @PathVariable Long id,
            @RequestBody Integer amount,
            Authentication auth) {

        userService.processDeposit(id, amount, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/generate-barcode")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public ResponseEntity<UserDTO> generateBarcode(@PathVariable Long id) {
        // Die Service-Methode sollte den aktualisierten User (oder das DTO) zurückgeben
        UserDTO updatedUser = userService.createNewBarcodeByID(id);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/generate-barcodes")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public ResponseEntity<Map<String, Object>> generateAllBarcodes() {
        int count = userService.generateBarcodeForAllUsers();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Barcodes erfolgreich generiert");
        response.put("count", count);

        return ResponseEntity.ok(response);
    }



    //Benutzer aktualisieren
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserRequestDTO dto) {
        User updatedUser = userService.updateUser(id, dto);
        return ResponseEntity.ok(updatedUser);
    }


    @GetMapping("/me")
    public UserDTO showSelf(Authentication auth) {
        return userService.getUserByUsername(auth.getName());
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateSelf(
            Authentication auth,
            @Valid @RequestBody SelfUpdateRequestDTO dto) {

        // auth.getName() liefert den aktuell eingeloggten Benutzernamen
        UserDTO updatedUser = userService.updateSelf(auth.getName(), dto);
        return ResponseEntity.ok(updatedUser);
    }

    // Benutzer löschen
    @PreAuthorize("hasAuthority('DELETE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
            userService.softDeleteUser(id);
            return ResponseEntity.noContent().build();
    }
}
