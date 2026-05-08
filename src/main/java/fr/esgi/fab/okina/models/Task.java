package fr.esgi.fab.okina.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id;
    private String boardId;
    private String title;
    private String description;
    private String typeId;
    private String columnId;
    private String assigneeId;
    private LocalDateTime createdAt;

    private List<Comment> comments = new ArrayList<>();
    private List<Attachment> attachments = new ArrayList<>();
    private List<TaskHistory> history = new ArrayList<>();
}
