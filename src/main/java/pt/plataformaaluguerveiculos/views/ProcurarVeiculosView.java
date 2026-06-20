package pt.plataformaaluguerveiculos.views;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aluguer.model.Veiculo;
import com.aluguer.service.VeiculoService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProcurarVeiculosView {

    private VBox root;
    private TableView<Veiculo> tabela;

    // Filtros
    private ComboBox<String> comboMarca;
    private ComboBox<String> comboModelo;
    private ComboBox<String> comboPrecoPreset;
    private TextField campoPrecoMin;
    private TextField campoPrecoMax;
    private ComboBox<String> comboLocalizacao;
    private ComboBox<String> comboTipoVeiculo;
    private ComboBox<String> comboCombustivel;
    private ComboBox<Integer> comboLugares;
    private ComboBox<String> comboTransmissao;
    private ComboBox<Integer> comboAvaliacaoMin;
    private ComboBox<Integer> comboAvaliacaoMax;

    // Mapeamento label → [precoMin, precoMax] para os presets rápidos
    private static final Map<String, double[]> RANGES_PRECO = new LinkedHashMap<>();
    static {
        RANGES_PRECO.put("Até 30€/dia",      new double[]{-1, 30});
        RANGES_PRECO.put("30€ – 60€/dia",    new double[]{30, 60});
        RANGES_PRECO.put("60€ – 100€/dia",   new double[]{60, 100});
        RANGES_PRECO.put("Mais de 100€/dia", new double[]{100, -1});
    }

    public ProcurarVeiculosView() {

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Procurar Veículos");
        titulo.getStyleClass().add("dashboard-titulo");

        // ============================
        // BARRA DE PESQUISA POR TEXTO
        // ============================
        TextField campoPesquisa = new TextField();
        campoPesquisa.setPromptText("Pesquisar por marca, modelo ou localização...");
        campoPesquisa.getStyleClass().add("campo-texto");
        HBox.setHgrow(campoPesquisa, Priority.ALWAYS);

        Button btnPesquisar = new Button("Pesquisar");
        btnPesquisar.getStyleClass().add("btn-primario");

        Button btnLimparPesquisa = new Button("x");
        btnLimparPesquisa.getStyleClass().add("btn-secundario");

        HBox pesquisaBox = new HBox(8, campoPesquisa, btnPesquisar, btnLimparPesquisa);
        pesquisaBox.setAlignment(Pos.CENTER_LEFT);

        btnPesquisar.setOnAction(e -> {
            String termo = campoPesquisa.getText().trim();
            if (!termo.isEmpty()) {
                pesquisar(termo);
            }
        });
        campoPesquisa.setOnAction(e -> btnPesquisar.fire());
        btnLimparPesquisa.setOnAction(e -> {
            campoPesquisa.clear();
            carregarVeiculos();
        });

        // ============================
        // GRELHA DE FILTROS (2 linhas x 4 colunas)
        // ============================
        GridPane grelhaFiltros = construirGrelhaFiltros();

        Button btnLimparFiltros = new Button("Limpar filtros");
        btnLimparFiltros.getStyleClass().add("btn-secundario");

        Button btnAplicarFiltros = new Button("Aplicar filtros");
        btnAplicarFiltros.getStyleClass().add("btn-primario");

        HBox botoesFiltro = new HBox(10, btnAplicarFiltros, btnLimparFiltros);
        botoesFiltro.setAlignment(Pos.CENTER_LEFT);
        botoesFiltro.setPadding(new Insets(8, 0, 0, 0));

        VBox filtrosBox = new VBox(10, grelhaFiltros, botoesFiltro);
        filtrosBox.setPadding(new Insets(14));
        filtrosBox.setStyle(
            "-fx-background-color: #f8f9fb;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        btnAplicarFiltros.setOnAction(e -> aplicarFiltros());
        btnLimparFiltros.setOnAction(e -> {
            limparFiltros();
            carregarVeiculos();
        });

        // ============================
        // TABELA
        // ============================
        tabela = new TableView<>();
        tabela.setPrefHeight(550);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Veiculo, String> colMarca = new TableColumn<>("Marca");
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));

        TableColumn<Veiculo, String> colModelo = new TableColumn<>("Modelo");
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));

        TableColumn<Veiculo, Integer> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("ano"));

        TableColumn<Veiculo, String> colCombustivel = new TableColumn<>("Combustível");
        colCombustivel.setCellValueFactory(new PropertyValueFactory<>("combustivel"));

        TableColumn<Veiculo, Double> colPreco = new TableColumn<>("Preço Diário (€)");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoDiario"));

        TableColumn<Veiculo, String> colLocalizacao = new TableColumn<>("Localização");
        colLocalizacao.setCellValueFactory(new PropertyValueFactory<>("localizacao"));

        TableColumn<Veiculo, Integer> colLugares = new TableColumn<>("Lugares");
        colLugares.setCellValueFactory(new PropertyValueFactory<>("lugares"));

        TableColumn<Veiculo, String> colTransmissao = new TableColumn<>("Transmissão");
        colTransmissao.setCellValueFactory(new PropertyValueFactory<>("transmissao"));

        TableColumn<Veiculo, String> colAvaliacao = new TableColumn<>("Avaliação");
        colAvaliacao.setCellValueFactory(data -> {
            double media = data.getValue().getAvaliacaoMedia();
            String texto = media < 0 ? "Sem avaliações" : String.format("%.1f ★", media);
            return new javafx.beans.property.SimpleStringProperty(texto);
        });

        tabela.getColumns().addAll(
            colMarca, colModelo, colAno, colCombustivel, colPreco,
            colLocalizacao, colLugares, colTransmissao, colAvaliacao
        );

        // ============================
        // BOTÃO DE AÇÃO — Ver Detalhes
        // ============================
        Button btnVerDetalhes = new Button("Ver Detalhes");
        btnVerDetalhes.getStyleClass().add("btn-primario");
        btnVerDetalhes.setDisable(true);

        tabela.getSelectionModel().selectedItemProperty().addListener(
            (obs, antigo, novo) -> btnVerDetalhes.setDisable(novo == null)
        );

        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirDetalhe(tabela.getSelectionModel().getSelectedItem());
            }
        });

        btnVerDetalhes.setOnAction(e ->
            abrirDetalhe(tabela.getSelectionModel().getSelectedItem())
        );

        carregarVeiculos();

        HBox acoesBox = new HBox(10, btnVerDetalhes);
        acoesBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(titulo, pesquisaBox, filtrosBox, tabela, acoesBox);
    }

    // ============================
    // CONSTRUÇÃO DA GRELHA DE FILTROS
    // ============================
    private GridPane construirGrelhaFiltros() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);

        // ---- Marca ----
        comboMarca = new ComboBox<>();
        comboMarca.setPromptText("Todas as marcas");
        comboMarca.setPrefWidth(170);
        carregarMarcas(comboMarca);
        comboMarca.setOnAction(e -> {
            // Modelo depende da marca escolhida
            comboModelo.getSelectionModel().clearSelection();
            comboModelo.setItems(FXCollections.observableArrayList());
            String marcaSel = comboMarca.getValue();
            if (marcaSel != null) {
                carregarModelosPorMarca(comboModelo, marcaSel);
                comboModelo.setDisable(false);
            } else {
                comboModelo.setPromptText("Todos os modelos");
                comboModelo.setDisable(true);
            }
        });

        // ---- Modelo (dependente da marca) ----
        comboModelo = new ComboBox<>();
        comboModelo.setPromptText("Selecione marca primeiro");
        comboModelo.setPrefWidth(170);
        comboModelo.setDisable(true);

        // ---- Preço: preset rápido ----
        comboPrecoPreset = new ComboBox<>();
        comboPrecoPreset.setPromptText("Faixa de preço");
        comboPrecoPreset.setPrefWidth(170);
        comboPrecoPreset.setItems(FXCollections.observableArrayList(RANGES_PRECO.keySet()));
        comboPrecoPreset.setOnAction(e -> {
            String sel = comboPrecoPreset.getValue();
            if (sel != null) {
                double[] range = RANGES_PRECO.get(sel);
                campoPrecoMin.setText(range[0] < 0 ? "" : String.valueOf((int) range[0]));
                campoPrecoMax.setText(range[1] < 0 ? "" : String.valueOf((int) range[1]));
            }
        });

        // ---- Preço: valores livres (mín / máx) ----
        campoPrecoMin = new TextField();
        campoPrecoMin.setPromptText("Preço mín. (€)");
        campoPrecoMin.setPrefWidth(110);

        campoPrecoMax = new TextField();
        campoPrecoMax.setPromptText("Preço máx. (€)");
        campoPrecoMax.setPrefWidth(110);

        HBox precoLivreBox = new HBox(6, campoPrecoMin, campoPrecoMax);

        // ---- Localização ----
        comboLocalizacao = new ComboBox<>();
        comboLocalizacao.setPromptText("Todas as localizações");
        comboLocalizacao.setPrefWidth(170);
        carregarLocalizacoes(comboLocalizacao);

        // ---- Tipo de veículo ----
        comboTipoVeiculo = new ComboBox<>();
        comboTipoVeiculo.setPromptText("Todos os tipos");
        comboTipoVeiculo.setPrefWidth(170);
        carregarTiposVeiculo(comboTipoVeiculo);

        // ---- Combustível ----
        comboCombustivel = new ComboBox<>();
        comboCombustivel.setPromptText("Todos os combustíveis");
        comboCombustivel.setPrefWidth(170);
        carregarCombustiveis(comboCombustivel);

        // ---- Lugares ----
        comboLugares = new ComboBox<>();
        comboLugares.setPromptText("Qualquer nº de lugares");
        comboLugares.setPrefWidth(170);
        comboLugares.setItems(FXCollections.observableArrayList(2, 4, 5, 7, 9));

        // ---- Transmissão ----
        comboTransmissao = new ComboBox<>();
        comboTransmissao.setPromptText("Qualquer transmissão");
        comboTransmissao.setPrefWidth(170);
        carregarTransmissoes(comboTransmissao);

        // ---- Avaliação (mín / máx) ----
        comboAvaliacaoMin = new ComboBox<>();
        comboAvaliacaoMin.setPromptText("Avaliação mín. ★");
        comboAvaliacaoMin.setPrefWidth(170);
        comboAvaliacaoMin.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        comboAvaliacaoMax = new ComboBox<>();
        comboAvaliacaoMax.setPromptText("Avaliação máx. ★");
        comboAvaliacaoMax.setPrefWidth(170);
        comboAvaliacaoMax.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        HBox avaliacaoBox = new HBox(6, comboAvaliacaoMin, comboAvaliacaoMax);

        // ---- Montagem da grelha: 2 linhas x 4 colunas ----
        grid.add(criarCampoComLabel("Marca", comboMarca),                 0, 0);
        grid.add(criarCampoComLabel("Modelo", comboModelo),               1, 0);
        grid.add(criarCampoComLabel("Preço (€/dia)", precoLivreBox),      2, 0);
        grid.add(criarCampoComLabel("Faixa de preço", comboPrecoPreset),  3, 0);

        grid.add(criarCampoComLabel("Localização", comboLocalizacao),     0, 1);
        grid.add(criarCampoComLabel("Tipo de veículo", comboTipoVeiculo), 1, 1);
        grid.add(criarCampoComLabel("Combustível", comboCombustivel),     2, 1);
        grid.add(criarCampoComLabel("Lugares", comboLugares),             3, 1);

        grid.add(criarCampoComLabel("Transmissão", comboTransmissao),     0, 2);
        grid.add(criarCampoComLabel("Avaliação (★ mín / máx)", avaliacaoBox), 1, 2);

        return grid;
    }

    private VBox criarCampoComLabel(String texto, javafx.scene.Node campo) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        VBox box = new VBox(4, lbl, campo);
        return box;
    }

    // ============================
    // AÇÕES DE FILTRO
    // ============================
    private void aplicarFiltros() {
        String marca       = comboMarca.getValue();
        String modelo       = comboModelo.getValue();
        String localizacao  = comboLocalizacao.getValue();
        String tipoVeiculo  = comboTipoVeiculo.getValue();
        String combustivel  = comboCombustivel.getValue();
        Integer lugares     = comboLugares.getValue();
        String transmissao  = comboTransmissao.getValue();
        Integer avalMin     = comboAvaliacaoMin.getValue();
        Integer avalMax     = comboAvaliacaoMax.getValue();

        Double precoMin = parseDoubleOuNull(campoPrecoMin.getText());
        Double precoMax = parseDoubleOuNull(campoPrecoMax.getText());

        if (precoMin != null && precoMax != null && precoMin > precoMax) {
            mostrarAviso("O preço mínimo não pode ser maior que o preço máximo.");
            return;
        }
        if (avalMin != null && avalMax != null && avalMin > avalMax) {
            mostrarAviso("A avaliação mínima não pode ser maior que a avaliação máxima.");
            return;
        }

        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.getVehiclesComFiltros(
                marca, modelo, precoMin, precoMax, localizacao,
                tipoVeiculo, combustivel, lugares, transmissao,
                avalMin != null ? avalMin.doubleValue() : null,
                avalMax != null ? avalMax.doubleValue() : null
            );
            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limparFiltros() {
        comboMarca.setValue(null);
        comboModelo.setValue(null);
        comboModelo.setItems(FXCollections.observableArrayList());
        comboModelo.setPromptText("Selecione marca primeiro");
        comboModelo.setDisable(true);
        comboPrecoPreset.setValue(null);
        campoPrecoMin.clear();
        campoPrecoMax.clear();
        comboLocalizacao.setValue(null);
        comboTipoVeiculo.setValue(null);
        comboCombustivel.setValue(null);
        comboLugares.setValue(null);
        comboTransmissao.setValue(null);
        comboAvaliacaoMin.setValue(null);
        comboAvaliacaoMax.setValue(null);
    }

    private Double parseDoubleOuNull(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void mostrarAviso(String mensagem) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING
        );
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    /** Abre o detalhe completo do veículo, de onde o utilizador pode reservar. */
    private void abrirDetalhe(Veiculo veiculoLinha) {
        if (veiculoLinha == null) return;
        NavigationManager.getInstance().navegarParaDetalheVeiculo(
            veiculoLinha, DetalheVeiculoView.Origem.PROCURAR_VEICULOS
        );
    }

    private void pesquisar(String termo) {
        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.pesquisar(termo);
            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarVeiculos() {
        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.getAllVehicles();
            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarMarcas(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getMarcas()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarModelosPorMarca(ComboBox<String> combo, String marca) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getModelosPorMarca(marca)));
            combo.setPromptText("Todos os modelos");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarLocalizacoes(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getLocalizacoes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarTiposVeiculo(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getTiposVeiculo()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarCombustiveis(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getCombustiveis()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarTransmissoes(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getTransmissoes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VBox getRoot() {
        return root;
    }
}