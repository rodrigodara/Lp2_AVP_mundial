package com.aluguer.controller;

import java.sql.SQLException;

import com.aluguer.dao.UserDAO;
import com.aluguer.service.PasswordRecoveryService;

public class RecuperarPasswordController {

    private final PasswordRecoveryService service = new PasswordRecoveryService(new UserDAO());

    public boolean enviarCodigo(String email) throws SQLException {
        return service.enviarCodigo(email);
    }

    public boolean verificarCodigo(String email, String codigo) throws SQLException {
        return service.verificarCodigo(email, codigo);
    }

    public boolean redefinirPassword(String email, String codigo, String novaPassword) throws SQLException {
        return service.redefinirPassword(email, codigo, novaPassword);
    }
}