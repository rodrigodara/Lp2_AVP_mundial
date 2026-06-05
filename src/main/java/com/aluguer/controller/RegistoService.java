package com.aluguer.controller;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDate;

public class RegistoService {

    private final UserDAO userDAO = new UserDAO();

    public User registar(String email,
                         String nome,
                         String nif,
                         String numeroCarta,
                         LocalDate validadeCarta,
                         String password) throws SQLException {

        validarDados(email, nome, nif, numeroCarta, validadeCarta, password);

        if (userDAO.emailExiste(email)) {
            throw new IllegalArgumentException("Já existe uma conta com este email.");
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        User user = new User(
                email.trim(),
                nome.trim(),
                nif.trim(),
                numeroCarta.trim(),
                validadeCarta,
                passwordHash
        );

        userDAO.registar(user);

        return user;
    }

    private void validarDados(String email,
                              String nome,
                              String nif,
                              String numeroCarta,
                              LocalDate validadeCarta,
                              String password) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("O email é obrigatório.");
        }

        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("O email introduzido não é válido.");
        }

        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }

        if (nif == null || !nif.matches("\\d{9}")) {
            throw new IllegalArgumentException("O NIF deve ter 9 dígitos.");
        }

        if (numeroCarta == null || numeroCarta.trim().isEmpty()) {
            throw new IllegalArgumentException("O número da carta de condução é obrigatório.");
        }

        if (validadeCarta == null) {
            throw new IllegalArgumentException("A validade da carta de condução é obrigatória.");
        }

        if (validadeCarta.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A carta de condução não pode estar expirada.");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("A password deve ter pelo menos 6 caracteres.");
        }
    }
}