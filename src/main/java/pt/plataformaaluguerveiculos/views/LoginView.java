package pt.plataformaaluguerveiculos.views;

import java.sql.SQLException;
import java.util.Optional;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.User;
import com.aluguer.util.PasswordUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class LoginView {

    private VBox root;

    public LoginView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setMaxWidth(500);
        root.setStyle("-fx-background-color: white;");
        root.getStyleClass().add("login-container");

        Text titulo = new Text("Plataforma de Aluguer de Veículos");
        titulo.getStyleClass().add("login-titulo");

        Text subtitulo = new Text("Inicie sessão para continuar");
        subtitulo.getStyleClass().add("login-subtitulo");

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.getStyleClass().add("campo-texto");
        campoEmail.setMaxWidth(320);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Password");
        campoPassword.getStyleClass().add("campo-texto");
        campoPassword.setMaxWidth(320);

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().add("btn-primario");
        btnEntrar.setMaxWidth(320);

        Label lblErro = new Label();
        lblErro.getStyleClass().add("mensagem-erro");
        lblErro.setVisible(false);

        btnEntrar.setOnAction(e -> {
            String email    = campoEmail.getText().trim();
            String password = campoPassword.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                lblErro.setText("Por favor preencha todos os campos.");
                lblErro.setVisible(true);
                return;
            }

            try {
                UserDAO userDAO = new UserDAO();
                Optional<User> userOpt = userDAO.findByEmail(email);

                System.out.println("[LOGIN] Email: '" + email + "'");
                System.out.println("[LOGIN] Utilizador encontrado: " + userOpt.isPresent());
                if (userOpt.isPresent()) {
                    String hash = userOpt.get().getPasswordHash();
                    System.out.println("[LOGIN] Hash: '" + hash + "'");
                    System.out.println("[LOGIN] Hash length: " + (hash != null ? hash.length() : "null"));
                    System.out.println("[LOGIN] Password ok: " + PasswordUtil.verifyPassword(password, hash));
                }

                if (userOpt.isEmpty() || !PasswordUtil.verifyPassword(password, userOpt.get().getPasswordHash())) {
                    lblErro.setText("Email ou password incorretos.");
                    lblErro.setVisible(true);
                    return;
                }

                User user = userOpt.get();

                if (!user.isAtivo()) {
                    lblErro.setText("A sua conta está desativada. Contacte o administrador.");
                    lblErro.setVisible(true);
                    return;
                }

                lblErro.setVisible(false);

                com.aluguer.util.SessionManager.getInstance().iniciarSessao(user);

                NavigationManager nav = NavigationManager.getInstance();
                nav.setUtilizadorLogado(user.getId());

                BaseLayoutView layout = nav.getBaseLayout();
                if (layout != null) {
                    layout.getRoot().setTop(layout.getNavbarView().getNavbar());
                }
                nav.navegarParaDashboard();

            } catch (SQLException ex) {
                ex.printStackTrace();
                lblErro.setText("Erro de ligação à base de dados.");
                lblErro.setVisible(true);
            }
        });

        campoPassword.setOnAction(e -> btnEntrar.fire());

        Button btnRegisto = new Button("Não tem conta? Registar-se");
        btnRegisto.getStyleClass().add("btn-secundario");
        btnRegisto.setMaxWidth(320);
        btnRegisto.setOnAction(e -> NavigationManager.getInstance().navegarParaRegisto());

        Button btnRecuperar = new Button("Esqueceste a password?");
        btnRecuperar.getStyleClass().add("btn-secundario");
        btnRecuperar.setMaxWidth(320);
        btnRecuperar.setOnAction(e -> NavigationManager.getInstance().navegarParaRecuperarPassword());

        root.getChildren().addAll(titulo, subtitulo, campoEmail, campoPassword, lblErro, btnEntrar, btnRecuperar, btnRegisto);
    }

    public VBox getRoot() {
        return root;
    }
}