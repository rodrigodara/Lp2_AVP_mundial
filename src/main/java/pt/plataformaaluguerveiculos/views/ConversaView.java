package pt.plataformaaluguerveiculos.views;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.aluguer.dao.UserDAO;
import com.aluguer.model.Mensagem;
import com.aluguer.model.Reserva;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.service.MensagemService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Página de chat entre o locatário e o proprietário de uma reserva ACEITE.
 *
 * O acesso é validado pelo MensagemService: só as duas partes da reserva
 * podem ver/enviar mensagens, e só enquanto o estado for ACEITE.
 */
public class ConversaView {

    private final VBox root;
    private final int reservaId;
    private final int utilizadorId;
    private final MensagemService mensagemService = new MensagemService();

    private VBox listaMensagens;
    private ScrollPane scroll;
    private Label lblErro;
    private TextArea txtMensagem;
    private Timeline autoRefresh;

    private int outroUtilizadorId = -1;
    private String nomeOutroUtilizador = "Utilizador";
    private boolean acessoValido = false;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public ConversaView(int reservaId, int utilizadorId) {
        this.reservaId = reservaId;
        this.utilizadorId = utilizadorId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        construirPagina();
    }

    private void construirPagina() {
        root.getChildren().clear();

        MensagemService.ResultadoConversa acesso = mensagemService.validarAcesso(reservaId, utilizadorId);
        acessoValido = acesso.isSucesso();

        if (!acessoValido) {
            construirCabecalho("Conversa", null);
            Label lblErroAcesso = new Label("⚠️  " + acesso.getErro());
            lblErroAcesso.setWrapText(true);
            lblErroAcesso.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #c62828; -fx-background-color: #ffebee;" +
                "-fx-background-radius: 8; -fx-padding: 16;"
            );
            root.getChildren().add(lblErroAcesso);
            return;
        }

        Reserva reserva = acesso.getReserva();
        Veiculo veiculo = acesso.getVeiculo();
        outroUtilizadorId = acesso.getOutroUtilizadorId();
        nomeOutroUtilizador = carregarNomeUtilizador(outroUtilizadorId);

        String subtitulo = veiculo.getMarca() + " " + veiculo.getModelo()
            + " (" + veiculo.getAno() + ")  •  Reserva #" + reserva.getId()
            + "  •  " + reserva.getDataInicio() + " → " + reserva.getDataFim();

        construirCabecalho("Conversa com " + nomeOutroUtilizador, subtitulo);

        listaMensagens = new VBox(10);
        listaMensagens.setPadding(new Insets(16));

        scroll = new ScrollPane(listaMensagens);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        lblErro = new Label();
        lblErro.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
        lblErro.setWrapText(true);
        lblErro.setVisible(false);

        HBox caixaEnvio = construirCaixaEnvio();

        root.getChildren().addAll(scroll, lblErro, caixaEnvio);

        carregarMensagens();
        mensagemService.marcarComoLidas(reservaId, utilizadorId);

        iniciarAutoRefresh();
    }

    private void construirCabecalho(String titulo, String subtitulo) {
        Label lblTitulo = new Label("💬  " + titulo);
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
        root.getChildren().add(lblTitulo);

        if (subtitulo != null) {
            Label lblSubtitulo = new Label(subtitulo);
            lblSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #777777;");
            root.getChildren().add(lblSubtitulo);
        }
    }

    private HBox construirCaixaEnvio() {
        txtMensagem = new TextArea();
        txtMensagem.setPromptText("Escreva uma mensagem...");
        txtMensagem.setWrapText(true);
        txtMensagem.setPrefRowCount(2);
        HBox.setHgrow(txtMensagem, Priority.ALWAYS);

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle(
            "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 10 20 10 20; -fx-cursor: hand;"
        );
        btnEnviar.setOnAction(e -> enviarMensagem());

        // Enter envia, Shift+Enter quebra linha
        txtMensagem.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                enviarMensagem();
            }
        });

        HBox caixa = new HBox(10, txtMensagem, btnEnviar);
        caixa.setAlignment(Pos.BOTTOM_RIGHT);
        return caixa;
    }

    private void enviarMensagem() {
        String texto = txtMensagem.getText();
        if (texto == null || texto.isBlank()) return;

        var resultado = mensagemService.enviarMensagem(reservaId, utilizadorId, texto);

        if (resultado.isSucesso()) {
            txtMensagem.clear();
            lblErro.setVisible(false);
            carregarMensagens();
        } else {
            lblErro.setText("⚠️ " + resultado.getMensagem());
            lblErro.setVisible(true);

            // Se a reserva deixou de estar ACEITE (ex.: foi concluída a meio
            // da conversa), refazemos a página para mostrar o estado real.
            construirPagina();
        }
    }

    private void carregarMensagens() {
        List<Mensagem> mensagens = mensagemService.listarMensagens(reservaId);
        listaMensagens.getChildren().clear();

        if (mensagens.isEmpty()) {
            Label vazio = new Label("Ainda não há mensagens. Diga olá!");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            listaMensagens.getChildren().add(vazio);
            return;
        }

        for (Mensagem m : mensagens) {
            listaMensagens.getChildren().add(criarBolha(m));
        }

        // marcar como lidas as que acabaram de chegar do outro lado
        mensagemService.marcarComoLidas(reservaId, utilizadorId);

        // scroll automático para o fundo
        javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private HBox criarBolha(Mensagem m) {
        boolean enviadaPorMim = m.foiEnviadaPor(utilizadorId);

        Label lblTexto = new Label(m.getConteudo());
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(360);
        lblTexto.setStyle(
            "-fx-font-size: 13px; -fx-padding: 10 14 10 14; -fx-background-radius: 14;" +
            (enviadaPorMim
                ? "-fx-background-color: #1a237e; -fx-text-fill: white;"
                : "-fx-background-color: #f0f1f5; -fx-text-fill: #222222;")
        );

        Label lblHora = new Label(
            m.getDataEnvio() != null ? m.getDataEnvio().format(FORMATO_HORA) : ""
        );
        lblHora.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");

        VBox bolha = new VBox(4, lblTexto, lblHora);
        bolha.setAlignment(enviadaPorMim ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox linha = new HBox(bolha);
        linha.setAlignment(enviadaPorMim ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return linha;
    }

    private void iniciarAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
        autoRefresh = new Timeline(
            new KeyFrame(Duration.seconds(8), e -> {
                if (acessoValido) carregarMensagens();
            })
        );
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    /** Deve ser chamado ao navegar para fora desta página, para parar o auto-refresh. */
    public void pararAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
    }

    private String carregarNomeUtilizador(int id) {
        try {
            return new UserDAO().findById(id).map(User::getNome).orElse("Utilizador #" + id);
        } catch (Exception ex) {
            return "Utilizador #" + id;
        }
    }

    public VBox getRoot() {
        return root;
    }
}