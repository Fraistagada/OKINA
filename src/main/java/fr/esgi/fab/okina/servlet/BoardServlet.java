package fr.esgi.fab.okina.servlet;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.models.Notification;
import fr.esgi.fab.okina.models.Task;
import fr.esgi.fab.okina.models.User;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.NotificationRepository;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/board")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 5 Mo max par fichier
public class BoardServlet extends HttpServlet {

    // Détecte les @pseudo dans les commentaires
    private static final Pattern MENTION = Pattern.compile("@([\\p{L}0-9_-]+)");

    private TemplateEngine templateEngine;
    private TaskService taskService;
    private BoardService boardService;
    private NotificationRepository notificationRepository;
    private UserRepository userRepository;

    @Override
    public void init() {
        templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        taskService = new TaskService(new TaskRepository());
        boardService = new BoardService(new BoardRepository(), new UserRepository());
        notificationRepository = new NotificationRepository();
        userRepository = new UserRepository();
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
            // Panneau de notifications
            ctx.setVariable("notifications", notificationRepository.findByUser(user.getId(), 15));
            ctx.setVariable("unreadCount", notificationRepository.countUnread(user.getId()));
            // Liste des tableaux de l'utilisateur pour la sidebar
            ctx.setVariable("myBoards", boardService.getBoardsForUser(user.getId()));
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
                    String assigneeId = req.getParameter("assigneeId");
                    String title = req.getParameter("title");
                    String erreur = taskService.createTask(
                            boardId,
                            title,
                            req.getParameter("description"),
                            req.getParameter("typeId"),
                            assigneeId
                    );
                    if (erreur == null && assigneeId != null && !assigneeId.isEmpty()
                            && !assigneeId.equals(user.getId())) {
                        notify(assigneeId, boardId, null,
                                user.getPseudo() + " vous a assigné la tâche « " + title + " »");
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "updateTask" -> {
                    String taskId = req.getParameter("taskId");
                    String assigneeId = req.getParameter("assigneeId");
                    Task before = taskService.getTaskById(taskId);
                    String erreur = taskService.updateTask(
                            taskId,
                            req.getParameter("title"),
                            req.getParameter("description"),
                            req.getParameter("typeId"),
                            assigneeId
                    );
                    // Notifie le nouvel assigné si l'assignation a changé
                    if (erreur == null && before != null
                            && assigneeId != null && !assigneeId.isEmpty()
                            && !assigneeId.equals(before.getAssigneeId())
                            && !assigneeId.equals(user.getId())) {
                        notify(assigneeId, boardId, taskId,
                                user.getPseudo() + " vous a assigné la tâche « "
                                        + req.getParameter("title") + " »");
                    }
                    // Rouvre la modale de la tâche après modification
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId
                            + "&task=" + taskId);
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
                    String taskId = req.getParameter("taskId");
                    String content = req.getParameter("content");
                    String erreur = taskService.addComment(taskId, user.getId(), content);
                    if (erreur == null && content != null) {
                        notifyMentions(boardId, taskId, content, user);
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId
                            + "&task=" + taskId);
                }

                case "addAttachment" -> {
                    Part filePart = req.getPart("file");
                    if (filePart != null && filePart.getSize() > 0) {
                        try (var in = filePart.getInputStream()) {
                            new TaskRepository().saveAttachment(
                                    buildAttachment(req.getParameter("taskId"), filePart),
                                    in
                            );
                        }
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId
                            + "&task=" + req.getParameter("taskId"));
                }

                case "invite" -> {
                    String pseudo = req.getParameter("pseudo");
                    String erreur = boardService.inviteMember(boardId, pseudo, user.getId());
                    if (erreur == null) {
                        // Prévient l'utilisateur invité
                        Board board = boardService.getBoardById(boardId);
                        userRepository.findByPseudo(pseudo).ifPresent(invited ->
                                notify(invited.getId(), boardId, null,
                                        user.getPseudo() + " vous a ajouté au tableau « "
                                                + (board != null ? board.getName() : "") + " »"));
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "renameBoard" -> {
                    boardService.renameBoard(boardId, req.getParameter("name"), user.getId());
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                case "deleteBoard" -> {
                    String erreur = boardService.deleteBoard(boardId, user.getId());
                    if (erreur != null) {
                        resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/boards");
                    }
                }

                case "removeMember" -> {
                    String memberId = req.getParameter("memberId");
                    Board board = boardService.getBoardById(boardId);
                    String erreur = boardService.removeMember(boardId, memberId, user.getId());
                    if (erreur == null && board != null) {
                        notify(memberId, null, null,
                                user.getPseudo() + " vous a retiré du tableau « "
                                        + board.getName() + " »");
                    }
                    resp.sendRedirect(req.getContextPath() + "/board?id=" + boardId);
                }

                default -> resp.sendError(400, "Action inconnue");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500);
        }
    }

    // Crée une notification ; une erreur ici ne doit jamais faire échouer l'action principale
    private void notify(String userId, String boardId, String taskId, String text) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setBoardId(boardId);
            n.setTaskId(taskId);
            n.setText(text);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Notifie chaque membre du tableau mentionné (@pseudo) dans un commentaire
    private void notifyMentions(String boardId, String taskId, String content, User author) {
        try {
            Board board = boardService.getBoardById(boardId);
            if (board == null) return;
            Task task = taskService.getTaskById(taskId);
            String taskTitle = task != null ? task.getTitle() : "une tâche";

            Set<String> alreadyNotified = new HashSet<>();
            Matcher matcher = MENTION.matcher(content);
            while (matcher.find()) {
                String pseudo = matcher.group(1);
                board.getMembers().stream()
                        .filter(m -> m.getPseudo().equalsIgnoreCase(pseudo))
                        .findFirst()
                        .ifPresent(member -> {
                            if (!member.getId().equals(author.getId())
                                    && alreadyNotified.add(member.getId())) {
                                notify(member.getId(), boardId, taskId,
                                        author.getPseudo() + " vous a mentionné sur « "
                                                + taskTitle + " »");
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
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
