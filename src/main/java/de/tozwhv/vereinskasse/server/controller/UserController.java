package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.UserDTO;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // Erzeugt den Konstruktor für Dependency Injection (Lombok)
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // Alle Benutzer abrufen
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    // Einzelnen Benutzer per ID finden
    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // Neuen Benutzer erstellen
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // Benutzer aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(userDetails.getUsername());
            user.setBalance(userDetails.getBalance());
            user.setPin(userDetails.getPin());
            user.setPinEnabled(userDetails.isPinEnabled());
            user.setRoles(userDetails.getRoles());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Benutzer löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
