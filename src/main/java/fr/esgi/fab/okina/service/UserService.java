package fr.esgi.fab.okina.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.UserRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Inscrit un nouvel utilisateur.
     * Retourne un message d'erreur si les données sont invalides, null si tout va bien.
     */
    public String register(String pseudo, String email, String password) throws SQLException {
        if (pseudo == null || pseudo.trim().isEmpty()) {
            return "Le pseudo est obligatoire.";
        }
        if (email == null || !email.contains("@")) {
            return "L'email est invalide.";
        }
        if (password == null || password.length() < 8) {
            return "Le mot de passe doit contenir au moins 8 caractères.";
        }
        if (userRepository.existsByPseudo(pseudo.trim())) {
            return "Ce pseudo est déjà utilisé.";
        }
        if (userRepository.existsByEmail(email.trim())) {
            return "Cet email est déjà utilisé.";
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        User user = new User();
        user.setPseudo(pseudo.trim());
        user.setEmail(email.trim());
        user.setHash(hashedPassword);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return null;
    }

    /**
     * Tente une connexion.
     * Retourne l'utilisateur si les credentials sont bons, Optional.empty() sinon.
     */
    public Optional<User> login(String pseudo, String password) throws SQLException {
        if (pseudo == null || password == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByPseudo(pseudo.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getHash());
        if (!result.verified) {
            return Optional.empty();
        }
        return Optional.of(user);
    }
}