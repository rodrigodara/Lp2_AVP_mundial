package pt.plataformaaluguerveiculos.views;

import com.aluguer.dao.TransactionDAO;
import com.aluguer.model.Transaction;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ALV-168 — Criar página histórico
 *
 * Mostra ao utilizador todas as suas transações da carteira:
 * depósitos e levantamentos.
 */
public class HistoricoTransacoesView {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox root;
    private final int utilizadorId;

    public HistoricoTransacoesView(int utilizadorId) {
        this.utilizadorId = utilizadorId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reservas-container");

        construirPagina();
    }

    private void construirPagina() {
        root.getChildren().clear();

        Label titulo = new Label("Histórico de Transações");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Registo de todos os depósitos e levantamentos da sua carteira");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);

        List<Transaction> depositos     = carregarDepositos();
        List<Transaction> levantamentos = carregarLevantamentos();
        double totalDepositos     = calcularTotal(depositos);
        double totalLevantamentos = calcularTotal(levantamentos);

        HBox resumo = criarResumo(depositos.size(), totalDepositos,
                                   levantamentos.size(), totalLevantamentos);
        root.getChildren().add(resumo);

        if (depositos.isEmpty() && levantamentos.isEmpty()) {
            Label vazio = new Label("Ainda não existem transações registadas.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("reservas-tabs");

        tabs.getTabs().addAll(
            criarTab("Depósitos (" + depositos.size() + ")",
                     depositos, "estado-concluido"),
            criarTab("Levantamentos (" + levantamentos.size() + ")",
                     levantamentos, "estado-rejeitado")
        );

        root.getChildren().add(tabs);
    }

    private HBox criarResumo(int nDep, double totalDep, int nLev, double totalLev) {
        HBox hbox = new HBox(16);
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.getChildren().addAll(
            criarCardResumo("Depósitos",     nDep, totalDep, "estado-concluido"),
            criarCardResumo("Levantamentos", nLev, totalLev, "estado-rejeitado")
        );

        return hbox;
    }

    private VBox criarCardResumo(String titulo, int count, double total, String estilo) {
        VBox card = new VBox(4);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setMinWidth(200);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().addAll("reserva-estado", estilo);

        Label lblCount = new Label(count + " transação" + (count != 1 ? "ões" : ""));
        lblCount.getStyleClass().add("reserva-card-detalhe");

        Label lblTotal = new Label(String.format("Total: %.2f€", total));
        lblTotal.getStyleClass().add("reserva-card-id");

        card.getChildren().addAll(lblTitulo, lblCount, lblTotal);
        return card;
    }

    private Tab criarTab(String tituloTab, List<Transaction> transacoes, String estilo) {
        Tab tab = new Tab(tituloTab);

        if (transacoes.isEmpty()) {
            Label vazio = new Label("Sem transações nesta categoria.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(30));
            tab.setContent(vazio);
            return tab;
        }

        VBox listaCards = new VBox(10);
        listaCards.setPadding(new Insets(16));

        for (Transaction t : transacoes) {
            listaCards.getChildren().add(criarCardTransacao(t, estilo));
        }

        ScrollPane scroll = new ScrollPane(listaCards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("reservas-scroll");
        scroll.setPrefHeight(450);

        tab.setContent(scroll);
        return tab;
    }

    private VBox criarCardTransacao(Transaction t, String estiloTipo) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(14));

        HBox linhaId = new HBox(10);
        linhaId.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Transação #" + t.getId());
        lblId.getStyleClass().add("reserva-card-id");

        Label lblTipo = new Label(t.getTipo().name().toUpperCase());
        lblTipo.getStyleClass().addAll("reserva-estado", estiloTipo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValor = new Label(String.format("%.2f€", t.getValor()));
        lblValor.getStyleClass().add("reserva-card-id");

        linhaId.getChildren().addAll(lblId, lblTipo, spacer, lblValor);

        Label lblData = new Label("Data: " +
            (t.getData() != null ? t.getData().format(FORMATO_DATA) : "—"));
        lblData.getStyleClass().add("reserva-card-detalhe");

        card.getChildren().addAll(linhaId, lblData);
        return card;
    }

    private List<Transaction> carregarDepositos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new TransactionDAO(conn).listarDepositosPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private List<Transaction> carregarLevantamentos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new TransactionDAO(conn).listarLevantamentosPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private double calcularTotal(List<Transaction> lista) {
        return lista.stream().mapToDouble(Transaction::getValor).sum();
    }

    public VBox getRoot() {
        return root;
    }
}
