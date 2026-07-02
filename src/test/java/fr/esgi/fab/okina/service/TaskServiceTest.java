package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.Comment;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.TaskHistory;
import fr.esgi.fab.okina.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService - tâches, déplacements et commentaires")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("refuse un titre vide")
        void titreVide() throws SQLException {
            assertEquals("Le titre est obligatoire.",
                    taskService.createTask("board-1", "  ", "desc", "standard", "user-1"));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un type vide")
        void typeVide() throws SQLException {
            assertEquals("Le type est obligatoire.",
                    taskService.createTask("board-1", "Titre", "desc", "  ", "user-1"));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("crée une tâche placée dans la colonne 'todo' et enregistre l'historique")
        void creationValide() throws SQLException {
            String resultat = taskService.createTask(
                    "board-1", "  Ma tâche  ", "une description", "bug", "user-1");

            assertNull(resultat);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(taskCaptor.capture());
            Task sauvegarde = taskCaptor.getValue();

            assertEquals("board-1", sauvegarde.getBoardId());
            assertEquals("Ma tâche", sauvegarde.getTitle()); // titre nettoyé
            assertEquals("bug", sauvegarde.getTypeId());
            assertEquals("todo", sauvegarde.getColumnId()); // toujours 'todo' à la création
            assertEquals("user-1", sauvegarde.getAssigneeId());
            assertNotNull(sauvegarde.getCreatedAt());

            // Une entrée d'historique doit être créée
            verify(taskRepository).saveHistory(any(TaskHistory.class));
        }

        @Test
        @DisplayName("met assigneeId à null quand il est vide")
        void assigneeVideDevientNull() throws SQLException {
            taskService.createTask("board-1", "Titre", "desc", "standard", "");

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            assertNull(captor.getValue().getAssigneeId());
        }
    }

    @Nested
    @DisplayName("moveTask()")
    class MoveTask {

        @Test
        @DisplayName("refuse une colonne invalide")
        void colonneInvalide() throws SQLException {
            assertEquals("Colonne invalide.", taskService.moveTask("task-1", "archive"));
            verify(taskRepository, never()).updateColumn(anyString(), anyString());
        }

        @Test
        @DisplayName("refuse si la tâche est introuvable")
        void tacheIntrouvable() throws SQLException {
            when(taskRepository.findById("task-1")).thenReturn(null);

            assertEquals("Tâche introuvable.", taskService.moveTask("task-1", "doing"));
            verify(taskRepository, never()).updateColumn(anyString(), anyString());
        }

        @Test
        @DisplayName("déplace la tâche et enregistre l'historique")
        void deplacementValide() throws SQLException {
            when(taskRepository.findById("task-1")).thenReturn(new Task());

            assertNull(taskService.moveTask("task-1", "done"));

            verify(taskRepository).updateColumn("task-1", "done");
            verify(taskRepository).saveHistory(any(TaskHistory.class));
        }
    }

    @Test
    @DisplayName("deleteTask délègue la suppression au repository")
    void deleteTask() throws SQLException {
        taskService.deleteTask("task-1");
        verify(taskRepository).delete("task-1");
    }

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @Test
        @DisplayName("refuse un commentaire vide")
        void commentaireVide() throws SQLException {
            assertEquals("Le commentaire est vide.",
                    taskService.addComment("task-1", "user-1", "   "));
            verify(taskRepository, never()).saveComment(any());
        }

        @Test
        @DisplayName("enregistre un commentaire valide avec contenu nettoyé")
        void commentaireValide() throws SQLException {
            assertNull(taskService.addComment("task-1", "user-1", "  Bien joué !  "));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(taskRepository).saveComment(captor.capture());
            Comment c = captor.getValue();

            assertEquals("task-1", c.getTaskId());
            assertEquals("user-1", c.getAuthorId());
            assertEquals("Bien joué !", c.getContent());
            assertNotNull(c.getCreatedAt());
        }
    }
}
