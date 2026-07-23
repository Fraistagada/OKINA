package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService - tableaux et invitations")
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;
    @Mock
    private UserRepository userRepository;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardRepository, userRepository);
    }

    @Nested
    @DisplayName("Lectures")
    class Lectures {

        @Test
        @DisplayName("getBoardsForUser délègue au repository")
        void getBoardsForUser() throws SQLException {
            List<Board> tableaux = List.of(new Board());
            when(boardRepository.findByUserId("user-1")).thenReturn(tableaux);

            assertEquals(tableaux, boardService.getBoardsForUser("user-1"));
            verify(boardRepository).findByUserId("user-1");
        }

        @Test
        @DisplayName("getBoardById délègue au repository")
        void getBoardById() throws SQLException {
            Board board = new Board();
            when(boardRepository.findById("board-1")).thenReturn(board);

            assertSame(board, boardService.getBoardById("board-1"));
        }
    }

    @Nested
    @DisplayName("createBoard()")
    class CreateBoard {

        @Test
        @DisplayName("refuse un nom null")
        void nomNull() throws SQLException {
            assertEquals("Le nom du tableau est obligatoire.",
                    boardService.createBoard(null, "owner-1"));
            verify(boardRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un nom vide")
        void nomVide() throws SQLException {
            assertEquals("Le nom du tableau est obligatoire.",
                    boardService.createBoard("   ", "owner-1"));
            verify(boardRepository, never()).save(any());
        }

        @Test
        @DisplayName("crée un tableau valide avec nom nettoyé et propriétaire")
        void creationValide() throws SQLException {
            String resultat = boardService.createBoard("  Mon projet  ", "owner-1");

            assertNull(resultat);

            ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
            verify(boardRepository).save(captor.capture());
            Board sauvegarde = captor.getValue();

            assertEquals("Mon projet", sauvegarde.getName());
            assertEquals("owner-1", sauvegarde.getOwnerId());
            assertNotNull(sauvegarde.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("inviteMember()")
    class InviteMember {

        @Test
        @DisplayName("refuse si l'utilisateur invité est introuvable")
        void utilisateurIntrouvable() throws SQLException {
            when(userRepository.findByPseudo("bob")).thenReturn(Optional.empty());

            assertEquals("Utilisateur introuvable.",
                    boardService.inviteMember("board-1", "bob", "owner-1"));
            verify(boardRepository, never()).addMember(anyString(), anyString());
        }

        @Test
        @DisplayName("refuse si l'utilisateur est déjà membre")
        void dejaMembre() throws SQLException {
            User bob = utilisateur("user-bob", "bob");
            when(userRepository.findByPseudo("bob")).thenReturn(Optional.of(bob));
            when(boardRepository.isMember("board-1", "user-bob")).thenReturn(true);

            assertEquals("Cet utilisateur est déjà membre du tableau.",
                    boardService.inviteMember("board-1", "bob", "owner-1"));
            verify(boardRepository, never()).addMember(anyString(), anyString());
        }

        @Test
        @DisplayName("ajoute un membre valide")
        void invitationValide() throws SQLException {
            User bob = utilisateur("user-bob", "bob");
            when(userRepository.findByPseudo("bob")).thenReturn(Optional.of(bob));
            when(boardRepository.isMember("board-1", "user-bob")).thenReturn(false);

            assertNull(boardService.inviteMember("board-1", "bob", "owner-1"));
            verify(boardRepository).addMember("board-1", "user-bob");
        }

        private User utilisateur(String id, String pseudo) {
            return new User(id, pseudo, pseudo + "@mail.com", "hash", null, LocalDateTime.now());
        }
    }
}
