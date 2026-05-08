package fr.esgi.fab.okina.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private String id;
    private String name;
    private String ownerId;
    private LocalDateTime createdAt;

    // Chargés séparément par le Repository
    private List<User> members = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();
}
