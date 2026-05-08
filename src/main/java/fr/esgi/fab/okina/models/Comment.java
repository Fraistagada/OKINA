package fr.esgi.fab.okina.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private String id;
    private String taskId;
    private String authorId;
    private String content;
    private LocalDateTime createdAt;
}
