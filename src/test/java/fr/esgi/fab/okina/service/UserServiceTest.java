package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    // ---- Tests inscription ----

    @Test
    void register_pseudoVide_retourneErreur() throws SQLException {
        String erreur = userService.register("", "test@test.fr", "motdepasse123");
        assertNotNull(erreur);
        assertTrue(erreur.contains("pseudo"));
    }

    @Test
    void register_emailInvalide_retourneErreur() throws SQLException {
        String erreur = userService.register("alice", "pasunmail", "motdepasse123");
        assertNotNull(erreur);
        assertTrue(erreur.contains("email") || erreur.contains("mail"));
    }

    @Test
    void register_motDePasseTropCourt_retourneErreur() throws SQLException {
        String erreur = userService.register("alice", "alice@test.fr", "court");
        assertNotNull(erreur);
        assertTrue(erreur.contains("8"));
    }

    @Test
    void register_pseudoDejaUtilise_retourneErreur() throws SQLException {
        when(userRepository.existsByPseudo("alice")).thenReturn(true);

        String erreur = userService.register("alice", "alice@test.fr", "motdepasse123");
        assertNotNull(erreur);
        assertTrue(erreur.contains("pseudo") || erreur.contains("utilisé"));
    }

    @Test
    void register_emailDejaUtilise_retourneErreur() throws SQLException {
        when(userRepository.existsByPseudo("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.fr")).thenReturn(true);

        String erreur = userService.register("alice", "alice@test.fr", "motdepasse123");
        assertNotNull(erreur);
        assertTrue(erreur.contains("email") || erreur.contains("mail"));
    }

    @Test
    void register_donneesValides_retourneNull() throws SQLException {
        when(userRepository.existsByPseudo("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.fr")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String erreur = userService.register("alice", "alice@test.fr", "motdepasse123");
        assertNull(erreur);

        verify(userRepository, times(1)).save(any(User.class));
    }

    // ---- Tests connexion ----

    @Test
    void login_pseudoInexistant_retourneEmpty() throws SQLException {
        when(userRepository.findByPseudo("inconnu")).thenReturn(Optional.empty());

        Optional<User> result = userService.login("inconnu", "motdepasse123");
        assertTrue(result.isEmpty());
    }

    @Test
    void login_mauvaisMotDePasse_retourneEmpty() throws SQLException {
        User user = new User();
        user.setId("1");
        user.setPseudo("alice");
        user.setHash(at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                .hashToString(12, "autremotdepasse".toCharArray()));
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByPseudo("alice")).thenReturn(Optional.of(user));

        Optional<User> result = userService.login("alice", "motdepasse123");
        assertTrue(result.isEmpty());
    }

    @Test
    void login_bonnesCredentials_retourneUser() throws SQLException {
        String password = "motdepasse123";
        User user = new User();
        user.setId("1");
        user.setPseudo("alice");
        user.setHash(at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                .hashToString(12, password.toCharArray()));
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByPseudo("alice")).thenReturn(Optional.of(user));

        Optional<User> result = userService.login("alice", password);
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getPseudo());
    }

    @Test
    void login_pseudoNull_retourneEmpty() throws SQLException {
        Optional<User> result = userService.login(null, "motdepasse123");
        assertTrue(result.isEmpty());
    }
}
