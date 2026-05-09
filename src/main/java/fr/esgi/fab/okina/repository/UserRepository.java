package fr.esgi.fab.okina.repository;

import fr.esgi.fab.okina.configuration.DatabaseConfig;
import fr.esgi.fab.okina.models.User;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserRepository {

    public User save(User user) throws SQLException {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO app_user (id, pseudo, email, password, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getPseudo());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getHash());
            ps.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
            ps.executeUpdate();
        }
        return user;
    }

    public Optional<User> findByPseudo(String pseudo) throws SQLException {
        String sql = "SELECT * FROM app_user WHERE pseudo = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pseudo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public boolean existsByPseudo(String pseudo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM app_user WHERE pseudo = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pseudo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM app_user WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setPseudo(rs.getString("pseudo"));
        u.setEmail(rs.getString("email"));
        u.setHash(rs.getString("password"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }
}