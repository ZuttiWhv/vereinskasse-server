package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<GrantedAuthority> auths = new ArrayList<>();

        user.getRoles().forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r.getName())));
        user.getPermissionGroups().forEach(pg ->
                pg.getPermissions().forEach(p -> auths.add(new SimpleGrantedAuthority(p.getName())))
        );

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), auths);
    }
}