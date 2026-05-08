package fr.esgi.fab.okina.model;

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
    private String password; // hashé en bcrypt
    private LocalDateTime createdAt;
}
