package fr.esgi.fab.okina.configuration;

import com.stripe.Stripe;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configure la connexion à l'API Stripe au démarrage de l'application.
 *
 * La clé secrète est lue, dans cet ordre, depuis :
 *   1. la variable d'environnement STRIPE_SECRET_KEY
 *   2. la propriété système stripe.secret.key (-Dstripe.secret.key=sk_...)
 *   3. le fichier stripe.properties sur le classpath (exclu de Git)
 *
 * On ne stocke jamais la clé en dur dans le code.
 */
@WebListener
public class StripeConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String secretKey = System.getenv("STRIPE_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = System.getProperty("stripe.secret.key");
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = loadFromProperties();
        }

        if (secretKey == null || secretKey.isBlank()) {
            System.err.println("[StripeConfig] Aucune clé secrète Stripe trouvée. "
                    + "Définissez la variable d'environnement STRIPE_SECRET_KEY "
                    + "(ou -Dstripe.secret.key=sk_...). Les paiements échoueront.");
            return;
        }

        Stripe.apiKey = secretKey;
        System.out.println("[StripeConfig] Clé Stripe chargée (mode "
                + (secretKey.startsWith("sk_live") ? "LIVE" : "TEST") + ").");
    }

    // Lit la clé depuis stripe.properties (placé dans src/main/resources/)
    private String loadFromProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("stripe.properties")) {
            if (is == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("stripe.secret.key");
        } catch (Exception e) {
            System.err.println("[StripeConfig] Erreur de lecture de stripe.properties : " + e.getMessage());
            return null;
        }
    }
}
