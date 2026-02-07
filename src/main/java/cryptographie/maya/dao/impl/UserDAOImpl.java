package cryptographie.maya.dao.impl;

import cryptographie.maya.dao.DatabaseManager;
import cryptographie.maya.dao.UserDAO;
import cryptographie.maya.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    @Override
    public int create(User user) throws Exception {
        String sql = """
            INSERT INTO users (username, password_hash, salt, first_name, last_name, email, role, public_key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFirstName());
            ps.setString(5, user.getLastName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getRole() == null ? "USER" : user.getRole());
            ps.setString(8, user.getPublicKey());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("User insertion failed: no generated key returned.");
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws Exception {
        String sql = """
            SELECT id, username, password_hash, salt, first_name, last_name, email, role, created_at, public_key
            FROM users
            WHERE username = ?
            """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapUser(rs));
            }
        }
    }

    @Override
    public Optional<User> findById(int id) throws Exception {
        String sql = """
            SELECT id, username, password_hash, salt, first_name, last_name, email, role, created_at, public_key
            FROM users
            WHERE id = ?
            """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapUser(rs));
            }
        }
    }

    @Override
    public String getPublicKeyById(int userId) throws Exception {
        String sql = "SELECT public_key FROM users WHERE id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("public_key");
            }
        }
    }

    // --- Nouveaux : listAll / updateRole / deleteById ---

    @Override
    public List<User> listAll() throws Exception {
        String sql = """
            SELECT id, username, password_hash, salt, first_name, last_name, email, role, created_at, public_key
            FROM users
            ORDER BY created_at DESC
            """;

        List<User> out = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapUser(rs));
            }
        }
        return out;
    }

    @Override
    public boolean updateRole(int userId, String newRole) throws Exception {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(int userId) throws Exception {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // --- util ---
    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setPublicKey(rs.getString("public_key"));

        Timestamp ts = null;
        try {
            ts = rs.getTimestamp("created_at");
        } catch (SQLException ignored) {
            // colonne absente ou autre, on ignore
        }
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }

        return u;
    }
}