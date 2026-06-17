package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class RegistoView {

    private VBox root;

    public RegistoView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));

        Text titulo = new Text("Criar Conta");
        titulo.getStyleClass().add("login-titulo");

        TextField campoNome = new TextField();
        campoNome.setPromptText("Nome completo");
        campoNome.setMaxWidth(320);

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.setMaxWidth(320);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Password");
        campoPassword.setMaxWidth(320);

        TextField campoNif = new TextField();
        campoNif.setPromptText("NIF");
        campoNif.setMaxWidth(320);

        TextField campoNumeroCarta = new TextField();
        campoNumeroCarta.setPromptText("Número da carta de condução");
        campoNumeroCarta.setMaxWidth(320);

        DatePicker campoValidadeCarta = new DatePicker();
        campoValidadeCarta.setPromptText("Validade da carta");
        campoValidadeCarta.setMaxWidth(320);

        // --- Pergunta de segurança ---
        ComboBox<String> comboPergunta = new ComboBox<>();
        comboPergunta.getItems().addAll(
            "Qual é o nome do teu animal de estimação?",
            "Qual é o nome da tua cidade natal?",
            "Qual é o nome do teu melhor amigo de infância?",
            "Qual é a tua comida favorita?"
        );
        comboPergunta.setPromptText("Escolhe uma pergunta de segurança");
        comboPergunta.setMaxWidth(320);

        PasswordField campoRespostaSeg = new PasswordField();
        campoRespostaSeg.setPromptText("Resposta de segurança");
        campoRespostaSeg.setMaxWidth(320);
        // ----------------------------

        Label lblErro = new Label();
        lblErro.setStyle("-fx-text-fill: #e53935;");
        lblErro.setVisible(false);

        Button btnRegistar = new Button("Registar");
        btnRegistar.setMaxWidth(320);
        btnRegistar.setOnAction(e -> {
            String nome = campoNome.getText().trim();
            String email = campoEmail.getText().trim();
            String password = campoPassword.getText().trim();
            String nif = campoNif.getText().trim();
            String carta = campoNumeroCarta.getText().trim();
            String pergunta = comboPergunta.getValue();
            String resposta = campoRespostaSeg.getText().trim();

            if (nome.isEmpty() || email.isEmpty() || password.isEmpty()
                    || nif.isEmpty() || carta.isEmpty()
                    || campoValidadeCarta.getValue() == null) {
                lblErro.setText("Por favor preencha todos os campos.");
                lblErro.setVisible(true);
                return;
            }

            if (pergunta == null || resposta.isEmpty()) {
                lblErro.setText("Por favor define uma pergunta e resposta de segurança.");
                lblErro.setVisible(true);
                return;
            }

            try {
                com.aluguer.dao.UserDAO dao = new com.aluguer.dao.UserDAO();
                com.aluguer.model.User novoUser = new com.aluguer.model.User();
                novoUser.setNome(nome);
                novoUser.setEmail(email);
                novoUser.setPasswordHash(com.aluguer.util.PasswordUtil.hashPassword(password));
                novoUser.setNif(nif);
                novoUser.setNumeroCarta(carta);
                novoUser.setValidadeCarta(campoValidadeCarta.getValue());
                novoUser.setPerfil("UTILIZADOR");
                novoUser.setAtivo(true);
                novoUser.setSecurityQuestion(pergunta);
                novoUser.setSecurityAnswer(com.aluguer.util.PasswordUtil.hashPassword(resposta.toLowerCase()));

                dao.registar(novoUser);

                NavigationManager.getInstance().navegarParaLogin();

            } catch (Exception ex) {
                ex.printStackTrace();
                lblErro.setText("Erro ao registar. Tente novamente.");
                lblErro.setVisible(true);
            }
        });

        Button btnVoltar = new Button("Já tenho conta");
        btnVoltar.setMaxWidth(320);
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaLogin());

        root.getChildren().addAll(titulo, campoNome, campoEmail, campoPassword,
                campoNif, campoNumeroCarta, campoValidadeCarta,
                comboPergunta, campoRespostaSeg,
                lblErro, btnRegistar, btnVoltar);
    }

    public VBox getRoot() {
        return root;
    }
}