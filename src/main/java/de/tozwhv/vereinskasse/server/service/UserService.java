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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       DepositRepository depositRepository, BillingGroupRepository billingGroupRepository, OrganisationalUnitRepository organisationalUnitRepository, PinAuthService pinAuthService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.depositRepository = depositRepository;
        this.billingGroupRepository = billingGroupRepository;
        this.organisationalUnitRepository = organisationalUnitRepository;
        this.pinAuthService = pinAuthService;
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

        user.setBalance(dto.balance() != null ? dto.balance() : 0);
        user.setPin(dto.pin() != null ? passwordEncoder.encode(dto.pin()) : null);
        user.setPinEnabled(false);

        // SETZEN DER ORG-UNIT
        if (dto.orgUnitId() != null) {
            user.setOrgUnit(organisationalUnitRepository.getReferenceById(dto.orgUnitId()));
        }

        user.setPasswordlessLoginEnabled(dto.passwordlessLoginEnabled() != null && dto.passwordlessLoginEnabled());

        // Rollen-Mapping
        if (dto.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));
            user.setRoles(roles);
        }

        // BILLING GROUP
        if (dto.billingGroupId() != null) {
            user.setBillingGroup(billingGroupRepository.getReferenceById(dto.billingGroupId()));
        }

        if (dto.Barcode() !=null){
            user.setBarcode(dto.Barcode());
        }

        return userRepository.save(user);
    }

    @Transactional
    public UserDTO updateSelf(String username, SelfUpdateRequestDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden"));

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

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
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

            // PIN hashen (BCrypt nutzt den gleichen Encoder wie das Passwort)
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
                user.getBillingGroup() != null ? user.getBillingGroup().getId() : null,
                user.getBillingGroup() != null ? user.getBillingGroup().getName() : "Keine Gruppe",
                user.getOrgUnit() != null ? user.getOrgUnit().getId() : null,
                user.getOrgUnit() != null ? user.getOrgUnit().getName() : "Keine Abteilung",
                user.isPasswordlessLoginEnabled(),
                user.getBarcode()
        );
    }

}