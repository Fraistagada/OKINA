package fr.esgi.fab.okina.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

@WebServlet(name = "helloServlet", value = {"/index", "/"})

public class HelloServlet extends HttpServlet {
    private TemplateEngine templateEngine = null;

    public void init() {
        IO.println("Initialisation de la servlet HelloServlet");
        String message = "Hello Kanban";
        // On récupère le moteur de template dans le contexte des servlets
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
    }

    // la méthode doGet ci-dessous sera invoquée par Tomcat
    // Tomcat invoque cette méthode car elle prend en charge toutes
    // les requêtes HTTP utilisant la méthode GET vers l'url index et vers l’url /
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // On crée un context Thymeleaf qui va accueillir les objets Java
        // qui seront envoyés à la vue Thymeleaf
        Context context = new Context();
        // On invoque la méthode process qui formule la réponse qui sera renvoyée au navigateur
        templateEngine.process("hello", context, response.getWriter());
    }

    public void destroy() {
    }
}