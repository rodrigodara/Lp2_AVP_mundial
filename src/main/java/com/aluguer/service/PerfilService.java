package com.aluguer.service;

import java.sql.SQLException;
import java.time.LocalDate;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.SessionManager;

/**
 * Serviço para edição dos dados de perfil do utilizador autenticado
 * (nome, email, NIF, carta de condução e foto de perfil).
 */
public class PerfilService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Atualiza o perfil do utilizador autenticado.
     * A sessão local (SessionManager) só é atualizada depois de a
     * gravação na base de dados ser confirmada com sucesso.
     */
    public ResultadoOperacao atualizarPerfil(String nome, String email, String nif,
                                              String numeroCarta, LocalDate validadeCarta,
                                              byte[] foto) {
        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            return ResultadoOperacao.erro("Não existe sessão ativa.");
        }

        if (nome == null || nome.isBlank()) {
            return ResultadoOperacao.erro("O nome não pode estar vazio.");
        }
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResultadoOperacao.erro("Indique um email válido.");
        }

        String nomeFinal = nome.trim();
        String emailFinal = email.trim();
        String nifFinal = (nif != null && !nif.isBlank()) ? nif.trim() : null;
        String cartaFinal = (numeroCarta != null && !numeroCarta.isBlank()) ? numeroCarta.trim() : null;

        try {
            boolean ok = userDAO.atualizarPerfil(
                user.getId(), nomeFinal, emailFinal, nifFinal, cartaFinal,
                validadeCarta != null ? java.sql.Date.valueOf(validadeCarta) : null,
                foto
            );

            if (!ok) {
                return ResultadoOperacao.erro("Não foi possível atualizar o perfil.");
            }

            // Só atualizamos a sessão depois de confirmado na base de dados
            user.setNome(nomeFinal);
            user.setEmail(emailFinal);
            user.setNif(nifFinal);
            user.setNumeroCarta(cartaFinal);
            user.setValidadeCarta(validadeCarta);
            user.setFoto(foto);

            return ResultadoOperacao.sucesso("Perfil atualizado com sucesso.");

        } catch (SQLException e) {
            String mensagem = (e.getMessage() != null && e.getMessage().contains("Duplicate"))
                ? "Esse email já está a ser usado por outra conta."
                : "Erro ao atualizar o perfil. Tente novamente.";
            System.err.println("[PerfilService] " + e.getMessage());
            return ResultadoOperacao.erro(mensagem);
        }
    }
}