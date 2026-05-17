package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.SelfUpdateRequestDTO;
import de.tozwhv.vereinskasse.server.dto.user.UserDTO;
import de.tozwhv.vereinskasse.server.dto.user.UserRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.*;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepositRepository depositRepository;
    private final BillingGroupRepository billingGroupRepository;
    private final OrganisationalUnitRepository organisationalUnitRepository;
    private final PinAuthService pinAuthService;
    private final AppSettingsService appSettingsService;
    private static final String ARCHIVED_MARKER = "_archived_";

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       DepositRepository depositRepository, BillingGroupRepository billingGroupRepository, OrganisationalUnitRepository organisationalUnitRepository, PinAuthService pinAuthService, AppSettingsService appSettingsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.depositRepository = depositRepository;
        this.billingGroupRepository = billingGroupRepository;
        this.organisationalUnitRepository = organisationalUnitRepository;
        this.pinAuthService = pinAuthService;
        this.appSettingsService = appSettingsService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive"));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), user.getAuthorities());
    }

    @Transactional
    public void processDeposit(Long targetUserId, int amount, String adminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Ziel-Benutzer nicht gefunden"));

        User admin = userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin nicht gefunden oder inaktiv"));

        targetUser.setBalance(targetUser.getBalance() + amount);
        userRepository.save(targetUser);

        Deposit deposit = new Deposit();
        deposit.setUser(targetUser);
        deposit.setCreatedBy(admin);
        deposit.setAmount(amount);

        depositRepository.save(deposit);
    }

    public User createUser(UserRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));

        user.setBalance(dto.balance() != null ? dto.balance() : 0);
        user.setPin(dto.pin() != null ? passwordEncoder.encode(dto.pin()) : null);
        user.setPinEnabled(false);
        user.setActive(true); // Explizit sicherstellen, dass neue User aktiv sind

        if (dto.orgUnitId() != null) {
            user.setOrgUnit(organisationalUnitRepository.getReferenceById(dto.orgUnitId()));
        }

        user.setPasswordlessLoginEnabled(dto.passwordlessLoginEnabled() != null && dto.passwordlessLoginEnabled());

        if (dto.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));
            user.setRoles(roles);
        }

        if (dto.billingGroupId() != null) {
            user.setBillingGroup(billingGroupRepository.getReferenceById(dto.billingGroupId()));
        }

        if (dto.Barcode() !=null){
            user.setBarcode(dto.Barcode());
        }else{
            if (appSettingsService.getSettingsInternal().isAllowBarcodeLogin()){
                user.setBarcode(generateUniqueBarcode());
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public UserDTO updateSelf(String username, SelfUpdateRequestDTO dto) {
        User user = userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden oder inaktiv"));

        if (dto.newPassword() != null && !dto.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.newPassword()));
        }

        if (dto.newPin() != null && !dto.newPin().isBlank()) {
            user.setPin(passwordEncoder.encode(dto.newPin()));
            pinAuthService.resetAttempts(username);
        }
        if (dto.passwordlessLoginEnabled() != null) {
            user.setPasswordlessLoginEnabled(dto.passwordlessLoginEnabled());
        }

        if (dto.pinEnabled() != null) {
            user.setPinEnabled(dto.pinEnabled());
        }

        if (dto.barcodeLoginEnabled() != null){
            user.setBarcodeLoginEnabled(dto.barcodeLoginEnabled());
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    private String generateUniqueBarcode() {
        String newBarcode;
        do {
            newBarcode = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
            // GEÄNDERT: Prüfung, ob der Barcode bei IRGENDEINEM (auch inaktiven) User existiert, um DB-Kollisionen zu vermeiden
        } while (userRepository.findByBarcode(newBarcode).isPresent());
        return newBarcode;
    }

    public UserDTO createNewBarcodeByID(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Benutzer mit ID " + id + " nicht gefunden"));

        user.setBarcode(generateUniqueBarcode());
        return convertToDTO(userRepository.save(user));
    }

    public int generateBarcodeForAllUsers() {
        int counter = 0;
        for (User user : userRepository.findAllByIsLockedFalseAndActiveTrue()) {
            if (user.getBarcode() == null) {
                user.setBarcode(generateUniqueBarcode());
                userRepository.save(user);
                counter += 1;
            }
        }
        return counter;
    }

    public User updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Benutzer mit ID " + id + " nicht gefunden"));

        user.setUsername(dto.username());

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        if (dto.passwordlessLoginEnabled() != null) {
            user.setPasswordlessLoginEnabled(dto.passwordlessLoginEnabled());
        }

        if (dto.balance() != null) user.setBalance(dto.balance());

        if (dto.pin() != null) {
            if (!dto.pin().matches("\\d{4,6}")) {
                throw new IllegalArgumentException("PIN muss aus 4 bis 6 Ziffern bestehen.");
            }
            user.setPin(passwordEncoder.encode(dto.pin()));
            user.setPinEnabled(true);
        }

        if (dto.orgUnitId() != null) {
            user.setOrgUnit(organisationalUnitRepository.getReferenceById(dto.orgUnitId()));
        } else {
            user.setOrgUnit(null);
        }

        if (dto.Barcode() !=null){
            user.setBarcode(dto.Barcode());
        }

        if (dto.billingGroupId() != null) {
            user.setBillingGroup(billingGroupRepository.getReferenceById(dto.billingGroupId()));
        }

        if (dto.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));
            if (!roles.isEmpty()) user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    public UserDTO getUserById(long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
    }

    public User getCurrentUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Nicht angemeldet");
        }


        return userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(auth.getName())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Benutzer nicht gefunden oder inaktiv"));
    }

    public UserDTO getUserByUsername(String username) {
        // GEÄNDERT: Suche nach dem aktiven Benutzernamen
        return userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(username)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
    }

    /**
     * Holt alle aktiven User aus der DB und gibt sie als Liste von DTOs zurück.
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAllByIsLockedFalseAndActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * NEU: Führt das "Soft-Delete" (Archivieren) eines Benutzers durch.
     * Schützt historische Kassendaten und gibt den originalen Benutzernamen wieder frei.
     */
    @Transactional
    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzer ist bereits archiviert");
        }

        long timestamp = System.currentTimeMillis() / 1000;

        // 1. Eindeutigen Suffix anhängen, um den echten Benutzernamen freizugeben
        user.setUsername(user.getUsername() + ARCHIVED_MARKER + timestamp);

        // 2. Physische Barcodes & Logins entkoppeln, damit der Nachfolger sie nutzen kann
        user.setBarcode(null);
        user.setBarcodeLoginEnabled(false);
        user.setPasswordlessLoginEnabled(false);
        user.setPinEnabled(false);
        user.setPin(null);

        // 3. Aus der Kiosk-Struktur (Abteilungs-Ordner) entfernen
        user.setOrgUnit(null);

        // 4. Status-Flags kippen
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private UserDTO convertToDTO(User user) {
        // Falls ein archivierter User (z.B. in alten Log-Einträgen) konvertiert wird,
        // schneiden wir den Suffix für die Benutzeroberfläche wieder ab.
        String displayUsername = user.getUsername();
        if (!user.isActive() && displayUsername.contains(ARCHIVED_MARKER)) {
            displayUsername = displayUsername.split(ARCHIVED_MARKER)[0] + " (Ehemalig)";
        }

        return new UserDTO(
                user.getId(),
                displayUsername, // Geänderten Anzeigenamen nutzen
                user.isPinEnabled(),
                user.getBalance(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet()),
                user.getBillingGroup() != null ? user.getBillingGroup().getId() : null,
                user.getBillingGroup() != null ? user.getBillingGroup().getName() : "Keine Gruppe",
                user.getOrgUnit() != null ? user.getOrgUnit().getId() : null,
                user.getOrgUnit() != null ? user.getOrgUnit().getName() : "Keine Abteilung",
                user.isPasswordlessLoginEnabled(),
                user.isBarcodeLoginEnabled()
        );
    }
}