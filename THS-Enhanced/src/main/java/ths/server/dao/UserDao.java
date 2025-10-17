// src/main/java/ths/server/dao/UserDao.java
package ths.server.dao;

import ths.server.db.Database;
import ths.server.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    /** Insert a user (using the fields already set on the User object). */
    public void create(User u) throws Exception {
        final String sql = "INSERT INTO users (name, email, password_hash, role, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection cx = Database.conn();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole());
            ps.setTimestamp(5, Timestamp.valueOf(u.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getLong(1));
                }
            }
        }
    }

    /** Find a user by email. */
    public static User findByEmail(String email) throws SQLException {
        final String sql = """
            SELECT id, name, email, password_hash, role, created_at
            FROM users
            WHERE email = ?
            """;
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Insert a user (convenience) and return the created row. */
    public static User create(String name, String email, String role, String passwordHash) throws SQLException {
        final String sql = """
            INSERT INTO users (name, email, role, password_hash, created_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, role);
            ps.setString(4, passwordHash);
            ps.executeUpdate();

            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for users.id");
                }
                id = keys.getLong(1);
            }

            User u = new User();
            u.setId(id);
            u.setName(name);
            u.setEmail(email);
            u.setPasswordHash(passwordHash);
            u.setRole(role);
            return u;
        }
    }

    /** List users by role (id, name, email, role). */
    public static List<User> listByRole(String role) throws SQLException {
        final String sql = """
            SELECT id, name, email, password_hash, role, created_at
            FROM users
            WHERE role = ?
            ORDER BY name ASC, email ASC
            """;
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> out = new ArrayList<>();
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setRole(rs.getString("role"));
                    out.add(u);
                }
                return out;
            }
        }
    }

    /** All users with role = PATIENT (id + name). */
    public List<User> listAllPatients() throws SQLException {
        final String sql = """
            SELECT id, name
            FROM users
            WHERE role = 'PATIENT'
            ORDER BY name ASC, id ASC
            """;
        List<User> out = new ArrayList<>();
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setName(rs.getString("name"));
                out.add(u);
            }
        }
        return out;
    }

    /** Distinct patients that have any prescription with this specialist. */
    public List<User> listMyPatients(long specialistId) throws SQLException {
        final String sql = """
            SELECT DISTINCT u.id, u.name
            FROM prescriptions rx
            JOIN users u ON u.id = rx.patient_id
            WHERE rx.specialist_id = ?
            ORDER BY u.name ASC, u.id ASC
            """;
        List<User> out = new ArrayList<>();
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, specialistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    out.add(u);
                }
            }
        }
        return out;
    }

    // --- helpers ---

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        // created_at exists in DB; add getter/setter in model if you want it back here
        return u;
    }
}
