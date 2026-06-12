package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.service.ReservaService;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public PedidosRecebidosView(int proprietarioId) {
        this.proprietarioId = proprietarioId;
        this.reservaService  = new ReservaService();

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

        Label lblVeiculo = new Label("Veículo ID: " + r.getVeiculoId());
        lblVeiculo.getStyleClass().add("reserva-card-detalhe");

        // --- ALV-91: Botões Aceitar / Rejeitar ---
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

        Label lblVeiculo = new Label("Veículo ID: " + r.getVeiculoId());
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

        Button btnConcluir = new Button("✔  Concluir Viagem");
        btnConcluir.getStyleClass().add("btn-aceitar");
        btnConcluir.setOnAction(e -> {
            ResultadoOperacao resultado = reservaService.concluirReserva(r.getId(), proprietarioId);
            mostrarFeedback(resultado);
            if (resultado.isSucesso()) construirPagina();
        });

        HBox acoes = new HBox(btnConcluir);
        acoes.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(linhaId, lblVeiculo, lblDatas, lblPreco, acoes);
        return card;
    }

    // ----------------------------------------------------------------
    // Lógica de dados
    // ----------------------------------------------------------------

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
        Alert alert = new Alert(
            resultado.isSucesso() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
        );
        alert.setTitle(resultado.isSucesso() ? "Sucesso" : "Erro");
        alert.setHeaderText(null);
        alert.setContentText(resultado.getMensagem());
        alert.getDialogPane().getStyleClass().add(
            resultado.isSucesso() ? "feedback-sucesso" : "feedback-error"
        );
        alert.showAndWait();
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public VBox getRoot() {
        return root;
    }
}
