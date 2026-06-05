package com.aluguer.controller;

import java.sql.SQLException;
import java.time.LocalDate;

<<<<<<< HEAD
import com.aluguer.MainApp;
import com.aluguer.model.User;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * ALV-29 / ALV-30 / ALV-31 — Controller JavaFX do formulário de registo.
 *
 * Liga o registo.fxml ao RegistoService (backend).
 * Trata erros de validação e de base de dados e mostra-os ao utilizador.
 */
=======
>>>>>>> a9cfcea30995633c429d48f491bf4e9ad2a1ec6a
public class RegistoController {

    @FXML private TextField tfEmail;
    @FXML private TextField tfNome;
    @FXML private TextField tfNif;
    @FXML private TextField tfNumeroCarta;
    @FXML private DatePicker dpValidadeCarta;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private Label lblErro;
    @FXML private Button btnRegistar;
    @FXML private VBox vboxForm;

    private final RegistoService registoService = new RegistoService();

<<<<<<< HEAD
    // ------------------------------------------------------------------
    // Acção do botão Registar
    // ------------------------------------------------------------------
    @FXML
    private void onVoltar() {
        MainApp.showLogin();
    }
=======
>>>>>>> a9cfcea30995633c429d48f491bf4e9ad2a1ec6a
    @FXML
    private void onRegistar() {
        lblErro.setVisible(false);
        lblErro.setText("");

        if (!pfPassword.getText().equals(pfConfirmarPassword.getText())) {
            mostrarErro("As passwords não coincidem.");
            return;
        }

        LocalDate validade = dpValidadeCarta.getValue();

        try {
            User user = registoService.registar(
                    tfEmail.getText(),
                    tfNome.getText(),
                    tfNif.getText(),
                    tfNumeroCarta.getText(),
                    validade,
                    pfPassword.getText()
            );

            mostrarSucesso("Conta criada com sucesso! Bem-vindo, " + user.getNome() + ".");
            limparFormulario();

        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());

        } catch (SQLException e) {
            mostrarErro("Erro ao registar. Tente novamente mais tarde.");
            e.printStackTrace();
        }
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

    private void limparFormulario() {
        tfEmail.clear();
        tfNome.clear();
        tfNif.clear();
        tfNumeroCarta.clear();
        dpValidadeCarta.setValue(null);
        pfPassword.clear();
        pfConfirmarPassword.clear();
    }

    // ------------------------------------------------------------------
    // Navegação — voltar ao login (ligar ao MainApp se necessário)
    // ------------------------------------------------------------------


}