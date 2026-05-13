package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.Comment;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.TaskHistory;
import fr.esgi.fab.okina.repository.TaskRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskService {

    private final TaskRepository taskRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getTasksForBoard(String boardId) throws SQLException {
        return taskRepository.findByBoardId(boardId);
    }

    public Task getTaskById(String taskId) throws SQLException {
        return taskRepository.findById(taskId);
    }

    public String createTask(String boardId, String title, String description,
                             String typeId, String assigneeId) throws SQLException {
        if (title == null || title.trim().isEmpty()) return "Le titre est obligatoire.";
        if (typeId == null || typeId.trim().isEmpty()) return "Le type est obligatoire.";

        Task task = new Task();
        task.setBoardId(boardId);
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setTypeId(typeId);
        task.setColumnId("todo");
        task.setAssigneeId(assigneeId == null || assigneeId.isEmpty() ? null : assigneeId);
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        TaskHistory h = new TaskHistory();
        h.setTaskId(task.getId());
        h.setText("Tâche créée le " + task.getCreatedAt().format(FMT));
        h.setCreatedAt(LocalDateTime.now());
        taskRepository.saveHistory(h);

        return null;
    }

    public String moveTask(String taskId, String newColumnId) throws SQLException {
        List<String> valid = List.of("todo", "doing", "done");
        if (!valid.contains(newColumnId)) return "Colonne invalide.";

        Task task = taskRepository.findById(taskId);
        if (task == null) return "Tâche introuvable.";

        taskRepository.updateColumn(taskId, newColumnId);

        String label = switch (newColumnId) {
            case "todo"  -> "À faire";
            case "doing" -> "En cours";
            case "done"  -> "Terminé";
            default -> newColumnId;
        };
        TaskHistory h = new TaskHistory();
        h.setTaskId(taskId);
        h.setText("Déplacée vers « " + label + " » le " + LocalDateTime.now().format(FMT));
        h.setCreatedAt(LocalDateTime.now());
        taskRepository.saveHistory(h);

        return null;
    }

    public void deleteTask(String taskId) throws SQLException {
        taskRepository.delete(taskId);
    }

    public String addComment(String taskId, String authorId, String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) return "Le commentaire est vide.";
        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setAuthorId(authorId);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        taskRepository.saveComment(comment);
        return null;
    }
}
