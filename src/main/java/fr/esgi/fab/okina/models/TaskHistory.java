package fr.esgi.fab.okina.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistory {
    private String id;
    private String taskId;
    private String text;
    private LocalDateTime createdAt;
}
