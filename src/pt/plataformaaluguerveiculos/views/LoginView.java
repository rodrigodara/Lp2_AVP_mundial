package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * ALV-59 - Ligar login para entrar no dashboard
 * Página de login que autentica o utilizador e redireciona para o Dashboard.
 *
 * Nota: A validação real de credenciais será integrada com o backend (ALV-33/ALV-38).
 * Por agora, valida campos não vazios e navega para o Dashboard.
 */
public class LoginView {

    private VBox root;

    public LoginView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
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

        // ALV-59: ao clicar Entrar, valida e navega para o Dashboard
        btnEntrar.setOnAction(e -> {
            String email = campoEmail.getText().trim();
            String password = campoPassword.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                lblErro.setText("Por favor preencha todos os campos.");
                lblErro.setVisible(true);
                return;
            }

            // TODO: integrar com UserService (ALV-33) para validação real
            // Por agora aceita qualquer credencial não vazia e entra no dashboard
            lblErro.setVisible(false);

            // Restaura navbar e navega para o dashboard
            NavigationManager nav = NavigationManager.getInstance();
            BaseLayoutView layout = nav.getBaseLayout();
            if (layout != null) {
                NavbarView navbar = layout.getNavbarView();
                layout.getRoot().setTop(navbar.getNavbar());
            }
            nav.navegarParaDashboard();
        });

        // Também entra ao pressionar Enter no campo password
        campoPassword.setOnAction(e -> btnEntrar.fire());

        root.getChildren().addAll(titulo, subtitulo, campoEmail, campoPassword, lblErro, btnEntrar);
    }

    public VBox getRoot() {
        return root;
    }
}
