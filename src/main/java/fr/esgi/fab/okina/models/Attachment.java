package fr.esgi.fab.okina.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String id;
    private String taskId;
    private String filename;
    private long fileSize;
    private LocalDateTime createdAt;
}
