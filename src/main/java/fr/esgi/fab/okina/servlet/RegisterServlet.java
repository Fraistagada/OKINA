package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.repository.UserRepository;
import fr.esgi.fab.okina.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private UserService userService;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        userService = new UserService(new UserRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        WebContext ctx = buildContext(req, resp);
        templateEngine.process("register", ctx, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pseudo = req.getParameter("pseudo");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        try {
            String erreur = userService.register(pseudo, email, password);
            if (erreur != null) {
                // Erreur de validation : on réaffiche le formulaire avec le message
                WebContext ctx = buildContext(req, resp);
                ctx.setVariable("erreur", erreur);
                ctx.setVariable("pseudo", pseudo);
                ctx.setVariable("email", email);
                templateEngine.process("register", ctx, resp.getWriter());
            } else {
                // Inscription OK : on redirige vers la page de connexion
                resp.sendRedirect(req.getContextPath() + "/login?registered=true");
            }
        } catch (Exception e) {
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("erreur", "Une erreur est survenue, réessayez.");
            templateEngine.process("register", ctx, resp.getWriter());
        }
    }

    private WebContext buildContext(HttpServletRequest req, HttpServletResponse resp) {
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(getServletContext());
        return new WebContext(app.buildExchange(req, resp));
    }
}