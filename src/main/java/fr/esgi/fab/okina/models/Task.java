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
public class Task {
    private String id;
    private String boardId;
    private String title;
    private String description;
    private String typeId;      // "standard", "bug", "spike", "amelioration"
    private String columnId;    // "todo", "doing", "done"
    private String assigneeId;
    private LocalDateTime createdAt;

    // Chargés séparément par le Repository
    private List<Comment> comments = new ArrayList<>();
    private List<Attachment> attachments = new ArrayList<>();
    private List<TaskHistory> history = new ArrayList<>();
}
