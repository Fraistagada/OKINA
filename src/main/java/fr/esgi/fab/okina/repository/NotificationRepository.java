package fr.esgi.fab.okina.repository;

import fr.esgi.fab.okina.configuration.DatabaseConfig;
import fr.esgi.fab.okina.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {

    public Notification save(Notification notification) throws SQLException {
        if (notification.getId() == null) notification.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO notification (id, user_id, board_id, task_id, text, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notification.getId());
            ps.setString(2, notification.getUserId());
            ps.setString(3, notification.getBoardId());
            ps.setString(4, notification.getTaskId());
            ps.setString(5, notification.getText());
            ps.setBoolean(6, notification.isRead());
            ps.setTimestamp(7, Timestamp.valueOf(notification.getCreatedAt()));
            ps.executeUpdate();
        }
        return notification;
    }

    // Dernières notifications d'un utilisateur (plus récentes en premier)
    public List<Notification> findByUser(String userId, int limit) throws SQLException {
        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.setId(rs.getString("id"));
                n.setUserId(rs.getString("user_id"));
                n.setBoardId(rs.getString("board_id"));
                n.setTaskId(rs.getString("task_id"));
                n.setText(rs.getString("text"));
                n.setRead(rs.getBoolean("is_read"));
                n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(n);
            }
        }
        return list;
    }

    public int countUnread(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void markAllRead(String userId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public void deleteForBoard(String boardId) throws SQLException {
        String sql = "DELETE FROM notification WHERE board_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ps.executeUpdate();
        }
    }
}
