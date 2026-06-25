package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.aluguer.dao.ReceitaVeiculoDAO;
import com.aluguer.model.ReceitaVeiculo;
import com.aluguer.util.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        javafx.scene.control.Button btnVoltar = new javafx.scene.control.Button("←  Voltar aos Meus Veículos");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaMeusVeiculos());

        javafx.scene.control.Button btnExportarPdf = new javafx.scene.control.Button("📄 Exportar PDF");
        btnExportarPdf.getStyleClass().add("btn-primario");
        btnExportarPdf.setOnAction(e -> exportarRelatorioPdf());

        HBox linhaBotoes = new HBox(10, btnVoltar, btnExportarPdf);
        linhaBotoes.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Consulta de Receita por Veículo");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Receita acumulada gerada por cada veículo");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        root.getChildren().addAll(linhaBotoes, titulo, subtitulo);

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

        // ---- Gráfico comparativo de evolução de receita ----
        Label lblGrafico = new Label("Comparar Veículos");
        lblGrafico.getStyleClass().add("reservas-subtitulo");
        lblGrafico.setPadding(new Insets(8, 0, 0, 0));
        root.getChildren().add(lblGrafico);

        root.getChildren().add(criarSecaoGrafico(receitas));

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
    // Gráfico comparativo de evolução de receita entre veículos
    // ----------------------------------------------------------------

    private VBox criarSecaoGrafico(List<ReceitaVeiculo> receitas) {
        VBox secao = new VBox(14);

        // ---- Controlos: Veículo A, Veículo B, período, tipo de gráfico ----
        ComboBox<ReceitaVeiculo> comboVeiculoA = new ComboBox<>(FXCollections.observableArrayList(receitas));
        comboVeiculoA.getStyleClass().add("combo-filtro");
        comboVeiculoA.setPromptText("Veículo A");
        comboVeiculoA.setConverter(criarConversorVeiculo());
        comboVeiculoA.setValue(receitas.get(0));
        comboVeiculoA.setPrefWidth(220);

        ComboBox<ReceitaVeiculo> comboVeiculoB = new ComboBox<>(FXCollections.observableArrayList(receitas));
        comboVeiculoB.getStyleClass().add("combo-filtro");
        comboVeiculoB.setPromptText("Veículo B (opcional)");
        comboVeiculoB.setConverter(criarConversorVeiculo());
        comboVeiculoB.setPrefWidth(220);

        ToggleGroup tgPeriodo = new ToggleGroup();
        RadioButton rbDia   = new RadioButton("Dia");   rbDia.setToggleGroup(tgPeriodo);
        RadioButton rbSemana = new RadioButton("Semana"); rbSemana.setToggleGroup(tgPeriodo);
        RadioButton rbMes   = new RadioButton("Mês");   rbMes.setToggleGroup(tgPeriodo); rbMes.setSelected(true);
        RadioButton rbAno   = new RadioButton("Ano");   rbAno.setToggleGroup(tgPeriodo);
        HBox periodoBox = new HBox(12, rbDia, rbSemana, rbMes, rbAno);
        periodoBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup tgTipo = new ToggleGroup();
        RadioButton rbBarras = new RadioButton("📊 Barras"); rbBarras.setToggleGroup(tgTipo); rbBarras.setSelected(true);
        RadioButton rbLinha  = new RadioButton("📈 Linha");  rbLinha.setToggleGroup(tgTipo);
        HBox tipoBox = new HBox(12, rbBarras, rbLinha);
        tipoBox.setAlignment(Pos.CENTER_LEFT);

        Button btnAtualizar = new Button("Atualizar Gráfico");
        btnAtualizar.getStyleClass().add("btn-primario");

        HBox linhaControlos = new HBox(16,
            criarCampoComLabelGrafico("Veículo A", comboVeiculoA),
            criarCampoComLabelGrafico("Veículo B", comboVeiculoB),
            criarCampoComLabelGrafico("Período", periodoBox),
            criarCampoComLabelGrafico("Tipo de gráfico", tipoBox),
            btnAtualizar
        );
        linhaControlos.setAlignment(Pos.CENTER_LEFT);

        // ---- Área do gráfico: troca entre BarChart e LineChart ----
        StackPane areaGrafico = new StackPane();
        areaGrafico.setPrefHeight(320);

        Runnable atualizar = () -> desenharGrafico(
            areaGrafico,
            comboVeiculoA.getValue(),
            comboVeiculoB.getValue(),
            agrupamentoSelecionado(rbDia, rbSemana, rbAno),
            rbBarras.isSelected()
        );

        btnAtualizar.setOnAction(e -> atualizar.run());
        atualizar.run(); // desenha logo com os valores por defeito

        secao.getChildren().addAll(linhaControlos, areaGrafico);
        return secao;
    }

    private String agrupamentoSelecionado(RadioButton rbDia, RadioButton rbSemana, RadioButton rbAno) {
        if (rbDia.isSelected()) return "DAY";
        if (rbSemana.isSelected()) return "WEEK";
        if (rbAno.isSelected()) return "YEAR";
        return "MONTH";
    }

    private VBox criarCampoComLabelGrafico(String texto, javafx.scene.Node campo) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        return new VBox(4, lbl, campo);
    }

    private javafx.util.StringConverter<ReceitaVeiculo> criarConversorVeiculo() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(ReceitaVeiculo rv) {
                return rv == null ? "Nenhum" : rv.getNomeVeiculo();
            }
            @Override
            public ReceitaVeiculo fromString(String s) {
                return null; // não editável, só seleção
            }
        };
    }

    /** Desenha o gráfico (barras ou linha) com 1 ou 2 veículos, no período escolhido. */
    private void desenharGrafico(StackPane area, ReceitaVeiculo veiculoA, ReceitaVeiculo veiculoB,
                                  String agrupamento, boolean usarBarras) {
        area.getChildren().clear();
        if (veiculoA == null) {
            Label vazio = new Label("Seleciona pelo menos o Veículo A.");
            vazio.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
            area.getChildren().add(vazio);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            ReceitaVeiculoDAO dao = new ReceitaVeiculoDAO(conn);
            List<Object[]> dadosA = dao.evolucaoReceitaPorVeiculo(veiculoA.getVeiculoId(), agrupamento);
            List<Object[]> dadosB = (veiculoB != null && veiculoB.getVeiculoId() != veiculoA.getVeiculoId())
                ? dao.evolucaoReceitaPorVeiculo(veiculoB.getVeiculoId(), agrupamento)
                : null;

            if (usarBarras) {
                BarChart<String, Number> chart = criarGraficoBarrasReceita();
                chart.getData().add(criarSerieBarras(veiculoA.getNomeVeiculo(), dadosA));
                if (dadosB != null) chart.getData().add(criarSerieBarras(veiculoB.getNomeVeiculo(), dadosB));
                chart.setLegendVisible(dadosB != null);
                area.getChildren().add(chart);
            } else {
                LineChart<String, Number> chart = criarGraficoLinhaReceita();
                chart.getData().add(criarSerieLinha(veiculoA.getNomeVeiculo(), dadosA));
                if (dadosB != null) chart.getData().add(criarSerieLinha(veiculoB.getNomeVeiculo(), dadosB));
                area.getChildren().add(chart);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label erro = new Label("Erro ao carregar dados do gráfico.");
            erro.setStyle("-fx-text-fill: #c62828;");
            area.getChildren().add(erro);
        }
    }

    private BarChart<String, Number> criarGraficoBarrasReceita() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Receita (€)");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(320);
        chart.setCategoryGap(12);
        return chart;
    }

    private LineChart<String, Number> criarGraficoLinhaReceita() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Receita (€)");
        xAxis.setLabel("Período");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(320);
        chart.setCreateSymbols(true);
        return chart;
    }

    private XYChart.Series<String, Number> criarSerieBarras(String nome, List<Object[]> dados) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nome);
        for (Object[] row : dados) {
            double valor = (double) row[1];
            XYChart.Data<String, Number> ponto = new XYChart.Data<>(String.valueOf(row[0]), valor);
            serie.getData().add(ponto);
            ponto.nodeProperty().addListener((obs, oldN, newN) -> {
                if (newN != null) {
                    Tooltip tt = new Tooltip(String.format("%s — %s\n%.2f €", nome, row[0], valor));
                    Tooltip.install(newN, tt);
                }
            });
        }
        return serie;
    }

    private XYChart.Series<String, Number> criarSerieLinha(String nome, List<Object[]> dados) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nome);
        for (Object[] row : dados) {
            serie.getData().add(new XYChart.Data<>(String.valueOf(row[0]), (double) row[1]));
        }
        return serie;
    }


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

    // ----------------------------------------------------------------
    // Exportação para PDF
    // ----------------------------------------------------------------

    private void exportarRelatorioPdf() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setInitialFileName("relatorio_receita.pdf");
        chooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Ficheiro PDF", "*.pdf"));

        java.io.File destino = chooser.showSaveDialog(root.getScene().getWindow());
        if (destino == null) return; // utilizador cancelou

        try {
            com.aluguer.model.User utilizador = com.aluguer.util.SessionManager.getInstance().getUtilizador();
            new com.aluguer.service.RelatorioReceitaPdfService().gerarRelatorio(utilizador, destino);
        } catch (Exception ex) {
            ex.printStackTrace();
            Label erro = new Label("Não foi possível gerar o PDF: " + ex.getMessage());
            erro.setStyle("-fx-text-fill: #c62828;");
            root.getChildren().add(erro);
        }
    }
}