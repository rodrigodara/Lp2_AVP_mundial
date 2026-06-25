package pt.plataformaaluguerveiculos.views;

import com.aluguer.controller.RecuperarPasswordController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Recuperação de password em 3 passos:
 *   1. Pede o email e envia um código de 6 dígitos por email.
 *   2. Pede o código recebido.
 *   3. Pede a nova password (e confirmação) e aplica.
 */
public class RecuperarPasswordView {

    private final VBox root;
    private final RecuperarPasswordController controller = new RecuperarPasswordController();

    // Guarda o estado entre passos
    private String emailAtual;
    private String codigoAtual;

    public RecuperarPasswordView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.getStyleClass().add("login-container");

        mostrarPassoEmail();
    }

    // ================================================================
    // Passo 1 — pedir o email
    // ================================================================

    private void mostrarPassoEmail() {
        root.getChildren().clear();

        Text titulo = new Text("Recuperar Password");
        titulo.getStyleClass().add("login-titulo");

        Label lblInfo = new Label("Indica o teu email. Vamos enviar-te um código de 6 dígitos.");
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(320);
        lblInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.setMaxWidth(320);
        campoEmail.getStyleClass().add("campo-texto");

        Label lblErro = new Label();
        lblErro.getStyleClass().add("mensagem-erro");
        lblErro.setWrapText(true);
        lblErro.setMaxWidth(320);
        lblErro.setVisible(false);

        Button btnEnviar = new Button("Enviar Código");
        btnEnviar.setMaxWidth(320);
        btnEnviar.getStyleClass().add("btn-primario");
        btnEnviar.setOnAction(e -> {
            String email = campoEmail.getText().trim();
            if (email.isEmpty()) {
                lblErro.setText("Por favor indica o teu email.");
                lblErro.setVisible(true);
                return;
            }

            btnEnviar.setDisable(true);
            try {
                controller.enviarCodigo(email);
                // Mensagem sempre igual, exista ou não a conta — não revela
                // se um email está registado na plataforma.
                emailAtual = email;
                mostrarPassoCodigo();
            } catch (Exception ex) {
                ex.printStackTrace();
                lblErro.setText("Erro ao enviar o código. Tenta novamente.");
                lblErro.setVisible(true);
                btnEnviar.setDisable(false);
            }
        });

        Button btnVoltar = new Button("Voltar ao Login");
        btnVoltar.setMaxWidth(320);
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaLogin());

        root.getChildren().addAll(titulo, lblInfo, campoEmail, lblErro, btnEnviar, btnVoltar);
    }

    // ================================================================
    // Passo 2 — pedir o código recebido por email
    // ================================================================

    private void mostrarPassoCodigo() {
        root.getChildren().clear();

        Text titulo = new Text("Verificar Código");
        titulo.getStyleClass().add("login-titulo");

        Label lblInfo = new Label(
            "Enviámos um código de 6 dígitos para " + emailAtual + ". " +
            "Verifica a tua caixa de entrada (e a pasta de spam) — o código expira em 15 minutos."
        );
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(320);
        lblInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        TextField campoCodigo = new TextField();
        campoCodigo.setPromptText("Código de 6 dígitos");
        campoCodigo.setMaxWidth(320);
        campoCodigo.getStyleClass().add("campo-texto");

        Label lblErro = new Label();
        lblErro.getStyleClass().add("mensagem-erro");
        lblErro.setWrapText(true);
        lblErro.setMaxWidth(320);
        lblErro.setVisible(false);

        Button btnVerificar = new Button("Verificar Código");
        btnVerificar.setMaxWidth(320);
        btnVerificar.getStyleClass().add("btn-primario");
        btnVerificar.setOnAction(e -> {
            String codigo = campoCodigo.getText().trim();
            if (codigo.isEmpty()) {
                lblErro.setText("Por favor indica o código recebido.");
                lblErro.setVisible(true);
                return;
            }

            try {
                boolean valido = controller.verificarCodigo(emailAtual, codigo);
                if (valido) {
                    codigoAtual = codigo;
                    mostrarPassoNovaPassword();
                } else {
                    lblErro.setText("Código inválido ou expirado. Verifica e tenta de novo.");
                    lblErro.setVisible(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                lblErro.setText("Erro ao verificar o código. Tenta novamente.");
                lblErro.setVisible(true);
            }
        });

        Button btnReenviar = new Button("Reenviar código");
        btnReenviar.setMaxWidth(320);
        btnReenviar.getStyleClass().add("btn-secundario");
        btnReenviar.setOnAction(e -> {
            try {
                controller.enviarCodigo(emailAtual);
                lblErro.setStyle("-fx-text-fill: #2e7d32;");
                lblErro.setText("Novo código enviado para " + emailAtual + ".");
                lblErro.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setMaxWidth(320);
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> mostrarPassoEmail());

        root.getChildren().addAll(titulo, lblInfo, campoCodigo, lblErro, btnVerificar, btnReenviar, btnVoltar);
    }

    // ================================================================
    // Passo 3 — definir a nova password
    // ================================================================

    private void mostrarPassoNovaPassword() {
        root.getChildren().clear();

        Text titulo = new Text("Nova Password");
        titulo.getStyleClass().add("login-titulo");

        Label lblInfo = new Label("Código confirmado! Define a tua nova password.");
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(320);
        lblInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        PasswordField campoNovaPassword = new PasswordField();
        campoNovaPassword.setPromptText("Nova password");
        campoNovaPassword.setMaxWidth(320);
        campoNovaPassword.getStyleClass().add("campo-texto");

        PasswordField campoConfirmar = new PasswordField();
        campoConfirmar.setPromptText("Confirmar nova password");
        campoConfirmar.setMaxWidth(320);
        campoConfirmar.getStyleClass().add("campo-texto");

        Label lblErro = new Label();
        lblErro.getStyleClass().add("mensagem-erro");
        lblErro.setWrapText(true);
        lblErro.setMaxWidth(320);
        lblErro.setVisible(false);

        Button btnConfirmar = new Button("Repor Password");
        btnConfirmar.setMaxWidth(320);
        btnConfirmar.getStyleClass().add("btn-primario");
        btnConfirmar.setOnAction(e -> {
            String novaPassword = campoNovaPassword.getText();
            String confirmar = campoConfirmar.getText();

            if (novaPassword == null || novaPassword.isBlank()) {
                lblErro.setText("Por favor indica a nova password.");
                lblErro.setVisible(true);
                return;
            }
            if (novaPassword.length() < 6) {
                lblErro.setText("A password deve ter pelo menos 6 caracteres.");
                lblErro.setVisible(true);
                return;
            }
            if (!novaPassword.equals(confirmar)) {
                lblErro.setText("As passwords não coincidem.");
                lblErro.setVisible(true);
                return;
            }

            try {
                boolean ok = controller.redefinirPassword(emailAtual, codigoAtual, novaPassword);
                if (ok) {
                    NavigationManager.getInstance().navegarParaLogin();
                } else {
                    lblErro.setText("Não foi possível repor a password. O código pode ter expirado — tenta novamente.");
                    lblErro.setVisible(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                lblErro.setText("Erro ao repor a password. Tenta novamente.");
                lblErro.setVisible(true);
            }
        });

        root.getChildren().addAll(titulo, lblInfo, campoNovaPassword, campoConfirmar, lblErro, btnConfirmar);
    }

    public VBox getRoot() {
        return root;
    }
}