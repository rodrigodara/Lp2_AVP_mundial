package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Veiculo;
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
    private int tabInicial = 0;

    public MinhasReservasView(int utilizadorId) {
        this(utilizadorId, 0);
    }

    public MinhasReservasView(int utilizadorId, int tabInicial) {
        this.utilizadorId = utilizadorId;
        this.tabInicial   = tabInicial;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F8FAFC;");

        construirPagina();
    }

    private void construirPagina() {
        root.getChildren().clear();

        Label titulo = new Label("As Minhas Reservas");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        Label subtitulo = new Label("Histórico e estado das suas reservas");
        subtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        root.getChildren().addAll(titulo, subtitulo);

        List<Reserva> todas = carregarReservas();

        if (todas.isEmpty()) {
            Label vazio = new Label("Ainda não tem reservas registadas.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
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
        tabs.setStyle("-fx-background-color: #F8FAFC;");

        tabs.getTabs().addAll(
            criarTab("Pendentes ("  + pendentes.size()  + ")", pendentes,  "#B45309", "#FEF3C7", nomesVeiculos),
            criarTab("Aceites ("    + aceites.size()    + ")", aceites,    "#22C55E", "#DCFCE7", nomesVeiculos),
            criarTab("Rejeitadas (" + rejeitadas.size() + ")", rejeitadas, "#EF4444", "#FEE2E2", nomesVeiculos),
            criarTab("Canceladas (" + canceladas.size() + ")", canceladas, "#EF4444", "#FEE2E2", nomesVeiculos),
            criarTab("Concluídas (" + concluidas.size() + ")", concluidas, "#2563EB", "#EAF2FF", nomesVeiculos)
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
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
            vazio.setPadding(new Insets(30));
            VBox wrapper = new VBox(vazio);
            wrapper.setStyle("-fx-background-color: #F8FAFC;");
            tab.setContent(wrapper);
            return tab;
        }

        VBox lista = new VBox(12);
        lista.setPadding(new Insets(16));
        lista.setStyle("-fx-background-color: #F8FAFC;");

        for (Reserva r : reservas) {
            lista.getChildren().add(criarCard(r, corTexto, corFundo, nomesVeiculos));
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #F8FAFC; -fx-background: #F8FAFC;");

        tab.setContent(scroll);
        return tab;
    }

    private VBox criarCard(Reserva r, String corTexto, String corFundo, Map<Integer, String> nomesVeiculos) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);"
        );

        // Linha topo: ID + badge estado
        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

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
        lblVeiculo.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");

        // Datas
        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");

        // Preço
        Label lblPreco = new Label(
            "Total: " + String.format("%.2f€", r.getPrecoTotal())
            + "  |  Caução: " + String.format("%.2f€", r.getCaucao())
        );
        lblPreco.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");

        card.getChildren().addAll(topo, lblVeiculo, lblDatas, lblPreco);

        // Botão Cancelar
        javafx.scene.control.Button btnCancelar = new javafx.scene.control.Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; " +
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