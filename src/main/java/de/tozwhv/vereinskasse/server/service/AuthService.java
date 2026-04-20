package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.config.JwtProvider;
import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import io.jsonwebtoken.security.InvalidKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String access = jwtProvider.generateAccessToken(username);
        String refresh = jwtProvider.generateRefreshToken(username);
        return new AuthResponse(access, refresh);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) ||
                !"refresh".equals(jwtProvider.extractType(refreshToken))) {
            throw new InvalidKeyException("Invalid refresh token");
        }
        String username = jwtProvider.getUsername(refreshToken);
        String access = jwtProvider.generateAccessToken(username);
        String refresh = jwtProvider.generateRefreshToken(username);

        return new AuthResponse(access, refresh);
    }
}