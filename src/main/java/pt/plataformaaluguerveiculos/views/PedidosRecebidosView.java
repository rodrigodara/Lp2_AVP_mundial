package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.service.DenunciaService;
import com.aluguer.service.ReservaService;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * ALV-90 — Criar página de pedidos recebidos
 * ALV-91 — Adicionar botões Aceitar/Rejeitar
 *
 * Página que o proprietário vê ao clicar em "Pedidos Recebidos".
 * Lista todas as reservas PENDENTES para os seus veículos e permite
 * aceitar ou rejeitar cada uma individualmente.
 */
public class PedidosRecebidosView {

    private VBox root;
    private final int proprietarioId;
    private final ReservaService reservaService;
    private final DenunciaService denunciaService;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public PedidosRecebidosView(int proprietarioId) {
        this.proprietarioId = proprietarioId;
        this.reservaService  = new ReservaService();
        this.denunciaService = new DenunciaService();

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("pedidos-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // Construção da UI
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        // Cabeçalho
        Label titulo = new Label("Pedidos Recebidos");
        titulo.getStyleClass().add("pedidos-titulo");

        Label subtitulo = new Label("Reservas pendentes de aprovação para os seus veículos");
        subtitulo.getStyleClass().add("pedidos-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);

        List<Reserva> pendentes = carregarPorEstado(Reserva.Estado.PENDENTE);
        List<Reserva> aceites   = carregarPorEstado(Reserva.Estado.ACEITE);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
            criarTabPendentes(pendentes),
            criarTabAceites(aceites)
        );

        root.getChildren().add(tabs);
    }

    // ----------------------------------------------------------------
    // Tabs
    // ----------------------------------------------------------------

    private Tab criarTabPendentes(List<Reserva> pendentes) {
        Tab tab = new Tab("Pendentes (" + pendentes.size() + ")");

        if (pendentes.isEmpty()) {
            Label vazio = new Label("Não tem pedidos pendentes de momento.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            VBox wrapper = new VBox(vazio);
            wrapper.setPadding(new Insets(30));
            tab.setContent(wrapper);
            return tab;
        }

        VBox lista = new VBox(12);
        lista.setPadding(new Insets(16));
        for (Reserva r : pendentes) lista.getChildren().add(criarCardReserva(r));

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pedidos-scroll");
        tab.setContent(scroll);
        return tab;
    }

    private Tab criarTabAceites(List<Reserva> aceites) {
        Tab tab = new Tab("Em Curso (" + aceites.size() + ")");

        if (aceites.isEmpty()) {
            Label vazio = new Label("Não tem reservas em curso de momento.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            VBox wrapper = new VBox(vazio);
            wrapper.setPadding(new Insets(30));
            tab.setContent(wrapper);
            return tab;
        }

        VBox lista = new VBox(12);
        lista.setPadding(new Insets(16));
        for (Reserva r : aceites) lista.getChildren().add(criarCardAceite(r));

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        tab.setContent(scroll);
        return tab;
    }

    // ----------------------------------------------------------------
    // ALV-91 — Card de reserva com botões Aceitar / Rejeitar
    // ----------------------------------------------------------------

    private VBox criarCardReserva(Reserva r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(16));

        // Linha 1: identificador + estado
        HBox linhaId = new HBox(10);
        linhaId.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.getStyleClass().add("reserva-card-id");

        Label lblEstado = new Label(r.getEstado().name());
        lblEstado.getStyleClass().addAll("reserva-estado", "estado-pendente");

        linhaId.getChildren().addAll(lblId, lblEstado);

        // Linha 2: datas e preço
        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.getStyleClass().add("reserva-card-detalhe");

        Label lblPreco = new Label(
            "Total: " + String.format("%.2f€", r.getPrecoTotal())
            + "  |  Caução: " + String.format("%.2f€", r.getCaucao())
        );
        lblPreco.getStyleClass().add("reserva-card-detalhe");

        Label lblVeiculo = new Label("🚗 " + nomeVeiculo(r.getVeiculoId()));
        lblVeiculo.getStyleClass().add("reserva-card-detalhe");
        Button btnAceitar  = new Button("✔  Aceitar");
        Button btnRejeitar = new Button("✘  Rejeitar");

        btnAceitar.getStyleClass().addAll("btn-aceitar");
        btnRejeitar.getStyleClass().addAll("btn-rejeitar");

        // Aceitar
        btnAceitar.setOnAction(e -> {
            ResultadoOperacao resultado = reservaService.aceitarReserva(r.getId(), proprietarioId);
            mostrarFeedback(resultado);
            if (resultado.isSucesso()) {
                construirPagina(); // recarrega a lista
            }
        });

        // Rejeitar
        btnRejeitar.setOnAction(e -> {
            ResultadoOperacao resultado = reservaService.rejeitarReserva(r.getId(), proprietarioId);
            mostrarFeedback(resultado);
            if (resultado.isSucesso()) {
                construirPagina(); // recarrega a lista
            }
        });

        HBox acoes = new HBox(12, btnAceitar, btnRejeitar);
        acoes.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(acoes, Priority.ALWAYS);

        card.getChildren().addAll(linhaId, lblVeiculo, lblDatas, lblPreco, acoes);
        return card;
    }

    // ----------------------------------------------------------------
    // Card para reservas ACEITES — botão Concluir
    // ----------------------------------------------------------------

    private VBox criarCardAceite(Reserva r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(16));

        HBox linhaId = new HBox(10);
        linhaId.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.getStyleClass().add("reserva-card-id");

        Label badge = new Label("ACEITE");
        badge.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;" +
            "-fx-background-color: #e8f5e9; -fx-background-radius: 4; -fx-padding: 2 8 2 8;"
        );

        linhaId.getChildren().addAll(lblId, badge);

        Label lblVeiculo = new Label("🚗 " + nomeVeiculo(r.getVeiculoId()));
        lblVeiculo.getStyleClass().add("reserva-card-detalhe");

        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.getStyleClass().add("reserva-card-detalhe");

        Label lblPreco = new Label(
            "Total: " + String.format("%.2f€", r.getPrecoTotal())
            + "  |  Caução: " + String.format("%.2f€", r.getCaucao())
        );
        lblPreco.getStyleClass().add("reserva-card-detalhe");

        Button btnConversar = new Button("💬  Conversar");
        btnConversar.setStyle(
            "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;"
        );
        btnConversar.setOnAction(e -> NavigationManager.getInstance().navegarParaConversa(r.getId()));

        Button btnConcluir = new Button("✔  Concluir Viagem");
        btnConcluir.getStyleClass().add("btn-aceitar");
        btnConcluir.setOnAction(e -> {
            ResultadoOperacao resultado = reservaService.concluirReserva(r.getId(), proprietarioId);
            mostrarFeedback(resultado);
            if (resultado.isSucesso()) construirPagina();
        });

        Button btnReportar = new Button("🚩  Reportar Problema");
        btnReportar.setStyle(
            "-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;"
        );
        btnReportar.setOnAction(e -> abrirDialogoReportar(r));

        HBox acoes = new HBox(10, btnConversar, btnReportar, btnConcluir);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(linhaId, lblVeiculo, lblDatas, lblPreco, acoes);
        return card;
    }

