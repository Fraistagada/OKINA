package fr.esgi.fab.okina.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.esgi.fab.okina.models.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - inscription et connexion")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("refuse un pseudo vide")
        void pseudoVide() throws SQLException {
            assertEquals("Le pseudo est obligatoire.",
                    userService.register("   ", "alice@mail.com", "motdepasse"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refuse un pseudo null")
        void pseudoNull() throws SQLException {
            assertEquals("Le pseudo est obligatoire.",
                    userService.register(null, "alice@mail.com", "motdepasse"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refuse un email sans @")
        void emailInvalide() throws SQLException {
            assertEquals("L'email est invalide.",
                    userService.register("alice", "alice-mail.com", "motdepasse"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refuse un mot de passe de moins de 8 caractères")
        void motDePasseTropCourt() throws SQLException {
            assertEquals("Le mot de passe doit contenir au moins 8 caractères.",
                    userService.register("alice", "alice@mail.com", "1234567"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refuse un pseudo déjà utilisé")
        void pseudoDejaUtilise() throws SQLException {
            when(userRepository.existsByPseudo("alice")).thenReturn(true);

            assertEquals("Ce pseudo est déjà utilisé.",
                    userService.register("alice", "alice@mail.com", "motdepasse"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un email déjà utilisé")
        void emailDejaUtilise() throws SQLException {
            when(userRepository.existsByPseudo("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

            assertEquals("Cet email est déjà utilisé.",
                    userService.register("alice", "alice@mail.com", "motdepasse"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("inscrit un utilisateur valide et hache le mot de passe")
        void inscriptionValide() throws SQLException {
            when(userRepository.existsByPseudo("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@mail.com")).thenReturn(false);

            String resultat = userService.register("  alice  ", "  alice@mail.com  ", "monMotDePasse");

            assertNull(resultat, "Une inscription valide doit retourner null (aucune erreur)");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User sauvegarde = captor.getValue();

            // Les champs sont nettoyés (trim)
            assertEquals("alice", sauvegarde.getPseudo());
            assertEquals("alice@mail.com", sauvegarde.getEmail());
            assertNotNull(sauvegarde.getCreatedAt());

            // Le mot de passe ne doit JAMAIS être stocké en clair
            assertNotEquals("monMotDePasse", sauvegarde.getHash());
            // Le hash bcrypt doit être vérifiable
            assertTrue(BCrypt.verifyer()
                    .verify("monMotDePasse".toCharArray(), sauvegarde.getHash())
                    .verified);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("échoue si le pseudo est null")
        void pseudoNull() throws SQLException {
            assertTrue(userService.login(null, "motdepasse").isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("échoue si le mot de passe est null")
        void motDePasseNull() throws SQLException {
            assertTrue(userService.login("alice", null).isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("échoue si l'utilisateur n'existe pas")
        void utilisateurInconnu() throws SQLException {
            when(userRepository.findByPseudo("alice")).thenReturn(Optional.empty());
            assertTrue(userService.login("alice", "motdepasse").isEmpty());
        }

        @Test
        @DisplayName("échoue si le mot de passe est incorrect")
        void mauvaisMotDePasse() throws SQLException {
            when(userRepository.findByPseudo("alice")).thenReturn(Optional.of(utilisateurAvecMotDePasse("leBonMotDePasse")));
            assertTrue(userService.login("alice", "leMauvais").isEmpty());
        }

        @Test
        @DisplayName("réussit avec les bons identifiants")
        void connexionValide() throws SQLException {
            User attendu = utilisateurAvecMotDePasse("monMotDePasse");
            when(userRepository.findByPseudo("alice")).thenReturn(Optional.of(attendu));

            Optional<User> resultat = userService.login(" alice ", "monMotDePasse");

            assertTrue(resultat.isPresent());
            assertEquals(attendu.getId(), resultat.get().getId());
        }

        private User utilisateurAvecMotDePasse(String motDePasseEnClair) {
            String hash = BCrypt.withDefaults().hashToString(12, motDePasseEnClair.toCharArray());
            return new User("id-1", "alice", "alice@mail.com", hash, LocalDateTime.now());
        }
    }
}
