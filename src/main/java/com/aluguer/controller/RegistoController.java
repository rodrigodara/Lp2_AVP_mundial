package com.aluguer.controller;

import com.aluguer.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;

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

    @FXML
    private void onVoltar() {
        pt.plataformaaluguerveiculos.views.NavigationManager
                .getInstance()
                .navegarParaLogin();
    }
}