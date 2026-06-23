package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Denuncia;
import com.aluguer.model.Reserva;
import com.aluguer.model.Veiculo;
import com.aluguer.service.AvaliacaoService;
import com.aluguer.service.DenunciaService;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MinhasReservasView {

    private VBox root;
    private final int utilizadorId;
    private int tabInicial = 0;
    private final DenunciaService denunciaService = new DenunciaService();

    public MinhasReservasView(int utilizadorId) {
        this(utilizadorId, 0);
    }

    public MinhasReservasView(int utilizadorId, int tabInicial) {
        this.utilizadorId = utilizadorId;
        this.tabInicial   = tabInicial;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        construirPagina();
    }

    private void construirPagina() {
        root.getChildren().clear();

        Label titulo = new Label("As Minhas Reservas");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label subtitulo = new Label("Histórico e estado das suas reservas");
        subtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #777777;");

        root.getChildren().addAll(titulo, subtitulo);

        List<Reserva> todas = carregarReservas();

        if (todas.isEmpty()) {
            Label vazio = new Label("Ainda não tem reservas registadas.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        // Pré-carrega os nomes dos veículos de uma só vez (evita N queries dentro do loop)
        Map<Integer, String> nomesVeiculos = carregarNomesVeiculos(todas);

        List<Reserva> pendentes  = filtrar(todas, Reserva.Estado.PENDENTE);
        List<Reserva> aceites    = filtrar(todas, Reserva.Estado.ACEITE);
        List<Reserva> rejeitadas = filtrar(todas, Reserva.Estado.REJEITADO);
        List<Reserva> canceladas = filtrar(todas, Reserva.Estado.CANCELADO);
        List<Reserva> concluidas = filtrar(todas, Reserva.Estado.CONCLUIDO);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: white;");

        tabs.getTabs().addAll(
            criarTab("Pendentes ("  + pendentes.size()  + ")", pendentes,  "#e65100", "#fff3e0", nomesVeiculos),
            criarTab("Aceites ("    + aceites.size()    + ")", aceites,    "#2e7d32", "#e8f5e9", nomesVeiculos),
            criarTab("Rejeitadas (" + rejeitadas.size() + ")", rejeitadas, "#c62828", "#ffebee", nomesVeiculos),
            criarTab("Canceladas (" + canceladas.size() + ")", canceladas, "#c62828", "#ffebee", nomesVeiculos),
            criarTab("Concluídas (" + concluidas.size() + ")", concluidas, "#1a237e", "#e8eaf6", nomesVeiculos)
        );

        tabs.getSelectionModel().select(tabInicial);

        root.getChildren().add(tabs);
    }

    /** Carrega os nomes (Marca Modelo Ano) de todos os veículos distintos referenciados nas reservas. */
    private Map<Integer, String> carregarNomesVeiculos(List<Reserva> reservas) {
        Map<Integer, String> nomes = new HashMap<>();
        VeiculoDAO dao = new VeiculoDAO();
        reservas.stream()
            .map(Reserva::getVeiculoId)
            .distinct()
            .forEach(id -> {
                try {
                    Veiculo v = dao.buscarPorId(id);
                    if (v != null) {
                        nomes.put(id, v.getMarca() + " " + v.getModelo() + " (" + v.getAno() + ")");
                    } else {
                        nomes.put(id, "Veículo #" + id);
                    }
                } catch (SQLException e) {
                    nomes.put(id, "Veículo #" + id);
                }
            });
        return nomes;
    }

    private Tab criarTab(String titulo, List<Reserva> reservas, String corTexto, String corFundo,
                         Map<Integer, String> nomesVeiculos) {
        Tab tab = new Tab(titulo);

        if (reservas.isEmpty()) {
            Label vazio = new Label("Sem reservas neste estado.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            vazio.setPadding(new Insets(30));
            VBox wrapper = new VBox(vazio);
            wrapper.setStyle("-fx-background-color: white;");
            tab.setContent(wrapper);
            return tab;
        }

        VBox lista = new VBox(12);
        lista.setPadding(new Insets(16));
        lista.setStyle("-fx-background-color: white;");

        for (Reserva r : reservas) {
            lista.getChildren().add(criarCard(r, corTexto, corFundo, nomesVeiculos));
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        tab.setContent(scroll);
        return tab;
    }

    private VBox criarCard(Reserva r, String corTexto, String corFundo, Map<Integer, String> nomesVeiculos) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);"
        );

        // Linha topo: ID + badge estado
        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label badge = new Label(r.getEstado().name());
        badge.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + corTexto + ";" +
            "-fx-background-color: " + corFundo + ";" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 2 8 2 8;"
        );

        topo.getChildren().addAll(lblId, badge);

        // Veículo — mostra o nome em vez do ID
        String nomeVeiculo = nomesVeiculos.getOrDefault(r.getVeiculoId(), "Veículo #" + r.getVeiculoId());
        Label lblVeiculo = new Label("Veículo: " + nomeVeiculo);
        lblVeiculo.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

        // Datas
        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

        // Preço
        Label lblPreco = new Label(
            "Total: " + String.format("%.2f€", r.getPrecoTotal())
            + "  |  Caução: " + String.format("%.2f€", r.getCaucao())
        );
        lblPreco.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

        card.getChildren().addAll(topo, lblVeiculo, lblDatas, lblPreco);

        // Aviso de denúncia / caução em disputa
        if (r.getEstado() == Reserva.Estado.CONCLUIDO && r.isCaucaoEmDisputa()) {
            Denuncia denuncia = denunciaService.buscarPendentePorReserva(r.getId());

            Label lblDisputa = new Label(
                "🚩 O proprietário reportou um problema nesta reserva. " +
                "A caução fica retida até decisão da administração."
            );
            lblDisputa.setWrapText(true);
            lblDisputa.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #c62828; -fx-font-weight: bold;" +
                "-fx-background-color: #ffebee; -fx-background-radius: 6; -fx-padding: 8;"
            );
            card.getChildren().add(lblDisputa);

            if (denuncia != null) {
                Button btnResponder = new Button(
                    denuncia.temResposta() ? "Resposta enviada" : "🗣  Responder à Denúncia"
                );
                btnResponder.setStyle(
                    "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-background-radius: 6; -fx-padding: 6 14;"
                );
                btnResponder.setDisable(denuncia.temResposta());
                btnResponder.setOnAction(e -> abrirDialogoResponderDenuncia(denuncia));

                HBox acoesDisputa = new HBox(btnResponder);
                acoesDisputa.setAlignment(Pos.CENTER_RIGHT);
                card.getChildren().add(acoesDisputa);
            }
        }

        // Botão Cancelar
        javafx.scene.control.Button btnCancelar = new javafx.scene.control.Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 6; -fx-padding: 6 12;"
        );

        if (podeCancelar(r)) {
            btnCancelar.setOnAction(e -> cancelarReserva(r));
            card.getChildren().add(btnCancelar);
        }

        // Botão Avaliar — apenas em reservas CONCLUÍDAS
        if (r.getEstado() == Reserva.Estado.CONCLUIDO) {
            try {
                AvaliacaoService avaliacaoService = new AvaliacaoService();
                boolean jaAvaliou = avaliacaoService.jaAvaliou(r.getId(), utilizadorId);

                Button btnAvaliar = new Button(jaAvaliou ? "Já avaliou" : "Avaliar Veículo");
                btnAvaliar.getStyleClass().add(jaAvaliou ? "btn-secundario" : "btn-primario");
                btnAvaliar.setDisable(jaAvaliou);
                btnAvaliar.setOnAction(e -> NavigationManager.getInstance().navegarParaAvaliar(
                    r.getId(),
                    utilizadorId,
                    r.getVeiculoId(),
                    nomeVeiculo
                ));

                HBox acoes = new HBox(btnAvaliar);
                acoes.setAlignment(Pos.CENTER_RIGHT);
                card.getChildren().add(acoes);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return card;
    }

    // ----------------------------------------------------------------
    // Diálogo: Responder à Denúncia (contraprova)
    // ----------------------------------------------------------------

    private void abrirDialogoResponderDenuncia(Denuncia denuncia) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label lblTitulo = new Label("Responder à Denúncia #" + denuncia.getId());
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        VBox topo = new VBox(lblTitulo);
        topo.setPadding(new Insets(18, 24, 16, 24));
        topo.setStyle("-fx-background-color: #1a237e; -fx-background-radius: 12 12 0 0;");

        Label lblMotivo = new Label("Motivo reportado: " + denuncia.getMotivo());
        lblMotivo.setWrapText(true);
        lblMotivo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

        // Foto do denunciante (se existir)
        StackPane fotoDenuncianteBox = new StackPane();
        fotoDenuncianteBox.setMinSize(140, 100);
        fotoDenuncianteBox.setMaxSize(140, 100);
        fotoDenuncianteBox.setStyle("-fx-background-color: #f1f1f1; -fx-background-radius: 6;");
        if (denuncia.getFoto() != null) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(denuncia.getFoto())));
            iv.setFitWidth(140);
            iv.setFitHeight(100);
            iv.setPreserveRatio(true);
            fotoDenuncianteBox.getChildren().add(iv);
        } else {
            Label lblSem = new Label("Sem foto");
            lblSem.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
            fotoDenuncianteBox.getChildren().add(lblSem);
        }

        Label lblInfo = new Label("Explica a tua versão. Podes juntar uma foto que prove o oposto.");
        lblInfo.setWrapText(true);
        lblInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

        TextArea txtResposta = new TextArea();
        txtResposta.setPromptText("A minha versão dos factos...");
        txtResposta.setWrapText(true);
        txtResposta.setPrefRowCount(4);

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
            chooser.setTitle("Escolher foto de contraprova");
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

        HBox linhaFotoDenunciante = new HBox(12, new Label("Prova do proprietário:"), fotoDenuncianteBox);
        linhaFotoDenunciante.setAlignment(Pos.CENTER_LEFT);

        HBox linhaFotoResposta = new HBox(16, previewBox, btnEscolherFoto);
        linhaFotoResposta.setAlignment(Pos.CENTER_LEFT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> dialog.close());

        Button btnEnviar = new Button("Enviar Resposta");
        btnEnviar.setStyle(
            "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;"
        );

        btnEnviar.setOnAction(e -> {
            String resposta = txtResposta.getText();
            if (resposta == null || resposta.isBlank()) {
                lblFeedback.setText("⚠️ Escreve a tua versão dos factos.");
                lblFeedback.setStyle("-fx-text-fill: #e65100;");
                return;
            }
            var resultado = denunciaService.responder(
                denuncia.getId(), utilizadorId, resposta, fotoSelecionada[0]
            );
            if (resultado.isSucesso()) {
                dialog.close();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
                );
                alert.setHeaderText(null);
                alert.setContentText(resultado.getMensagem());
                alert.showAndWait();
                construirPagina();
            } else {
                lblFeedback.setText("❌ " + resultado.getMensagem());
                lblFeedback.setStyle("-fx-text-fill: #c62828;");
            }
        });

        HBox botoes = new HBox(10, btnCancelar, btnEnviar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        VBox corpo = new VBox(14,
            lblMotivo, linhaFotoDenunciante, lblInfo, txtResposta, linhaFotoResposta,
            lblFeedback, botoes
        );
        corpo.setPadding(new Insets(20, 24, 22, 24));
        corpo.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");

        VBox layout = new VBox(topo, corpo);
        layout.setStyle("-fx-background-radius: 12; -fx-background-color: white;");

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 460, 560);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private boolean podeCancelar(Reserva r) {
        if (r.getEstado() != Reserva.Estado.ACEITE)
            return false;
        long horas = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                r.getDataInicio().atStartOfDay()
        ).toHours();
        return horas >= 48;
    }

    private void cancelarReserva(Reserva r) {
        com.aluguer.service.ReservaService service = new com.aluguer.service.ReservaService();
        var resultado = service.cancelarReserva(r.getId(), utilizadorId);

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            resultado.isSucesso() ? javafx.scene.control.Alert.AlertType.INFORMATION
                                  : javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setHeaderText(null);
        alert.setContentText(resultado.getMensagem());
        alert.showAndWait();

        if (resultado.isSucesso()) {
            construirPagina();
        }
    }

    private List<Reserva> carregarReservas() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new ReservaDAO(conn).listarPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private List<Reserva> filtrar(List<Reserva> lista, Reserva.Estado estado) {
        return lista.stream().filter(r -> r.getEstado() == estado).collect(Collectors.toList());
    }

    public VBox getRoot() { return root; }
}