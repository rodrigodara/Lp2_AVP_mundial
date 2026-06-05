package com.aluguer.controller;

import java.sql.SQLException;
import java.time.LocalDate;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.util.PasswordUtil;

/**
 * ALV-27 — Lógica de negócio para o registo de utilizadores.
 *
 * Valida os dados de entrada, faz hash da password e delega a persistência ao UserDAO.
 * Usado pelo RegistoController (JavaFX) para ligar a UI ao backend.
 */
public class RegistoService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Regista um novo utilizador.
     *
     * @param email          email único
     * @param nome           nome completo
     * @param nif            NIF (9 dígitos)
     * @param numeroCarta    nº carta de condução
     * @param validadeCarta  data de validade da carta
     * @param password       password em texto simples (será convertida em hash)
     * @return User recém-criado com id preenchido
     * @throws IllegalArgumentException se algum campo for inválido ou email duplicado
     * @throws SQLException             em caso de erro de base de dados
     */
    public User registar(String email, String nome, String nif,
                         String numeroCarta, LocalDate validadeCarta,
                         String password) throws SQLException {

        // --- Validações ---
        validarEmail(email);
        validarNome(nome);
        validarNif(nif);
        validarNumeroCarta(numeroCarta);
        validarValidadeCarta(validadeCarta);
        validarPassword(password);

        // ALV-26: hash da password antes de persistir
        String hash = PasswordUtil.hashPassword(password);

        // Montar o objecto User
        User user = new User(email.trim().toLowerCase(), nome.trim(),
                nif.trim(), numeroCarta.trim(), validadeCarta, hash);

        // ALV-27 + ALV-28: persistir (o DAO valida o email único)
        userDAO.registar(user);

        return user;
    }

    // -----------------------------------------------------------------
    // Validações privadas
    // -----------------------------------------------------------------

    private void validarEmail(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("O email é obrigatório.");
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("O email não tem formato válido.");
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank())
            throw new IllegalArgumentException("O nome é obrigatório.");
        if (nome.trim().length() < 3)
            throw new IllegalArgumentException("O nome deve ter pelo menos 3 caracteres.");
    }

    private void validarNif(String nif) {
        if (nif == null || !nif.matches("\\d{9}"))
            throw new IllegalArgumentException("O NIF deve ter exactamente 9 dígitos.");
    }

    private void validarNumeroCarta(String numeroCarta) {
        if (numeroCarta == null || numeroCarta.isBlank())
            throw new IllegalArgumentException("O número de carta de condução é obrigatório.");
    }

    private void validarValidadeCarta(LocalDate validade) {
        if (validade == null)
            throw new IllegalArgumentException("A validade da carta é obrigatória.");
        if (validade.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("A carta de condução está expirada.");
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("A password deve ter pelo menos 8 caracteres.");
    }
}
