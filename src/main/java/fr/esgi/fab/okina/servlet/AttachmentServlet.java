package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.Attachment;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.TaskRepository;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Téléchargement d'une pièce jointe : /attachment?id=...
 *
 * Le fichier n'est servi que si l'utilisateur connecté est membre
 * du tableau auquel appartient la tâche de la pièce jointe.
 */
@WebServlet("/attachment")
public class AttachmentServlet extends HttpServlet {

    private TaskRepository taskRepository;

    @Override
    public void init() {
        taskRepository = new TaskRepository();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        String id = req.getParameter("id");
        if (id == null || id.isBlank()) {
            resp.sendError(400);
            return;
        }

        try {
            Attachment attachment = taskRepository.findAttachmentIfMember(id, user.getId());
            if (attachment == null) {
                resp.sendError(404);
                return;
            }

            byte[] data = taskRepository.findAttachmentData(id);
            if (data == null) {
                // Pièce jointe créée avant l'ajout du stockage de contenu
                resp.sendError(404, "Contenu indisponible : fichier ajouté avant la mise à jour.");
                return;
            }

            String encodedName = URLEncoder.encode(attachment.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            resp.setContentType("application/octet-stream");
            resp.setContentLengthLong(data.length);
            resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            resp.getOutputStream().write(data);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }
}
