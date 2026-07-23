package fr.esgi.fab.okina.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String pseudo;
    private String email;
    private String hash;
    private String avatarColor;
    private LocalDateTime createdAt;

    /** Couleur d'avatar affichable, avec repli sur la couleur d'accent. */
    public String getColor() {
        return avatarColor == null || avatarColor.isBlank() ? "var(--accent)" : avatarColor;
    }
}
