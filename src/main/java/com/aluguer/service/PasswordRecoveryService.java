package com.aluguer.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Optional;

import com.aluguer.dao.CodigoRecuperacaoDAO;
import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.util.EmailService;
import com.aluguer.util.PasswordUtil;

/**
 * PasswordRecoveryService — recuperação de password por código enviado
 * por email (substitui o antigo mecanismo de pergunta de segurança).
 *
 * Fluxo: enviarCodigo(email) → verificarCodigo(email, codigo) →
 *        redefinirPassword(email, codigo, novaPassword)
 */
public class PasswordRecoveryService {

    private final UserDAO userDAO;
    private final CodigoRecuperacaoDAO codigoDAO = new CodigoRecuperacaoDAO();
    private static final SecureRandom RANDOM = new SecureRandom();

    public PasswordRecoveryService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Gera um código de 6 dígitos, invalida códigos anteriores ainda não
     * usados, grava o novo e envia por email. Devolve false se não existir
     * conta com esse email (o chamador decide a mensagem genérica a mostrar,
     * para não revelar se um email está ou não registado).
     */
    public boolean enviarCodigo(String email) throws SQLException {
        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        String codigo = gerarCodigo();

        codigoDAO.invalidarAnteriores(user.getId());
        codigoDAO.inserir(user.getId(), codigo);

        EmailService.enviarCodigoRecuperacao(user.getEmail(), user.getNome(), codigo);
        return true;
    }

    /** Verifica se o código introduzido é válido para este email. */
    public boolean verificarCodigo(String email, String codigo) throws SQLException {
        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        return codigoDAO.isValido(userOpt.get().getId(), codigo.trim());
    }

    /**
     * Redefine a password, desde que o código ainda seja válido. Marca o
     * código como usado, para não poder ser reutilizado.
     */
    public boolean redefinirPassword(String email, String codigo, String novaPassword) throws SQLException {
        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        if (!codigoDAO.isValido(user.getId(), codigo.trim())) {
            return false;
        }

        String novoHash = PasswordUtil.hashPassword(novaPassword);
        boolean ok = userDAO.updatePassword(email, novoHash);
        if (ok) {
            codigoDAO.marcarComoUsado(user.getId(), codigo.trim());
        }
        return ok;
    }

    private String gerarCodigo() {
        int numero = 100000 + RANDOM.nextInt(900000); // 6 dígitos, 100000-999999
        return String.valueOf(numero);
    }
}