package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.NotificationRepository;
import fr.esgi.fab.okina.repository.TaskRepository;
import fr.esgi.fab.okina.repository.UserRepository;
import fr.esgi.fab.okina.service.BoardService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/boards")
public class BoardsServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private BoardService boardService;
    private TaskRepository taskRepository;
    private NotificationRepository notificationRepository;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        boardService = new BoardService(new BoardRepository(), new UserRepository());
        taskRepository = new TaskRepository();
        notificationRepository = new NotificationRepository();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        try {
            List<Board> boards = boardService.getBoardsForUser(user.getId());
            // Récap des tâches par tableau : [àfaire, encours, terminé, total]
            Map<String, int[]> stats = new HashMap<>();
            for (Board board : boards) {
                stats.put(board.getId(), computeStats(board.getId()));
            }

            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("user", user);
            ctx.setVariable("boards", boards);
            ctx.setVariable("myBoards", boards); // pour la sidebar partagée
            ctx.setVariable("stats", stats);
            if ("true".equals(req.getParameter("created"))) {
                ctx.setVariable("success", "Tableau créé avec succès !");
            }
            String erreur = messageForError(req.getParameter("erreur"));
            if (erreur != null) {
                ctx.setVariable("erreur", erreur);
            }
            // Panneau de notifications
            ctx.setVariable("notifications", notificationRepository.findByUser(user.getId(), 15));
            ctx.setVariable("unreadCount", notificationRepository.countUnread(user.getId()));
            templateEngine.process("boards", ctx, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    // Compte les tâches d'un tableau par colonne
    private int[] computeStats(String boardId) throws java.sql.SQLException {
        int todo = 0, doing = 0, done = 0;
        List<Task> tasks = taskRepository.findByBoardId(boardId);
        for (Task t : tasks) {
            String col = t.getColumnId();
            if ("todo".equals(col)) todo++;
            else if ("doing".equals(col)) doing++;
            else if ("done".equals(col)) done++;
        }
        return new int[]{todo, doing, done, tasks.size()};
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        // La création de tableau passe obligatoirement par le paiement Stripe :
        // POST /checkout -> page Stripe -> /checkout/success (qui crée le tableau).
        // Ce chemin direct est bloqué pour empêcher de contourner le paiement.
        resp.sendRedirect(req.getContextPath() + "/boards?erreur=paiement");
    }

    // Traduit le code d'erreur de l'URL en message affichable
    private String messageForError(String code) {
        if (code == null) return null;
        return switch (code) {
            case "paiement" -> "Le paiement n'a pas pu être vérifié. Veuillez réessayer.";
            case "annule" -> "Paiement annulé. Le tableau n'a pas été créé.";
            case "stripe" -> "Erreur de connexion à Stripe. Veuillez réessayer.";
            case "nom" -> "Le nom du tableau est obligatoire.";
            case "creation" -> "Erreur lors de la création du tableau.";
            default -> "Une erreur est survenue.";
        };
    }

    // Vérifie la session, redirige vers /login si pas connecté
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
