package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.TaskRepository;
import fr.esgi.fab.okina.repository.UserRepository;
import fr.esgi.fab.okina.service.BoardService;
import fr.esgi.fab.okina.service.TaskService;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.util.List;

@WebServlet("/board")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 5 Mo max par fichier
public class BoardServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private TaskService taskService;
    private BoardService boardService;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        taskService = new TaskService(new TaskRepository());
        boardService = new BoardService(new BoardRepository(), new UserRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUser(req, resp);
        if (user == null) return;

        String boardId = req.getParameter("id");
        try {
            Board board = boardService.getBoardById(boardId);
            if (board == null) {
                resp.sendError(404);
                return;
            }

            // Vérifie que l'utilisateur est bien membre
            if (board.getMembers().stream().noneMatch(m -> m.getId().equals(user.getId()))) {
                resp.sendError(403);
                return;
            }

            List<Task> tasks = taskService.getTasksForBoard(boardId);
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("user", user);
            ctx.setVariable("board", board);
            ctx.setVariable("tasks", tasks);
            ctx.setVariable("countTodo", tasks.stream().filter(t -> "todo".equals(t.getColumnId())).count());
            ctx.setVariable("countDoing", tasks.stream().filter(t -> "doing".equals(t.getColumnId())).count());
            ctx.setVariable("countDone", tasks.stream().filter(t -> "done".equals(t.getColumnId())).count());
            ctx.setVariable("contextPath", req.getContextPath());
            // Passe les membres pour le sélecteur "assigner à"
            ctx.setVariable("members", board.getMembers());
            templateEngine.process("board", ctx, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUser(req, resp);
        if (user == null) return;

        String action = req.getParameter("action");
        String boardId = req.getParameter("boardId");

        try {
            switch (action == null ? "" : action) {

                case "createTask" -> {
                    taskService.createTask(
                            boardId,
                            req.getParameter("title"),
                            req.getParameter("description"),
                            req.getParameter("typeId"),
                            req.getParameter("assigneeId")
                    );
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "moveTask" -> {
                    taskService.moveTask(req.getParameter("taskId"), req.getParameter("columnId"));
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "deleteTask" -> {
                    taskService.deleteTask(req.getParameter("taskId"));
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "addComment" -> {
                    taskService.addComment(
                            req.getParameter("taskId"),
                            user.getId(),
                            req.getParameter("content")
                    );
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId
                            + "&task=" + req.getParameter("taskId"));
                }

                case "addAttachment" -> {
                    Part filePart = req.getPart("file");
                    if (filePart != null && filePart.getSize() > 0) {
                        new TaskRepository().saveAttachment(
                                buildAttachment(req.getParameter("taskId"), filePart)
                        );
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId
                            + "&task=" + req.getParameter("taskId"));
                }

                case "invite" -> {
                    boardService.inviteMember(boardId, req.getParameter("pseudo"), user.getId());
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                default -> resp.sendError(400, "Action inconnue");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    private fr.esgi.fab.okina.models.Attachment buildAttachment(String taskId, Part filePart)
            throws IOException {
        fr.esgi.fab.okina.models.Attachment a = new fr.esgi.fab.okina.models.Attachment();
        a.setTaskId(taskId);
        a.setFilename(filePart.getSubmittedFileName());
        a.setFileSize(filePart.getSize());
        a.setCreatedAt(java.time.LocalDateTime.now());
        return a;
    }

    private User getUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
