package fr.esgi.fab.okina.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String userId;
    /** Références souples (pas de FK) : servent à construire le lien de navigation. */
    private String boardId;
    private String taskId;
    private String text;
    private boolean read;
    private LocalDateTime createdAt;

    /** Date affichable dans le panneau. */
    public String getWhen() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM 'à' HH:mm"));
    }
}
