package fr.esgi.fab.okina.repository;

import fr.esgi.fab.okina.configuration.DatabaseConfig;
import fr.esgi.fab.okina.models.Attachment;
import fr.esgi.fab.okina.models.Comment;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.TaskHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskRepository {

    public Task save(Task task) throws SQLException {
        if (task.getId() == null) task.setId(UUID.randomUUID().toString());
        String sql = """
                INSERT INTO task (id, board_id, title, description, type_id, column_id, assignee_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getId());
            ps.setString(2, task.getBoardId());
            ps.setString(3, task.getTitle());
            ps.setString(4, task.getDescription());
            ps.setString(5, task.getTypeId());
            ps.setString(6, task.getColumnId());
            ps.setString(7, task.getAssigneeId());
            ps.setTimestamp(8, Timestamp.valueOf(task.getCreatedAt()));
            ps.executeUpdate();
        }
        return task;
    }

    public List<Task> findByBoardId(String boardId) throws SQLException {
        String sql = "SELECT * FROM task WHERE board_id = ? ORDER BY created_at ASC";
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tasks.add(mapRow(rs));
        }
        return tasks;
    }

    public Task findById(String taskId) throws SQLException {
        String sql = "SELECT * FROM task WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Task task = mapRow(rs);
                task.setComments(findComments(taskId));
                task.setAttachments(findAttachments(taskId));
                task.setHistory(findHistory(taskId));
                return task;
            }
        }
        return null;
    }

    public void updateColumn(String taskId, String columnId) throws SQLException {
        String sql = "UPDATE task SET column_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, columnId);
            ps.setString(2, taskId);
            ps.executeUpdate();
        }
    }

    public void delete(String taskId) throws SQLException {
        deleteComments(taskId);
        deleteAttachments(taskId);
        deleteHistory(taskId);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM task WHERE id = ?")) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        }
    }

    // --- Commentaires ---

    public Comment saveComment(Comment comment) throws SQLException {
        if (comment.getId() == null) comment.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO comment (id, task_id, author_id, content, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comment.getId());
            ps.setString(2, comment.getTaskId());
            ps.setString(3, comment.getAuthorId());
            ps.setString(4, comment.getContent());
            ps.setTimestamp(5, Timestamp.valueOf(comment.getCreatedAt()));
            ps.executeUpdate();
        }
        return comment;
    }

    public List<Comment> findComments(String taskId) throws SQLException {
        String sql = "SELECT * FROM comment WHERE task_id = ? ORDER BY created_at ASC";
        List<Comment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getString("id"));
                c.setTaskId(rs.getString("task_id"));
                c.setAuthorId(rs.getString("author_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(c);
            }
        }
        return list;
    }

    private void deleteComments(String taskId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM comment WHERE task_id = ?")) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        }
    }

    // --- Pièces jointes ---

    public Attachment saveAttachment(Attachment attachment) throws SQLException {
        if (attachment.getId() == null) attachment.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO attachment (id, task_id, filename, file_size, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, attachment.getId());
            ps.setString(2, attachment.getTaskId());
            ps.setString(3, attachment.getFilename());
            ps.setLong(4, attachment.getFileSize());
            ps.setTimestamp(5, Timestamp.valueOf(attachment.getCreatedAt()));
            ps.executeUpdate();
        }
        return attachment;
    }

    public List<Attachment> findAttachments(String taskId) throws SQLException {
        String sql = "SELECT * FROM attachment WHERE task_id = ? ORDER BY created_at ASC";
        List<Attachment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attachment a = new Attachment();
                a.setId(rs.getString("id"));
                a.setTaskId(rs.getString("task_id"));
                a.setFilename(rs.getString("filename"));
                a.setFileSize(rs.getLong("file_size"));
                a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(a);
            }
        }
        return list;
    }

    private void deleteAttachments(String taskId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM attachment WHERE task_id = ?")) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        }
    }

    // --- Historique ---

    public void saveHistory(TaskHistory history) throws SQLException {
        if (history.getId() == null) history.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO task_history (id, task_id, text, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, history.getId());
            ps.setString(2, history.getTaskId());
            ps.setString(3, history.getText());
            ps.setTimestamp(4, Timestamp.valueOf(history.getCreatedAt()));
            ps.executeUpdate();
        }
    }

    public List<TaskHistory> findHistory(String taskId) throws SQLException {
        String sql = "SELECT * FROM task_history WHERE task_id = ? ORDER BY created_at ASC";
        List<TaskHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TaskHistory h = new TaskHistory();
                h.setId(rs.getString("id"));
                h.setTaskId(rs.getString("task_id"));
                h.setText(rs.getString("text"));
                h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(h);
            }
        }
        return list;
    }

    private void deleteHistory(String taskId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM task_history WHERE task_id = ?")) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        }
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getString("id"));
        t.setBoardId(rs.getString("board_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setTypeId(rs.getString("type_id"));
        t.setColumnId(rs.getString("column_id"));
        t.setAssigneeId(rs.getString("assignee_id"));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    }
}
