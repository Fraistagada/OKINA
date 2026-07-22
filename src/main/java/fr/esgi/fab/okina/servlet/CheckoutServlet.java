package fr.esgi.fab.okina.servlet;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Démarre un paiement Stripe pour la création d'un tableau.
 *
 * On utilise une Checkout Session en mode "setup" : Stripe valide la carte
 * du client sans aucun débit (montant 0 €). C'est le flux recommandé pour
 * démontrer que la connexion à l'API Stripe fonctionne.
 *
 * Le nom du tableau est mémorisé en session pour être réutilisé au retour
 * de Stripe (voir CheckoutSuccessServlet).
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    static final String SESSION_BOARD_NAME = "pendingBoardName";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String name = req.getParameter("name");
        if (name == null || name.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/boards?erreur=nom");
            return;
        }

        // On garde le nom du tableau côté serveur, jamais dans l'URL de retour
        session.setAttribute(SESSION_BOARD_NAME, name.trim());

        String baseUrl = buildBaseUrl(req);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SETUP)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setSuccessUrl(baseUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(baseUrl + "/boards?erreur=annule")
                    .build();

            Session checkoutSession = Session.create(params);
            resp.sendRedirect(checkoutSession.getUrl());
        } catch (StripeException e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/boards?erreur=stripe");
        }
    }

    // Reconstitue l'URL de base de l'application (scheme://host[:port]/contextPath)
    private String buildBaseUrl(HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        if (!defaultPort) {
            sb.append(":").append(port);
        }
        sb.append(req.getContextPath());
        return sb.toString();
    }
}
