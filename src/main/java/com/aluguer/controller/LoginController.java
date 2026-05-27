package com.aluguer.controller;

import com.aluguer.model.User;
import com.aluguer.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.aluguer.MainApp;

import java.sql.SQLException;

/**
 * Controller do formulário de login.
 * Liga login.fxml ao LoginService.
 */
public class LoginController {

    // ------------------------------------------------------------------
    // FXML bindings
    // ------------------------------------------------------------------

    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblErro;
    @FXML private Button        btnEntrar;

    // ------------------------------------------------------------------
    // Dependências
    // ------------------------------------------------------------------

    private final LoginService loginService = new LoginService();

    // ------------------------------------------------------------------
    // Ação do botão Entrar
    // ------------------------------------------------------------------

    @FXML
    private void onEntrar() {
        lblErro.setVisible(false);
        lblErro.setText("");

        try {
            User user = loginService.autenticar(
                    tfEmail.getText(),
                    pfPassword.getText()
            );

            // Login bem-sucedido
            // TODO: substituir pela navegação para o teu dashboard
            // Exemplo:  MainApp.showDashboard();
            // Por agora mostra uma confirmação
            mostrarSucesso("Bem-vindo, " + user.getNome() + "!");

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());

        } catch (SQLException e) {
            mostrarErro("Erro de ligação. Tente novamente mais tarde.");
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Ação do botão Criar Conta → navegar para registo
    // ------------------------------------------------------------------

    @FXML
    private void onCriarConta() {
        MainApp.showRegisto();
    }

    // ------------------------------------------------------------------
    // Logout estático — chamar em qualquer controller que precise de logout
    //
    // Exemplo de uso num dashboard:
    //   @FXML private void onLogout() { LoginController.logout(); }
    // ------------------------------------------------------------------

    public static void logout() {
        SessionManager.getInstance().terminarSessao();
        MainApp.showLogin();
    }

    // ------------------------------------------------------------------
    // Utilitários de UI
    // ------------------------------------------------------------------

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