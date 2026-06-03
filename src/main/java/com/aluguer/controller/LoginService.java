package com.aluguer.controller;

import java.sql.SQLException;
import java.util.Optional;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.util.PasswordUtil;
import com.aluguer.util.SessionManager;

/**
 * Serviço de autenticação — separa a lógica do controller JavaFX.
 */
public class LoginService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Autentica o utilizador com email e password.
     *
     * @param email    email introduzido
     * @param password password em texto simples
     * @return User autenticado
     * @throws IllegalArgumentException se os campos estiverem vazios,
     *                                  ou se as credenciais forem inválidas
     * @throws SQLException             em caso de erro de base de dados
     */
    public User autenticar(String email, String password) throws SQLException {

        // 1. Validações básicas
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Introduza o seu email.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Introduza a sua password.");
        }

        // 2. Procurar utilizador por email
        Optional<User> opt = userDAO.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Email ou password incorretos.");
        }

        User user = opt.get();

        // 3. Verificar conta ativa
        if (!user.isAtivo()) {
            throw new IllegalArgumentException("Conta suspensa. Contacte o suporte.");
        }

        // 4. Verificar password com BCrypt
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou password incorretos.");
        }

        // 5. Guardar sessão
        SessionManager.getInstance().iniciarSessao(user);

        return user;
    }
}