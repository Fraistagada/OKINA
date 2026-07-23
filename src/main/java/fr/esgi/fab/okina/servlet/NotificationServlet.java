package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.NotificationRepository;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Marque toutes les notifications de l'utilisateur connecté comme lues.
 * Appelé en fetch() à l'ouverture du panneau de notifications.
 */
@WebServlet("/notifications/read")
public class NotificationServlet extends HttpServlet {

    private NotificationRepository notificationRepository;

    @Override
    public void init() {
        notificationRepository = new NotificationRepository();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(401);
            return;
        }
        User user = (User) session.getAttribute("user");
        try {
            notificationRepository.markAllRead(user.getId());
            resp.setStatus(204);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }
}