    // ----------------------------------------------------------------
    // Diálogo: Reportar Problema (motivo + foto de prova)
    // ----------------------------------------------------------------

    private void abrirDialogoReportar(Reserva r) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label lblTitulo = new Label("Reportar problema — Reserva #" + r.getId());
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        VBox topo = new VBox(lblTitulo);
        topo.setPadding(new Insets(18, 24, 16, 24));
        topo.setStyle("-fx-background-color: #c62828; -fx-background-radius: 12 12 0 0;");

        Label lblInfo = new Label(
            "Descreve o problema encontrado (ex.: danos no veículo). A reserva será " +
            "concluída e a caução de " + String.format("%.2f€", r.getCaucao()) +
            " fica retida até a administração decidir o caso."
        );
        lblInfo.setWrapText(true);
        lblInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText("Descreve o problema...");
        txtMotivo.setWrapText(true);
        txtMotivo.setPrefRowCount(4);

        // ---- Foto de prova ----
        byte[][] fotoSelecionada = {null};
        StackPane previewBox = new StackPane();
        previewBox.setMinSize(120, 90);
        previewBox.setMaxSize(120, 90);
        previewBox.setStyle("-fx-background-color: #f1f1f1; -fx-background-radius: 6;");

        Label lblSemFoto = new Label("Sem foto");
        lblSemFoto.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
        previewBox.getChildren().add(lblSemFoto);

        Button btnEscolherFoto = new Button("📷  Escolher foto…");
        btnEscolherFoto.getStyleClass().add("btn-secundario");

        Label lblFeedback = new Label();
        lblFeedback.setStyle("-fx-font-size: 12px;");

