package com.aluguer.controller;

import com.aluguer.model.User;
import com.aluguer.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import pt.plataformaaluguerveiculos.views.NavigationManager;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblErro;
    @FXML private Button        btnEntrar;

    private final LoginService loginService = new LoginService();

    @FXML
    private void onEntrar() {
        lblErro.setVisible(false);
        lblErro.setText("");

        try {
            User user = loginService.autenticar(
                    tfEmail.getText(),
                    pfPassword.getText()
            );
            mostrarSucesso("Bem-vindo, " + user.getNome() + "!");

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());

        } catch (SQLException e) {
            mostrarErro("Erro de ligação. Tente novamente mais tarde.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onCriarConta() {
        NavigationManager.getInstance().navegarParaRegisto();
    }

    public static void logout() {
        SessionManager.getInstance().terminarSessao();
        NavigationManager.getInstance().navegarParaLogin();
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText("⚠ " + mensagem);
        lblErro.setStyle("-fx-text-fill: #e53935;");
        lblErro.setVisible(true);
    }

    private void mostrarSucesso(String mensagem) {
        lblErro.setText("✓ " + mensagem);
        lblErro.setStyle("-fx-text-fill: #43a047;");
        lblErro.setVisible(true);
    }
}