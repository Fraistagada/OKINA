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
