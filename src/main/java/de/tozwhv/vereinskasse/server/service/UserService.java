package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.UserDTO;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<GrantedAuthority> auths = new ArrayList<>();

        user.getRoles().forEach(role -> {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(permission -> auths.add(new SimpleGrantedAuthority(permission.getName())));
        });

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), auths);
    }

    public UserDTO getUserById(long id) {
        return userRepository.findById(id)
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
                        .collect(Collectors.toSet())
        );
    }

}