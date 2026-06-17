package pt.plataformaaluguerveiculos.views;

import java.sql.SQLException;

import com.aluguer.dao.UserDAO;
import com.aluguer.service.PasswordRecoveryService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
public class RecuperarPasswordView {

    private VBox root;

    public RecuperarPasswordView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setMaxWidth(500);
        root.setStyle("-fx-background-color: white;");

        Text titulo = new Text("Recuperar Password");
        titulo.getStyleClass().add("login-titulo");

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.getStyleClass().add("campo-texto");
        campoEmail.setMaxWidth(320);

        Label lblPergunta = new Label();
        lblPergunta.setMaxWidth(320);
        lblPergunta.setWrapText(true);

        PasswordField campoResposta = new PasswordField();
        campoResposta.setPromptText("Resposta de segurança");
        campoResposta.getStyleClass().add("campo-texto");
        campoResposta.setMaxWidth(320);
        campoResposta.setVisible(false);

        PasswordField campoNovaPw = new PasswordField();
        campoNovaPw.setPromptText("Nova password");
        campoNovaPw.getStyleClass().add("campo-texto");
        campoNovaPw.setMaxWidth(320);
        campoNovaPw.setVisible(false);

        PasswordField campoConfirmar = new PasswordField();
        campoConfirmar.setPromptText("Confirmar password");
        campoConfirmar.getStyleClass().add("campo-texto");
        campoConfirmar.setMaxWidth(320);
        campoConfirmar.setVisible(false);

        Button btnVerificar = new Button("Verificar Email");
        btnVerificar.getStyleClass().add("btn-primario");
        btnVerificar.setMaxWidth(320);

        Button btnRedefinir = new Button("Redefinir Password");
        btnRedefinir.getStyleClass().add("btn-primario");
        btnRedefinir.setMaxWidth(320);
        btnRedefinir.setVisible(false);

        Label lblMensagem = new Label();
        lblMensagem.getStyleClass().add("mensagem-erro");
        lblMensagem.setVisible(false);

        PasswordRecoveryService service = new PasswordRecoveryService(new UserDAO());

        btnVerificar.setOnAction(e -> {
            String email = campoEmail.getText().trim();
            try {
                String pergunta = service.getSecurityQuestion(email);
                if (pergunta == null) {
                    lblMensagem.setText("Email não encontrado.");
                    lblMensagem.setVisible(true);
                    return;
                }
                lblPergunta.setText("Pergunta: " + pergunta);
                campoResposta.setVisible(true);
                campoNovaPw.setVisible(true);
                campoConfirmar.setVisible(true);
                btnRedefinir.setVisible(true);
                lblMensagem.setVisible(false);
            } catch (SQLException ex) {
                ex.printStackTrace();
                lblMensagem.setText("Erro de ligação à base de dados.");
                lblMensagem.setVisible(true);
            }
        });

        btnRedefinir.setOnAction(e -> {
            String novaPw = campoNovaPw.getText();
            if (!novaPw.equals(campoConfirmar.getText())) {
                lblMensagem.setText("As passwords não coincidem.");
                lblMensagem.setVisible(true);
                return;
            }
            if (novaPw.length() < 6) {
                lblMensagem.setText("Mínimo 6 caracteres.");
                lblMensagem.setVisible(true);
                return;
            }
            try {
                boolean ok = service.resetPassword(
                    campoEmail.getText().trim(),
                    campoResposta.getText(),
                    novaPw
                );
                if (ok) {
                    lblMensagem.setStyle("-fx-text-fill: green;");
                    lblMensagem.setText("Password alterada! Podes fazer login.");
                } else {
                    lblMensagem.setText("Resposta incorreta.");
                }
                lblMensagem.setVisible(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
                lblMensagem.setText("Erro ao redefinir password.");
                lblMensagem.setVisible(true);
            }
        });

        Button btnVoltar = new Button("Voltar ao Login");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setMaxWidth(320);
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaLogin());

        root.getChildren().addAll(
            titulo, campoEmail, btnVerificar,
            lblPergunta, campoResposta, campoNovaPw, campoConfirmar,
            btnRedefinir, lblMensagem, btnVoltar
        );
    }

    public VBox getRoot() { return root; }
}