package pt.plataformaaluguerveiculos.views;

import com.aluguer.dao.ReceitaVeiculoDAO;
import com.aluguer.model.ReceitaVeiculo;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * ALV-188 — Agrupar por veículo
 * ALV-189 — Criar endpoint estatísticas
 * ALV-190 — Mostrar valor acumulado
 *
 * Página que mostra ao proprietário a receita gerada por cada veículo,
 * com cards de resumo no topo e lista detalhada por veículo abaixo.
 */
public class ConsultaReceitaView {

    private final VBox root;
    private final int  proprietarioId;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public ConsultaReceitaView(int proprietarioId) {
        this.proprietarioId = proprietarioId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reservas-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // Construção da página
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        // Cabeçalho
        Label titulo = new Label("Consulta de Receita por Veículo");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Receita acumulada gerada por cada veículo");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);

        // Carregar dados
        List<ReceitaVeiculo> receitas    = carregarReceitas();
        double               receitaTotal = calcularTotal(receitas);
        int                  totalReservas = receitas.stream()
                                                     .mapToInt(ReceitaVeiculo::getTotalReservas)
                                                     .sum();

        // ALV-189 — Cards de estatísticas no topo
        HBox resumo = criarResumo(receitas.size(), totalReservas, receitaTotal);
        root.getChildren().add(resumo);

        // Lista vazia
        if (receitas.isEmpty()) {
            Label vazio = new Label("Ainda não existem receitas registadas para os seus veículos.");
            vazio.getStyleClass().add("reservas-vazio");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        // ALV-188 + ALV-190 — Lista de cards por veículo
        Label lblLista = new Label("Receita por Veículo");
        lblLista.getStyleClass().add("reservas-subtitulo");
        lblLista.setPadding(new Insets(8, 0, 0, 0));
        root.getChildren().add(lblLista);

        VBox listaCards = new VBox(10);
        listaCards.setPadding(new Insets(4, 0, 0, 0));

        for (ReceitaVeiculo rv : receitas) {
            listaCards.getChildren().add(criarCardVeiculo(rv, receitaTotal));
        }

        ScrollPane scroll = new ScrollPane(listaCards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("reservas-scroll");
        scroll.setPrefHeight(460);

        root.getChildren().add(scroll);
    }

    // ----------------------------------------------------------------
    // ALV-189 — Cards de resumo (estatísticas)
    // ----------------------------------------------------------------

    private HBox criarResumo(int nVeiculos, int nReservas, double total) {
        HBox hbox = new HBox(16);
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.getChildren().addAll(
            criarCard("Veículos",  String.valueOf(nVeiculos),  "estado-pendente"),
            criarCard("Reservas",  String.valueOf(nReservas),  "estado-concluido"),
            criarCard("Receita Total", String.format("%.2f€", total), "estado-concluido")
        );

        return hbox;
    }

    private VBox criarCard(String titulo, String valor, String estilo) {
        VBox card = new VBox(4);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setMinWidth(160);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().addAll("reserva-estado", estilo);

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("reserva-card-id");

        card.getChildren().addAll(lblTitulo, lblValor);
        return card;
    }

    // ----------------------------------------------------------------
    // ALV-188 + ALV-190 — Card individual por veículo
    // Mostra nome, receita acumulada, nº reservas e barra de progresso
    // ----------------------------------------------------------------

    private VBox criarCardVeiculo(ReceitaVeiculo rv, double totalGeral) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(14));

        // Linha 1: nome do veículo + receita (ALV-190 — valor acumulado)
        HBox linhaTop = new HBox(10);
        linhaTop.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(rv.getNomeVeiculo());
        lblNome.getStyleClass().add("reserva-card-id");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblReceita = new Label(String.format("%.2f€", rv.getReceitaTotal()));
        lblReceita.getStyleClass().addAll("reserva-estado", "estado-concluido");

        linhaTop.getChildren().addAll(lblNome, spacer, lblReceita);

        // Linha 2: nº de reservas
        Label lblReservas = new Label(
            rv.getTotalReservas() + " reserva" + (rv.getTotalReservas() != 1 ? "s" : "") + " aceite"
                + (rv.getTotalReservas() != 1 ? "s" : "")
        );
        lblReservas.getStyleClass().add("reserva-card-detalhe");

        // Linha 3: barra de progresso proporcional (ALV-190 — valor acumulado visual)
        double percentagem = totalGeral > 0 ? rv.getReceitaTotal() / totalGeral : 0;
        HBox barraFundo = new HBox();
        barraFundo.setStyle(
            "-fx-background-color: #e0e0e0; -fx-background-radius: 4; -fx-pref-height: 8;"
        );
        HBox.setHgrow(barraFundo, Priority.ALWAYS);

        Region barraPreenchida = new Region();
        barraPreenchida.setStyle(
            "-fx-background-color: #2e7d32; -fx-background-radius: 4;"
        );
        barraPreenchida.setPrefWidth(0); // será definido após layout

        // Usa binding para largura proporcional
        barraFundo.widthProperty().addListener((obs, oldW, newW) ->
            barraPreenchida.setPrefWidth(newW.doubleValue() * percentagem)
        );

        barraFundo.getChildren().add(barraPreenchida);

        Label lblPerc = new Label(String.format("%.1f%% da receita total", percentagem * 100));
        lblPerc.getStyleClass().add("reserva-card-detalhe");

        card.getChildren().addAll(linhaTop, lblReservas, barraFundo, lblPerc);
        return card;
    }

    // ----------------------------------------------------------------
    // Carregar dados da BD
    // ----------------------------------------------------------------

    private List<ReceitaVeiculo> carregarReceitas() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new ReceitaVeiculoDAO(conn).listarReceitaPorVeiculo(proprietarioId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // Auxiliar
    // ----------------------------------------------------------------

    private double calcularTotal(List<ReceitaVeiculo> lista) {
        return lista.stream().mapToDouble(ReceitaVeiculo::getReceitaTotal).sum();
    }

    public VBox getRoot() {
        return root;
    }
}