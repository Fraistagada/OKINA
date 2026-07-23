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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page de statistiques globales : agrège les tâches de tous les tableaux
 * de l'utilisateur (répartition par type, par colonne, avancement par tableau).
 */
@WebServlet("/stats")
public class StatsServlet extends HttpServlet {

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
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        try {
            List<Board> boards = boardService.getBoardsForUser(user.getId());

            Map<String, Integer> types = new HashMap<>(
                    Map.of("standard", 0, "bug", 0, "spike", 0, "amelioration", 0));
            Map<String, Integer> columns = new HashMap<>(
                    Map.of("todo", 0, "doing", 0, "done", 0));
            List<Map<String, Object>> perBoard = new ArrayList<>();
            int totalTasks = 0;

            for (Board board : boards) {
                List<Task> tasks = taskRepository.findByBoardId(board.getId());
                int done = 0;
                for (Task t : tasks) {
                    types.merge(t.getTypeId(), 1, Integer::sum);
                    columns.merge(t.getColumnId(), 1, Integer::sum);
                    if ("done".equals(t.getColumnId())) done++;
                }
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", board.getName());
                entry.put("total", tasks.size());
                entry.put("done", done);
                perBoard.add(entry);
                totalTasks += tasks.size();
            }

            int totalDone = columns.getOrDefault("done", 0);
            int completion = totalTasks > 0 ? totalDone * 100 / totalTasks : 0;

            Map<String, Object> statsData = new HashMap<>();
            statsData.put("types", types);
            statsData.put("columns", columns);
            statsData.put("boards", perBoard);

            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("user", user);
            ctx.setVariable("myBoards", boards);
            ctx.setVariable("totalBoards", boards.size());
            ctx.setVariable("totalTasks", totalTasks);
            ctx.setVariable("totalDone", totalDone);
            ctx.setVariable("completion", completion);
            ctx.setVariable("statsData", statsData);
            ctx.setVariable("notifications", notificationRepository.findByUser(user.getId(), 15));
            ctx.setVariable("unreadCount", notificationRepository.countUnread(user.getId()));
            templateEngine.process("stats", ctx, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    private WebContext buildContext(HttpServletRequest req, HttpServletResponse resp) {
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(getServletContext());
        return new WebContext(app.buildExchange(req, resp));
    }
}
