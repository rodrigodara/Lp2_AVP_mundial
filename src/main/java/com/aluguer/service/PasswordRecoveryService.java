package com.aluguer.service;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.aluguer.dao.UserDAO;
public class PasswordRecoveryService {

    private final UserDAO userDAO;

    public PasswordRecoveryService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public String getSecurityQuestion(String email) throws SQLException {
        return userDAO.getSecurityQuestion(email);
    }

    public boolean resetPassword(String email, String answer, String newPassword) throws SQLException {
        String storedHash = userDAO.getSecurityAnswerHash(email);
        if (storedHash == null || !BCrypt.checkpw(answer.toLowerCase().trim(), storedHash)) {
            return false;
        }
        String newHashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        return userDAO.updatePassword(email, newHashed);
    }
}