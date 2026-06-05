package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * ALV-99  — Criar endpoint GET /my-reservations
 * ALV-100 — Criar página "As Minhas Reservas"
 * ALV-101 — Mostrar reservas pendentes
 * ALV-102 — Mostrar reservas aceites
 * ALV-103 — Mostrar reservas rejeitadas
 * ALV-104 — Testar listagem de reservas
 *
 * Página que o locatário vê ao clicar em "As Minhas Reservas".
 * Lista todas as reservas do utilizador organizada por estado em tabs.
 */
public class MinhasReservasView {

    private VBox root;
    private final int utilizadorId;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public MinhasReservasView(int utilizadorId) {
        this.utilizadorId = utilizadorId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reservas-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // Construção da UI — ALV-100
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        // Cabeçalho
        Label titulo = new Label("As Minhas Reservas");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Histórico e estado das suas reservas");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);

        // Carregar todas as reservas do utilizador — ALV-99
        List<Reserva> todas = carregarReservas();

        if (todas.isEmpty()) {
            Label vazio = new Label("Ainda não tem reservas registadas.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        // Separar por estado — ALV-101 / ALV-102 / ALV-103
        List<Reserva> pendentes   = filtrarPorEstado(todas, Reserva.Estado.PENDENTE);
        List<Reserva> aceites     = filtrarPorEstado(todas, Reserva.Estado.ACEITE);
        List<Reserva> rejeitadas  = filtrarPorEstado(todas, Reserva.Estado.REJEITADO);
        List<Reserva> canceladas  = filtrarPorEstado(todas, Reserva.Estado.CANCELADO);
        List<Reserva> concluidas  = filtrarPorEstado(todas, Reserva.Estado.CONCLUIDO);

        // Tabs por estado
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("reservas-tabs");

        tabs.getTabs().addAll(
            criarTab("Pendentes ("   + pendentes.size()  + ")", pendentes,  "estado-pendente"),
            criarTab("Aceites ("     + aceites.size()    + ")", aceites,    "estado-aceite"),
            criarTab("Rejeitadas ("  + rejeitadas.size() + ")", rejeitadas, "estado-rejeitado"),
            criarTab("Canceladas ("  + canceladas.size() + ")", canceladas, "estado-cancelado"),
            criarTab("Concluídas ("  + concluidas.size() + ")", concluidas, "estado-concluido")
        );

        root.getChildren().add(tabs);
    }

    // ----------------------------------------------------------------
    // Criar tab com lista de reservas
    // ----------------------------------------------------------------

    private Tab criarTab(String titulo, List<Reserva> reservas, String estiloEstado) {
        Tab tab = new Tab(titulo);

        if (reservas.isEmpty()) {
            Label vazio = new Label("Sem reservas neste estado.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(30));
            tab.setContent(vazio);
            return tab;
        }

        VBox listaCards = new VBox(12);
        listaCards.setPadding(new Insets(16));

        for (Reserva r : reservas) {
            listaCards.getChildren().add(criarCardReserva(r, estiloEstado));
        }

        ScrollPane scroll = new ScrollPane(listaCards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("reservas-scroll");
        scroll.setPrefHeight(450);

        tab.setContent(scroll);
        return tab;
    }

    // ----------------------------------------------------------------
    // Card de reserva
    // ----------------------------------------------------------------

    private VBox criarCardReserva(Reserva r, String estiloEstado) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(16));

        // Linha 1: id + estado
        HBox linhaId = new HBox(10);
        linhaId.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.getStyleClass().add("reserva-card-id");

        Label lblEstado = new Label(r.getEstado().name());
        lblEstado.getStyleClass().addAll("reserva-estado", estiloEstado);

        linhaId.getChildren().addAll(lblId, lblEstado);

        // Linha 2: veículo
        Label lblVeiculo = new Label("Veículo ID: " + r.getVeiculoId());
        lblVeiculo.getStyleClass().add("reserva-card-detalhe");

        // Linha 3: datas e duração
        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.getStyleClass().add("reserva-card-detalhe");

        // Linha 4: preço e caução
        Label lblPreco = new Label(
            "Total: " + String.format("%.2f€", r.getPrecoTotal())
            + "  |  Caução: " + String.format("%.2f€", r.getCaucao())
        );
        lblPreco.getStyleClass().add("reserva-card-detalhe");

        card.getChildren().addAll(linhaId, lblVeiculo, lblDatas, lblPreco);
        return card;
    }

    // ----------------------------------------------------------------
    // ALV-99 — Carregar reservas do utilizador (GET /my-reservations)
    // ----------------------------------------------------------------

    private List<Reserva> carregarReservas() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            return dao.listarPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // Auxiliar — filtrar por estado
    // ----------------------------------------------------------------

    private List<Reserva> filtrarPorEstado(List<Reserva> reservas, Reserva.Estado estado) {
        return reservas.stream()
                .filter(r -> r.getEstado() == estado)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public VBox getRoot() {
        return root;
    }
}