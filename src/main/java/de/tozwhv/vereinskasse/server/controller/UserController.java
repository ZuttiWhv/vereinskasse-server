package de.tozwhv.vereinskasse.server.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public String adminEndpoint() {
        return "Admin only";
    }

    @GetMapping("/manage-users")
    @PreAuthorize("hasAuthority('WRITE_USER')")
    public String manageUsers() {
        return "Permission Group protected";
    }
}