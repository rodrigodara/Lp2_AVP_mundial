package com.aluguer.controller;

import java.sql.SQLException;

import com.aluguer.dao.UserDAO;
import com.aluguer.service.PasswordRecoveryService;

public class RecuperarPasswordController {

    private final PasswordRecoveryService service = new PasswordRecoveryService(new UserDAO());

    public String getSecurityQuestion(String email) throws SQLException {
        return service.getSecurityQuestion(email);
    }

    public boolean resetPassword(String email, String answer, String newPassword) throws SQLException {
        return service.resetPassword(email, answer, newPassword);
    }
}