package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.UserRepository;
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
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private UserService userService;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        userService = new UserService(new UserRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Si déjà connecté, on redirige directement vers les tableaux
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/boards");
            return;
        }

        WebContext ctx = buildContext(req, resp);
        // Petit message de confirmation après inscription
        if ("true".equals(req.getParameter("registered"))) {
            ctx.setVariable("success", "Inscription réussie ! Connectez-vous.");
        }
        templateEngine.process("login", ctx, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pseudo = req.getParameter("pseudo");
        String password = req.getParameter("password");

        try {
            Optional<User> userOpt = userService.login(pseudo, password);
            if (userOpt.isEmpty()) {
                WebContext ctx = buildContext(req, resp);
                ctx.setVariable("erreur", "Pseudo ou mot de passe incorrect.");
                ctx.setVariable("pseudo", pseudo);
                templateEngine.process("login", ctx, resp.getWriter());
            } else {
                // On stocke l'utilisateur dans la session
                HttpSession session = req.getSession(true);
                session.setAttribute("user", userOpt.get());
                resp.sendRedirect(req.getContextPath() + "/boards");
            }
        } catch (Exception e) {
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("erreur", "Une erreur est survenue, réessayez.");
            templateEngine.process("login", ctx, resp.getWriter());
        }
    }

    private WebContext buildContext(HttpServletRequest req, HttpServletResponse resp) {
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(getServletContext());
        return new WebContext(app.buildExchange(req, resp));
    }
}
