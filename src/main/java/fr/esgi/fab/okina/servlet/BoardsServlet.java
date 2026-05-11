package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
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
import java.util.List;

@WebServlet("/boards")
public class BoardsServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private BoardService boardService;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        boardService = new BoardService(new BoardRepository(), new UserRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        try {
            List<Board> boards = boardService.getBoardsForUser(user.getId());
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("user", user);
            ctx.setVariable("boards", boards);
            if ("true".equals(req.getParameter("created"))) {
                ctx.setVariable("success", "Tableau créé avec succès !");
            }
            templateEngine.process("boards", ctx, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getConnectedUser(req, resp);
        if (user == null) return;

        String name = req.getParameter("name");
        // On vérifie que le "paiement" Stripe sandbox a été confirmé
        String stripePaid = req.getParameter("stripePaid");

        if (!"true".equals(stripePaid)) {
            // Ne devrait pas arriver, mais sécurité côté serveur
            resp.sendRedirect(req.getContextPath() + "/boards?erreur=paiement");
            return;
        }

        try {
            String erreur = boardService.createBoard(name, user.getId());
            if (erreur != null) {
                List<Board> boards = boardService.getBoardsForUser(user.getId());
                WebContext ctx = buildContext(req, resp);
                ctx.setVariable("user", user);
                ctx.setVariable("boards", boards);
                ctx.setVariable("erreur", erreur);
                ctx.setVariable("showModal", true);
                templateEngine.process("boards", ctx, resp.getWriter());
            } else {
                resp.sendRedirect(req.getContextPath() + "/boards?created=true");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
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
