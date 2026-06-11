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
 * Mostra ao utilizador todas as suas transações financeiras
 * separadas em dois tabs: Pagamentos e Recebimentos.
 * Inclui resumo de totais no topo.
 */
public class HistoricoTransacoesView {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox root;
    private final int utilizadorId;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public HistoricoTransacoesView(int utilizadorId) {
        this.utilizadorId = utilizadorId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reservas-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // ALV-168 — Construir a página
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        // Cabeçalho
        Label titulo = new Label("Histórico de Transações");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Registo de todos os pagamentos e recebimentos");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);

        // Carregar dados da BD
        List<Transaction> pagamentos   = carregarPagamentos();
        List<Transaction> recebimentos = carregarRecebimentos();
        double totalPago     = calcularTotal(pagamentos);
        double totalRecebido = calcularTotal(recebimentos);

        // Cards de resumo
        HBox resumo = criarResumo(pagamentos.size(), totalPago,
                                   recebimentos.size(), totalRecebido);
        root.getChildren().add(resumo);

        // Se não há nenhuma transação
        if (pagamentos.isEmpty() && recebimentos.isEmpty()) {
            Label vazio = new Label("Ainda não existem transações registadas.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        // Tabs: Pagamentos | Recebimentos
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("reservas-tabs");

        tabs.getTabs().addAll(
            criarTab("Pagamentos (" + pagamentos.size() + ")",
                     pagamentos, "estado-rejeitado"),
            criarTab("Recebimentos (" + recebimentos.size() + ")",
                     recebimentos, "estado-concluido")
        );

        root.getChildren().add(tabs);
    }

    // ----------------------------------------------------------------
    // Cards de resumo no topo
    // ----------------------------------------------------------------

    private HBox criarResumo(int nPag, double totalPag, int nRec, double totalRec) {
        HBox hbox = new HBox(16);
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.getChildren().addAll(
            criarCardResumo("Pagamentos", nPag, totalPag, "estado-rejeitado"),
            criarCardResumo("Recebimentos", nRec, totalRec, "estado-concluido")
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

    // ----------------------------------------------------------------
    // Tab com lista de transações
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // Card de transação individual
    // ----------------------------------------------------------------

    private VBox criarCardTransacao(Transaction t, String estiloTipo) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(14));

        // Linha 1: id + tipo
        HBox linhaId = new HBox(10);
        linhaId.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Transação #" + t.getId());
        lblId.getStyleClass().add("reserva-card-id");

        Label lblTipo = new Label(t.getTipo().name());
        lblTipo.getStyleClass().addAll("reserva-estado", estiloTipo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValor = new Label(String.format("%.2f€", t.getValor()));
        lblValor.getStyleClass().add("reserva-card-id");

        linhaId.getChildren().addAll(lblId, lblTipo, spacer, lblValor);

        // Linha 2: reserva associada
        Label lblReserva = new Label("Reserva #" + t.getReservaId());
        lblReserva.getStyleClass().add("reserva-card-detalhe");

        // Linha 3: data
        Label lblData = new Label("Data: " + t.getData().format(FORMATO_DATA));
        lblData.getStyleClass().add("reserva-card-detalhe");

        // Linha 4: descrição
        Label lblDesc = new Label(t.getDescricao());
        lblDesc.getStyleClass().add("reserva-card-detalhe");

        card.getChildren().addAll(linhaId, lblReserva, lblData, lblDesc);
        return card;
    }

    // ----------------------------------------------------------------
    // Carregar dados da BD (ALV-169 via TransactionDAO)
    // ----------------------------------------------------------------

    private List<Transaction> carregarPagamentos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new TransactionDAO(conn).listarPagamentosPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private List<Transaction> carregarRecebimentos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new TransactionDAO(conn).listarRecebimentosPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // Auxiliar
    // ----------------------------------------------------------------

    private double calcularTotal(List<Transaction> lista) {
        return lista.stream().mapToDouble(Transaction::getValor).sum();
    }

    public VBox getRoot() {
        return root;
    }
}