package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.UserDTO;
import de.tozwhv.vereinskasse.server.dto.UserRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.BillingGroupRepository;
import de.tozwhv.vereinskasse.server.repository.DepositRepository;
import de.tozwhv.vereinskasse.server.repository.RoleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    // final stellt sicher, dass die Abhängigkeiten beim Start gesetzt werden müssen
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepositRepository depositRepository;
    private final BillingGroupRepository billingGroupRepository;

    // Der Konstruktor für Spring (kein @Autowired mehr nötig ab Spring 4.3+)
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       DepositRepository depositRepository, BillingGroupRepository billingGroupRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.depositRepository = depositRepository;
        this.billingGroupRepository = billingGroupRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), user.getAuthorities());
    }

    @Transactional
    public void processDeposit(Long targetUserId, int amount, String adminUsername) {
        // 1. Beteiligte laden
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Ziel-Benutzer nicht gefunden"));

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin nicht gefunden"));

        // 2. Guthaben aktualisieren
        targetUser.setBalance(targetUser.getBalance() + amount);
        userRepository.save(targetUser);

        // 3. Einzahlung protokollieren
        Deposit deposit = new Deposit();
        deposit.setUser(targetUser);
        deposit.setCreatedBy(admin);
        deposit.setAmount(amount);

        depositRepository.save(deposit); // Du benötigst ein DepositRepository
    }

    public User createUser(UserRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));

        // Null-Check oder Default-Werte setzen
        user.setBalance(dto.balance() != null ? dto.balance() : 0);
        user.setPin(dto.pin() != null ? dto.pin() : 0);
        user.setPinEnabled(false);

        // Rollen-Mapping
        if (dto.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));
            user.setRoles(roles);
        }

        user.setBillingGroup(billingGroupRepository.getReferenceById(dto.billingGroupId()));

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserRequestDTO dto) {
        // 1. Bestehenden Benutzer laden oder Fehler werfen
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Benutzer mit ID " + id + " nicht gefunden"));

        user.setUsername(dto.username());

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        if (dto.balance() != null) {
            user.setBalance(dto.balance());
        }
        if (dto.pin() != null) {
            user.setPin(dto.pin());
        }
        if (dto.billingGroupId() != null){
            user.setBillingGroup(billingGroupRepository.getReferenceById(dto.billingGroupId()));
        }

        // 5. Rollen aktualisieren
        if (dto.roleIds() != null) {
            // Alle Rollen-Entitäten anhand der gelieferten IDs laden
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));

            // Validierung: Ein User sollte mindestens eine Rolle haben
            if (!roles.isEmpty()) {
                user.setRoles(roles);
            }
        }

        // 6. Speichern und zurückgeben
        return userRepository.save(user);
    }

    public UserDTO getUserById(long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

    }

    public UserDTO getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
    }

    /**
     * Holt alle User aus der DB und gibt sie als Liste von DTOs zurück.
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll() // 1. Liste von User-Entities holen
                .stream()               // 2. Stream öffnen
                .map(this::convertToDTO)// 3. Jedes Element umwandeln
                .toList();              // 4. Als Liste sammeln
    }

    /**
     * Hilfsmethode für das Mapping (Entity -> DTO)
     */
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.isPinEnabled(),
                user.getBalance(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet()),
                user.getBillingGroup().getId(),
                user.getBillingGroup().getName()
        );
    }

}