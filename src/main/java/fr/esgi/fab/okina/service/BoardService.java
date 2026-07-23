package fr.esgi.fab.okina.service;

import fr.esgi.fab.okina.models.Board;
import fr.esgi.fab.okina.repository.BoardRepository;
import fr.esgi.fab.okina.repository.UserRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public BoardService(BoardRepository boardRepository, UserRepository userRepository) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
    }

    public List<Board> getBoardsForUser(String userId) throws SQLException {
        return boardRepository.findByUserId(userId);
    }

    public Board getBoardById(String boardId) throws SQLException {
        return boardRepository.findById(boardId);
    }

    public String createBoard(String name, String ownerId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return "Le nom du tableau est obligatoire.";
        }
        Board board = new Board();
        board.setName(name.trim());
        board.setOwnerId(ownerId);
        board.setCreatedAt(LocalDateTime.now());
        boardRepository.save(board);
        return null;
    }

    public String renameBoard(String boardId, String name, String requesterId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return "Le nom du tableau est obligatoire.";
        }
        String erreur = checkOwner(boardId, requesterId);
        if (erreur != null) return erreur;
        boardRepository.rename(boardId, name.trim());
        return null;
    }

    public String deleteBoard(String boardId, String requesterId) throws SQLException {
        String erreur = checkOwner(boardId, requesterId);
        if (erreur != null) return erreur;
        boardRepository.delete(boardId);
        return null;
    }

    public String removeMember(String boardId, String memberId, String requesterId) throws SQLException {
        Board board = boardRepository.findById(boardId);
        if (board == null) return "Tableau introuvable.";
        if (!board.getOwnerId().equals(requesterId)) {
            return "Seul le propriétaire peut retirer un membre.";
        }
        if (board.getOwnerId().equals(memberId)) {
            return "Le propriétaire ne peut pas être retiré du tableau.";
        }
        boardRepository.removeMember(boardId, memberId);
        return null;
    }

    // Vérifie que le demandeur est bien le propriétaire du tableau
    private String checkOwner(String boardId, String requesterId) throws SQLException {
        Board board = boardRepository.findById(boardId);
        if (board == null) return "Tableau introuvable.";
        if (!board.getOwnerId().equals(requesterId)) {
            return "Seul le propriétaire peut effectuer cette action.";
        }
        return null;
    }

    // Invite un utilisateur par son pseudo
    public String inviteMember(String boardId, String pseudo, String requesterId) throws SQLException {
        var userOpt = userRepository.findByPseudo(pseudo);
        if (userOpt.isEmpty()) {
            return "Utilisateur introuvable.";
        }
        var user = userOpt.get();
        if (boardRepository.isMember(boardId, user.getId())) {
            return "Cet utilisateur est déjà membre du tableau.";
        }
        boardRepository.addMember(boardId, user.getId());
        return null;
    }
}
