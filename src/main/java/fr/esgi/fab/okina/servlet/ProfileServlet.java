package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.NotificationRepository;
import fr.esgi.fab.okina.repository.UserRepository;
import fr.esgi.fab.okina.service.BoardService;
import fr.esgi.fab.okina.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Page profil : modification du pseudo/email, du mot de passe
 * et de la couleur d'avatar.
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private UserService userService;
    private UserRepository userRepository;
    private BoardService boardService;
    private NotificationRepository notificationRepository;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        userRepository = new UserRepository();
        userService = new UserService(userRepository);
        boardService = new BoardService(new BoardRepository(), new UserRepository());
        notificationRepository = new NotificationRepository();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        try {
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("user", user);
            ctx.setVariable("myBoards", boardService.getBoardsForUser(user.getId()));
            ctx.setVariable("avatarColors", UserService.AVATAR_COLORS);
            ctx.setVariable("notifications", notificationRepository.findByUser(user.getId(), 15));
            ctx.setVariable("unreadCount", notificationRepository.countUnread(user.getId()));
            if (req.getParameter("success") != null) {
                ctx.setVariable("success", req.getParameter("success"));
            }
            if (req.getParameter("erreur") != null) {
                ctx.setVariable("erreur", req.getParameter("erreur"));
            }
            templateEngine.process("profile", ctx, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        String action = req.getParameter("action");
        try {
            String erreur;
            String success;
            switch (action == null ? "" : action) {
                case "updateProfile" -> {
                    erreur = userService.updateProfile(
                            user.getId(), req.getParameter("pseudo"), req.getParameter("email"));
                    success = "Profil mis à jour.";
                }
                case "updatePassword" -> {
                    String newPassword = req.getParameter("newPassword");
                    String confirm = req.getParameter("confirmPassword");
                    if (newPassword != null && !newPassword.equals(confirm)) {
                        erreur = "La confirmation ne correspond pas au nouveau mot de passe.";
                    } else {
                        erreur = userService.updatePassword(
                                user.getId(), req.getParameter("currentPassword"), newPassword);
                    }
                    success = "Mot de passe modifié.";
                }
                case "updateColor" -> {
                    erreur = userService.updateAvatarColor(user.getId(), req.getParameter("color"));
                    success = "Couleur d'avatar mise à jour.";
                }
                default -> {
                    resp.sendError(400, "Action inconnue");
                    return;
                }
            }

            if (erreur != null) {
                redirectWith(req, resp, "erreur", erreur);
            } else {
                refreshSessionUser(req, user.getId());
                redirectWith(req, resp, "success", success);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    // Recharge l'utilisateur en session après modification
    private void refreshSessionUser(HttpServletRequest req, String userId) throws Exception {
        userRepository.findById(userId)
                .ifPresent(fresh -> req.getSession().setAttribute("user", fresh));
    }

    private void redirectWith(HttpServletRequest req, HttpServletResponse resp,
                              String key, String message) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/profile?" + key + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private User getConnectedUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return (User) session.getAttribute("user");
    }

    private WebContext buildContext(HttpServletRequest req, HttpServletResponse resp) {
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(getServletContext());
        return new WebContext(app.buildExchange(req, resp));
    }
}