        btnEscolherFoto.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Escolher foto de prova");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File ficheiro = chooser.showOpenDialog(dialog);
            if (ficheiro != null) {
                try {
                    fotoSelecionada[0] = Files.readAllBytes(ficheiro.toPath());
                    ImageView iv = new ImageView(new Image(new ByteArrayInputStream(fotoSelecionada[0])));
                    iv.setFitWidth(120);
                    iv.setFitHeight(90);
                    iv.setPreserveRatio(true);
                    previewBox.getChildren().setAll(iv);
                } catch (IOException ex) {
                    lblFeedback.setText("❌ Não foi possível ler a imagem selecionada.");
                    lblFeedback.setStyle("-fx-text-fill: #c62828;");
                }
            }
        });

        HBox linhaFoto = new HBox(16, previewBox, btnEscolherFoto);
        linhaFoto.setAlignment(Pos.CENTER_LEFT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> dialog.close());

        Button btnConfirmar = new Button("🚩  Reportar e Concluir");
        btnConfirmar.setStyle(
            "-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;"
        );

        btnConfirmar.setOnAction(e -> {
            String motivo = txtMotivo.getText();
            if (motivo == null || motivo.isBlank()) {
                lblFeedback.setText("⚠️ Indica o motivo do problema.");
                lblFeedback.setStyle("-fx-text-fill: #e65100;");
                return;
            }
            ResultadoOperacao resultado = denunciaService.reportarProblemaReserva(
                r.getId(), proprietarioId, motivo, fotoSelecionada[0]
            );
            if (resultado.isSucesso()) {
                dialog.close();
                javafx.application.Platform.runLater(() -> {
                    mostrarFeedback(resultado);
                    construirPagina();
                });
            } else {
                lblFeedback.setText("❌ " + resultado.getMensagem());
                lblFeedback.setStyle("-fx-text-fill: #c62828;");
            }
        });

        HBox botoes = new HBox(10, btnCancelar, btnConfirmar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        VBox corpo = new VBox(14, lblInfo, txtMotivo, linhaFoto, lblFeedback, botoes);
        corpo.setPadding(new Insets(20, 24, 22, 24));
        corpo.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");

        VBox layout = new VBox(topo, corpo);
        layout.setStyle("-fx-background-radius: 12; -fx-background-color: white;");

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 440, 420);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ----------------------------------------------------------------
    // Lógica de dados
    // ----------------------------------------------------------------

    /** Cache simples: evita ir à BD repetidamente para o mesmo veículo na mesma página. */
    private final java.util.Map<Integer, String> cacheNomeVeiculo = new java.util.HashMap<>();

    private String nomeVeiculo(int veiculoId) {
        return cacheNomeVeiculo.computeIfAbsent(veiculoId, id -> {
            try {
                com.aluguer.dao.VeiculoDAO veiculoDAO = new com.aluguer.dao.VeiculoDAO();
                com.aluguer.model.Veiculo veiculo = veiculoDAO.buscarPorId(id);
                if (veiculo != null) {
                    return veiculo.getMarca() + " " + veiculo.getModelo();
                }
            } catch (SQLException e) {
                System.err.println("[PedidosRecebidosView] Erro ao buscar veículo #" + id + ": " + e.getMessage());
            }
            return "Veículo #" + id;
        });
    }

    private List<Reserva> carregarPorEstado(Reserva.Estado estado) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            return dao.listarTodasPorProprietario(proprietarioId).stream()
                .filter(r -> r.getEstado() == estado)
                .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // Feedback ao utilizador
    // ----------------------------------------------------------------

    private void mostrarFeedback(ResultadoOperacao resultado) {
        boolean sucesso = resultado.isSucesso();
        String cor = sucesso ? "#2e7d32" : "#c62828";
        String icone = sucesso ? "✔" : "✘";
        String titulo = sucesso ? "Sucesso" : "Erro";

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(root.getScene() != null ? root.getScene().getWindow() : null);

        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox iconeTitulo = new HBox(10, lblIcone, lblTitulo);
        iconeTitulo.setAlignment(Pos.CENTER_LEFT);

        Button btnFechar = new Button("×");
        btnFechar.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"
        );
        btnFechar.setOnAction(e -> dialog.close());

        HBox topo = new HBox(iconeTitulo, btnFechar);
        topo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(iconeTitulo, Priority.ALWAYS);
        topo.setPadding(new Insets(16, 18, 14, 20));
        topo.setStyle("-fx-background-color: " + cor + "; -fx-background-radius: 14 14 0 0;");

        Label lblMensagem = new Label(resultado.getMensagem());
        lblMensagem.setWrapText(true);
        lblMensagem.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        Button btnOk = new Button("OK");
        btnOk.setStyle(
            "-fx-background-color: " + cor + "; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 8 28 8 28; -fx-cursor: hand;"
        );
        btnOk.setOnAction(e -> dialog.close());
        btnOk.setDefaultButton(true);

        HBox linhaBotao = new HBox(btnOk);
        linhaBotao.setAlignment(Pos.CENTER_RIGHT);

        VBox corpo = new VBox(18, lblMensagem, linhaBotao);
        corpo.setPadding(new Insets(20, 22, 20, 22));
        corpo.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 14 14;");

        VBox layout = new VBox(topo, corpo);
        layout.setStyle(
            "-fx-background-radius: 14; -fx-background-color: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 0, 6);"
        );

        StackPane wrapper = new StackPane(layout);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(12));

        javafx.scene.Scene scene = new javafx.scene.Scene(wrapper);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public VBox getRoot() {
        return root;
    }
}