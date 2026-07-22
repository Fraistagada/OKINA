package fr.esgi.fab.okina.servlet;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.UserRepository;
import fr.esgi.fab.okina.service.BoardService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Point de retour après un paiement Stripe réussi.
 *
 * Stripe redirige ici avec ?session_id=... On vérifie que la session de
 * paiement est bien finalisée, puis on crée réellement le tableau à partir
 * du nom mémorisé en session (voir CheckoutServlet).
 */
@WebServlet("/checkout/success")
public class CheckoutSuccessServlet extends HttpServlet {

    private BoardService boardService;

    @Override
    public void init() {
        boardService = new BoardService(new BoardRepository(), new UserRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        String sessionId = req.getParameter("session_id");
        String boardName = (String) session.getAttribute(CheckoutServlet.SESSION_BOARD_NAME);

        if (sessionId == null || boardName == null) {
            resp.sendRedirect(req.getContextPath() + "/boards?erreur=paiement");
            return;
        }

        try {
            // On vérifie auprès de Stripe que la session est bien finalisée
            Session checkoutSession = Session.retrieve(sessionId);
            if (!"complete".equals(checkoutSession.getStatus())) {
                resp.sendRedirect(req.getContextPath() + "/boards?erreur=paiement");
                return;
            }

            String erreur = boardService.createBoard(boardName, user.getId());
            session.removeAttribute(CheckoutServlet.SESSION_BOARD_NAME);

            if (erreur != null) {
                resp.sendRedirect(req.getContextPath() + "/boards?erreur=creation");
            } else {
                resp.sendRedirect(req.getContextPath() + "/boards?created=true");
            }
        } catch (StripeException e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/boards?erreur=stripe");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }
}
