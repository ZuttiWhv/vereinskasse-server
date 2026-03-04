package de.tozwhv.vereinskasse.server.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Open to all";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Admin only";
    }

    @GetMapping("/manage-users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public String manageUsers() {
        return "Permission Group protected";
    }
}