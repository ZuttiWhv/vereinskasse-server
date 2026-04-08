package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.UserRequestDTO;
import org.springframework.web.bind.annotation.*;
import de.tozwhv.vereinskasse.server.dto.UserDTO;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;


import java.util.List;

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


    // Benutzer löschen
    @PreAuthorize("hasAuthority('DELETE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
