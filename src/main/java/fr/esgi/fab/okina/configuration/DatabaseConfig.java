package fr.esgi.fab.okina.configuration;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Initialise la base H2 au démarrage de l'application.
 * Le pool de connexions est stocké dans le contexte servlet
 * pour être accessible depuis tous les Repository.
 */
@WebListener
public class DatabaseConfig implements ServletContextListener {

    private static JdbcConnectionPool pool;

    public static Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[DatabaseConfig] Initialisation de la base H2...");

        // Base de données fichier, elle survit aux redémarrages Tomcat
        // Pour repartir de zéro : supprimer le fichier ~/okina-kanban.mv.db
        pool = JdbcConnectionPool.create(
                "jdbc:h2:~/okina-kanban;AUTO_SERVER=TRUE",
                "sa",
                ""
        );

        runSchema();

        sce.getServletContext().setAttribute("dbPool", pool);
        System.out.println("[DatabaseConfig] Base H2 prête.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (pool != null) {
            pool.dispose();
        }
    }

    // Lit et exécute le fichier schema.sql placé dans src/main/resources/.
    private void runSchema() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                System.err.println("[DatabaseConfig] schema.sql introuvable dans les resources !");
                return;
            }
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = pool.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("[DatabaseConfig] schema.sql exécuté avec succès.");
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Impossible d'initialiser la base de données", e);
        }
    }
}