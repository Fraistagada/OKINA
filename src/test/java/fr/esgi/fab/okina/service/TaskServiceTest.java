package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = Mockito.mock(TaskRepository.class);
        taskService = new TaskService(taskRepository);
    }

    // ---- Tests création ----

    @Test
    void createTask_titreVide_retourneErreur() throws SQLException {
        String erreur = taskService.createTask("board1", "", "desc", "bug", null);
        assertNotNull(erreur);
        assertTrue(erreur.contains("titre") || erreur.contains("Titre"));
    }

    @Test
    void createTask_titreNull_retourneErreur() throws SQLException {
        String erreur = taskService.createTask("board1", null, "desc", "bug", null);
        assertNotNull(erreur);
    }

    @Test
    void createTask_typeNull_retourneErreur() throws SQLException {
        String erreur = taskService.createTask("board1", "Ma tâche", "desc", null, null);
        assertNotNull(erreur);
        assertTrue(erreur.contains("type") || erreur.contains("Type"));
    }

    @Test
    void createTask_donneesValides_retourneNull() throws SQLException {
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        String erreur = taskService.createTask("board1", "Ma tâche", "desc", "bug", "user1");
        assertNull(erreur);

        verify(taskRepository, times(1)).save(any(Task.class));
        verify(taskRepository, times(1)).saveHistory(any());
    }

    @Test
    void createTask_sanAssignee_retourneNull() throws SQLException {
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        String erreur = taskService.createTask("board1", "Ma tâche", null, "standard", "");
        assertNull(erreur);
    }

    // ---- Tests déplacement ----

    @Test
    void moveTask_colonneInvalide_retourneErreur() throws SQLException {
        String erreur = taskService.moveTask("task1", "invalid_col");
        assertNotNull(erreur);
        assertTrue(erreur.contains("invalide") || erreur.contains("Colonne"));
    }

    @Test
    void moveTask_tacheIntrouvable_retourneErreur() throws SQLException {
        when(taskRepository.findById("task_inexistant")).thenReturn(null);

        String erreur = taskService.moveTask("task_inexistant", "doing");
        assertNotNull(erreur);
        assertTrue(erreur.contains("introuvable") || erreur.contains("Tâche"));
    }

    @Test
    void moveTask_versDoing_retourneNull() throws SQLException {
        Task task = buildTask("task1", "todo");
        when(taskRepository.findById("task1")).thenReturn(task);

        String erreur = taskService.moveTask("task1", "doing");
        assertNull(erreur);

        verify(taskRepository, times(1)).updateColumn("task1", "doing");
        verify(taskRepository, times(1)).saveHistory(any());
    }

    @Test
    void moveTask_versDone_retourneNull() throws SQLException {
        Task task = buildTask("task1", "doing");
        when(taskRepository.findById("task1")).thenReturn(task);

        String erreur = taskService.moveTask("task1", "done");
        assertNull(erreur);
        verify(taskRepository, times(1)).updateColumn("task1", "done");
    }

    // ---- Tests commentaire ----

    @Test
    void addComment_contenuVide_retourneErreur() throws SQLException {
        String erreur = taskService.addComment("task1", "user1", "");
        assertNotNull(erreur);
    }

    @Test
    void addComment_contenuNull_retourneErreur() throws SQLException {
        String erreur = taskService.addComment("task1", "user1", null);
        assertNotNull(erreur);
    }

    @Test
    void addComment_contenuValide_retourneNull() throws SQLException {
        String erreur = taskService.addComment("task1", "user1", "Super commentaire");
        assertNull(erreur);
        verify(taskRepository, times(1)).saveComment(any());
    }

    // ---- Tests suppression ----

    @Test
    void deleteTask_appelleRepositoryDelete() throws SQLException {
        taskService.deleteTask("task1");
        verify(taskRepository, times(1)).delete("task1");
    }

    // ---- Utilitaire ----

    private Task buildTask(String id, String columnId) {
        Task t = new Task();
        t.setId(id);
        t.setBoardId("board1");
        t.setTitle("Tâche test");
        t.setTypeId("standard");
        t.setColumnId(columnId);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}
