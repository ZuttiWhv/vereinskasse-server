package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class V1_1__Prepopulate_User_And_Permissions extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {

        var connection = context.getConnection();

        // -----------------------------
        // Permissions
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO permission (name) VALUES (?)")) {
            stmt.setString(1, "READ_USER");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_USER");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_USER");
            stmt.executeUpdate();

            stmt.setString(1, "READ_ROLE");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_ROLE");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_ROLE");
            stmt.executeUpdate();

            stmt.setString(1, "READ_PERMISSION");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_PERMISSION");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_PERMISSION");
            stmt.executeUpdate();

            stmt.setString(1, "READ_PRODUCT");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_PRODUCT");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_PRODUCT");
            stmt.executeUpdate();

            stmt.setString(1, "READ_CATEGORY");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_CATEGORY");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_CATEGORY");
            stmt.executeUpdate();


            stmt.setString(1, "READ_ALL_SALES");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_ALL_SALES");
            stmt.executeUpdate();
            stmt.setString(1, "DELETE_ALL_SALES");
            stmt.executeUpdate();

            stmt.setString(1, "READ_OWN_SALES");
            stmt.executeUpdate();
            stmt.setString(1, "WRITE_OWN_SALES");
            stmt.executeUpdate();


        }


        // -----------------------------
        // Roles
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO ROLE (name) VALUES (?) ")) {
            stmt.setString(1, "USER");
            stmt.executeUpdate();
            stmt.setString(1, "ADMIN");
            stmt.executeUpdate();
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
                String hashedPassword = encoder.encode("admin");

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users (username, password,balance,pin_enabled) VALUES (?, ?,?,?)")) {
                    insert.setString(1, "admin");
                    insert.setString(2, hashedPassword);
                    insert.setInt(3, 0);
                    insert.setBoolean(4, false);
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
                        "SELECT u.id, r.id FROM users u, role r WHERE u.username='admin' AND r.name='ADMIN' ")) {
            stmt.executeUpdate();
        }


        // -----------------------------
        // Optional: Admin Group bekommt alle Permissions
        // -----------------------------
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO ROLE_PERMISSION (role_id, permission_id) " +
                        "SELECT role.id, permission.id FROM role role, permission permission WHERE role.name='ADMIN' ")) {
            stmt.executeUpdate();
        }
    }
}



