package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class V2__insert_default_roles_and_admin_user extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {

        var connection = context.getConnection();

        // -----------------------------
        // Roles
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO role (name) VALUES (?) ON DUPLICATE KEY IGNORE")) {
            stmt.setString(1, "ROLE_USER");
            stmt.executeUpdate();
            stmt.setString(1, "ROLE_ADMIN");
            stmt.executeUpdate();
        }

        // -----------------------------
        // Permissions
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO permission (name) VALUES (?) ON DUPLICATE KEY IGNORE")) {
            stmt.setString(1, "READ_USER"); stmt.executeUpdate();
            stmt.setString(1, "WRITE_USER"); stmt.executeUpdate();
            stmt.setString(1, "DELETE_USER"); stmt.executeUpdate();
        }

        // -----------------------------
        // Permission Groups
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO permission_group (name) VALUES (?) ON DUPLICATE KEY IGNORE")) {
            stmt.setString(1, "ADMIN_GROUP"); stmt.executeUpdate();
            stmt.setString(1, "USER_GROUP"); stmt.executeUpdate();
        }

        // -----------------------------
        // Admin User
        // -----------------------------
        // Prüfen, ob Admin schon existiert
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            check.setString(1, "admin");
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String hashedPassword = encoder.encode("ChangeMe123!");

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users (username, password) VALUES (?, ?)")) {
                    insert.setString(1, "admin");
                    insert.setString(2, hashedPassword);
                    insert.executeUpdate();
                }
            }
        }

        // -----------------------------
        // Optional: Zuordnungen Rollen & Groups
        // -----------------------------
        // Hier kannst du SQL einfügen, um Admin-User direkt zu ROLE_ADMIN und ADMIN_GROUP zuzuordnen
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT u.id, r.id FROM users u, role r WHERE u.username='admin' AND r.name='ROLE_ADMIN' " +
                        "ON DUPLICATE KEY IGNORE")) {
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_permission_groups (user_id, permission_group_id) " +
                        "SELECT u.id, pg.id FROM users u, permission_group pg WHERE u.username='admin' AND pg.name='ADMIN_GROUP' " +
                        "ON DUPLICATE KEY IGNORE")) {
            stmt.executeUpdate();
        }

        // -----------------------------
        // Optional: Admin Group bekommt alle Permissions
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO permission_group_permissions (permission_group_id, permission_id) " +
                        "SELECT pg.id, p.id FROM permission_group pg, permission p WHERE pg.name='ADMIN_GROUP' " +
                        "ON DUPLICATE KEY IGNORE")) {
            stmt.executeUpdate();
        }
    }
}