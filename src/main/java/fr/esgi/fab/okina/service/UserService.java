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

    /** Couleurs d'avatar autorisées (mêmes que la palette de l'interface). */
    public static final java.util.List<String> AVATAR_COLORS = java.util.List.of(
            "#6366f1", "#14b8a6", "#f59e0b", "#ec4899", "#3b82f6", "#a855f7");

    /**
     * Met à jour pseudo et email.
     * Retourne un message d'erreur, ou null si tout va bien.
     */
    public String updateProfile(String userId, String pseudo, String email) throws SQLException {
        if (pseudo == null || pseudo.trim().isEmpty()) {
            return "Le pseudo est obligatoire.";
        }
        if (email == null || !email.contains("@")) {
            return "L'email est invalide.";
        }
        Optional<User> currentOpt = userRepository.findById(userId);
        if (currentOpt.isEmpty()) return "Utilisateur introuvable.";
        User current = currentOpt.get();

        // Unicité, uniquement si la valeur change
        if (!pseudo.trim().equals(current.getPseudo()) && userRepository.existsByPseudo(pseudo.trim())) {
            return "Ce pseudo est déjà utilisé.";
        }
        if (!email.trim().equalsIgnoreCase(current.getEmail()) && userRepository.existsByEmail(email.trim())) {
            return "Cet email est déjà utilisé.";
        }
        userRepository.updateProfile(userId, pseudo.trim(), email.trim());
        return null;
    }

    /**
     * Change le mot de passe après vérification de l'actuel.
     */
    public String updatePassword(String userId, String currentPassword, String newPassword) throws SQLException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return "Utilisateur introuvable.";

        BCrypt.Result result = BCrypt.verifyer()
                .verify(currentPassword == null ? new char[0] : currentPassword.toCharArray(),
                        userOpt.get().getHash());
        if (!result.verified) {
            return "Le mot de passe actuel est incorrect.";
        }
        if (newPassword == null || newPassword.length() < 8) {
            return "Le nouveau mot de passe doit contenir au moins 8 caractères.";
        }
        String hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        userRepository.updatePassword(userId, hash);
        return null;
    }

    /**
     * Change la couleur d'avatar (limitée à la palette autorisée).
     */
    public String updateAvatarColor(String userId, String color) throws SQLException {
        if (color == null || !AVATAR_COLORS.contains(color)) {
            return "Couleur invalide.";
        }
        userRepository.updateAvatarColor(userId, color);
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