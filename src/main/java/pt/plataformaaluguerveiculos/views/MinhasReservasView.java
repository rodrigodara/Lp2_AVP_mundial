package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Avaliacao;
import com.aluguer.model.Reserva;
import com.aluguer.service.AvaliacaoService;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MinhasReservasView {

    private VBox root;
    private final int utilizadorId;
    private int tabInicial = 0; // 0=Pendentes, 1=Aceites, 2=Rejeitadas, 3=Canceladas, 4=Concluídas

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

        List<Reserva> pendentes  = filtrar(todas, Reserva.Estado.PENDENTE);
        List<Reserva> aceites    = filtrar(todas, Reserva.Estado.ACEITE);
        List<Reserva> rejeitadas = filtrar(todas, Reserva.Estado.REJEITADO);
        List<Reserva> canceladas = filtrar(todas, Reserva.Estado.CANCELADO);
        List<Reserva> concluidas = filtrar(todas, Reserva.Estado.CONCLUIDO);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: white;");

        tabs.getTabs().addAll(
            criarTab("Pendentes ("  + pendentes.size()  + ")", pendentes,  "#e65100",  "#fff3e0"),
            criarTab("Aceites ("    + aceites.size()    + ")", aceites,    "#2e7d32",  "#e8f5e9"),
            criarTab("Rejeitadas (" + rejeitadas.size() + ")", rejeitadas, "#c62828",  "#ffebee"),
            criarTab("Canceladas (" + canceladas.size() + ")", canceladas, "#c62828",  "#ffebee"),
            criarTab("Concluídas (" + concluidas.size() + ")", concluidas, "#1a237e",  "#e8eaf6")
        );

        tabs.getSelectionModel().select(tabInicial);

        root.getChildren().add(tabs);
    }

    private Tab criarTab(String titulo, List<Reserva> reservas, String corTexto, String corFundo) {
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
            lista.getChildren().add(criarCard(r, corTexto, corFundo));
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        tab.setContent(scroll);
        return tab;
    }

    private VBox criarCard(Reserva r, String corTexto, String corFundo) {
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

        // Veículo
        Label lblVeiculo = new Label("Veículo ID: " + r.getVeiculoId());
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

        // Botão Cancelar (só aparece se puder cancelar)
        javafx.scene.control.Button btnCancelar = new javafx.scene.control.Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 6; -fx-padding: 6 12;"
        );

        // Verificar se pode cancelar
        if (podeCancelar(r)) {
            btnCancelar.setOnAction(e -> cancelarReserva(r));
            card.getChildren().add(btnCancelar);
        }

        // Botão Avaliar — apenas em reservas CONCLUÍDAS
        if (r.getEstado() == Reserva.Estado.CONCLUIDO) {
            try {
                AvaliacaoService avaliacaoService = new AvaliacaoService();
                boolean jaAvaliou = avaliacaoService.jaAvaliou(r.getId(), utilizadorId);

                Button btnAvaliar = new Button(jaAvaliou ? "Já avaliou" : "Avaliar Proprietário");
                btnAvaliar.getStyleClass().add(jaAvaliou ? "btn-secundario" : "btn-primario");
                btnAvaliar.setDisable(jaAvaliou);
                btnAvaliar.setOnAction(e -> NavigationManager.getInstance().navegarParaAvaliar(
                    r.getId(),
                    utilizadorId,
                    r.getVeiculoId(), // será resolvido para o proprietário na view
                    Avaliacao.TipoAvaliado.PROPRIETARIO,
                    "Proprietário do veículo #" + r.getVeiculoId()
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

    //METODOS CACELAMENTO RESERVAS
    private boolean podeCancelar(Reserva r) {

    // 1. Só reservas ACEITES podem ser canceladas
    if (r.getEstado() != Reserva.Estado.ACEITE)
        return false;

    // 2. Regra das 48 horas
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
        construirPagina(); // refresca a página
    }
    }
    //FIM METODOS CANCELAR VIAGENS

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