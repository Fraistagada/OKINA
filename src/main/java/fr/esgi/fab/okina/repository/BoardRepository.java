package fr.esgi.fab.okina.repository;

import fr.esgi.fab.okina.configuration.DatabaseConfig;
import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BoardRepository {

    public Board save(Board board) throws SQLException {
        if (board.getId() == null) {
            board.setId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO board (id, name, owner_id, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, board.getId());
            ps.setString(2, board.getName());
            ps.setString(3, board.getOwnerId());
            ps.setTimestamp(4, Timestamp.valueOf(board.getCreatedAt()));
            ps.executeUpdate();
        }
        // Le créateur est automatiquement membre
        addMember(board.getId(), board.getOwnerId());
        return board;
    }

    // Récupère tous les tableaux auxquels un utilisateur contribue
    public List<Board> findByUserId(String userId) throws SQLException {
        String sql = """
                SELECT b.* FROM board b
                JOIN board_member bm ON b.id = bm.board_id
                WHERE bm.user_id = ?
                ORDER BY b.created_at DESC
                """;
        List<Board> boards = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                boards.add(mapRow(rs));
            }
        }
        // Pour chaque tableau, on charge les membres
        for (Board board : boards) {
            board.setMembers(findMembers(board.getId()));
        }
        return boards;
    }

    public Board findById(String boardId) throws SQLException {
        String sql = "SELECT * FROM board WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Board board = mapRow(rs);
                board.setMembers(findMembers(boardId));
                return board;
            }
        }
        return null;
    }

    public void addMember(String boardId, String userId) throws SQLException {
        String sql = "MERGE INTO board_member (board_id, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean isMember(String boardId, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM board_member WHERE board_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private List<User> findMembers(String boardId) throws SQLException {
        String sql = """
                SELECT u.* FROM app_user u
                JOIN board_member bm ON u.id = bm.user_id
                WHERE bm.board_id = ?
                """;
        List<User> members = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getString("id"));
                u.setPseudo(rs.getString("pseudo"));
                u.setEmail(rs.getString("email"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                members.add(u);
            }
        }
        return members;
    }

    private Board mapRow(ResultSet rs) throws SQLException {
        Board b = new Board();
        b.setId(rs.getString("id"));
        b.setName(rs.getString("name"));
        b.setOwnerId(rs.getString("owner_id"));
        b.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return b;
    }
}
