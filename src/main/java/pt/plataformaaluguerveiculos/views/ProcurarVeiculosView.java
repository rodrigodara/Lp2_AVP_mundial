package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ProcurarVeiculosView {

    private VBox root;

    /** ALV-XX (redesign): grelha de cards que substitui a antiga TableView. */
    private FlowPane grelhaResultados;
    private Label labelSemResultados;

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
            "-fx-background-color: #EAF2FF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        btnAplicarFiltros.setOnAction(e -> aplicarFiltros());
        btnLimparFiltros.setOnAction(e -> {
            limparFiltros();
            carregarVeiculos();
        });

        // ============================
        // GRELHA DE CARDS (ALV-XX redesign — substitui a TableView)
        // ============================
        // NOTA: sem ScrollPane aqui — o BaseLayoutView já envolve toda a
        // página num ScrollPane; aninhar outro por dentro causava o
        // colapso de layout (cards sem altura, texto a fugir da caixa).
        grelhaResultados = new FlowPane();
        grelhaResultados.setHgap(24);
        grelhaResultados.setVgap(24);
        grelhaResultados.setPadding(new Insets(4));
        grelhaResultados.setPrefWrapLength(1100);

        labelSemResultados = new Label("Nenhum veículo encontrado com os critérios escolhidos.");
        labelSemResultados.getStyleClass().add("veiculo-grid-vazio");
        labelSemResultados.setVisible(false);
        labelSemResultados.setManaged(false);

        carregarVeiculos();

        root.getChildren().addAll(titulo, pesquisaBox, filtrosBox, labelSemResultados, grelhaResultados);
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
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
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
            popularGrelha(lista);
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
            popularGrelha(lista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarVeiculos() {
        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.getAllVehicles();
            popularGrelha(lista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================
    // GRELHA DE CARDS (ALV-XX redesign)
    // ============================

    /** Substitui o conteúdo da grelha de resultados pelos cards da lista dada. */
    private void popularGrelha(List<Veiculo> veiculos) {
        grelhaResultados.getChildren().clear();

        boolean vazio = veiculos == null || veiculos.isEmpty();
        labelSemResultados.setVisible(vazio);
        labelSemResultados.setManaged(vazio);

        if (vazio) return;

        for (Veiculo veiculo : veiculos) {
            grelhaResultados.getChildren().add(criarCardVeiculo(veiculo));
        }
    }


    private static final double CARD_LARGURA = 300;
    private static final double CARD_FOTO_ALTURA = 190;

    private VBox criarCardVeiculo(Veiculo veiculo) {
        VBox card = new VBox();
        card.getStyleClass().add("veiculo-card");
        card.setPrefWidth(CARD_LARGURA);
        card.setMaxWidth(CARD_LARGURA);

        // ---- Foto (primeira foto disponível, ou placeholder) ----
        StackPane fotoWrapper = new StackPane();
        fotoWrapper.getStyleClass().add("veiculo-card-foto-wrapper");
        fotoWrapper.setPrefSize(CARD_LARGURA, CARD_FOTO_ALTURA);
        fotoWrapper.setMinSize(CARD_LARGURA, CARD_FOTO_ALTURA);
        fotoWrapper.setMaxSize(CARD_LARGURA, CARD_FOTO_ALTURA);

        // Cantos arredondados no topo da foto (clip do mesmo tamanho exato do
        // wrapper — um clip maior do que o nó faz o StackPane recalcular bounds
        // de forma imprevisível e desalinha todo o card).
        Rectangle clip = new Rectangle(CARD_LARGURA, CARD_FOTO_ALTURA);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        fotoWrapper.setClip(clip);

        List<byte[]> fotos = veiculo.getFotos();
        if (fotos != null && !fotos.isEmpty() && fotos.get(0) != null) {
            ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(fotos.get(0))));
            imageView.setFitWidth(CARD_LARGURA);
            imageView.setFitHeight(CARD_FOTO_ALTURA);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            fotoWrapper.getChildren().add(imageView);
        } else {
            Label semFoto = new Label("Sem fotografia");
            semFoto.getStyleClass().add("veiculo-card-sem-foto");
            fotoWrapper.getChildren().add(semFoto);
        }

        // ---- Badge de preço sobreposto no canto superior direito da foto ----
        HBox badgePreco = new HBox();
        badgePreco.getStyleClass().add("veiculo-card-badge-preco");
        Label precoTexto = new Label(String.format("%.0f €/dia", veiculo.getPrecoDiario()));
        precoTexto.getStyleClass().add("veiculo-card-badge-preco-texto");
        badgePreco.getChildren().add(precoTexto);
        StackPane.setAlignment(badgePreco, Pos.TOP_RIGHT);
        StackPane.setMargin(badgePreco, new Insets(12, 12, 0, 0));
        fotoWrapper.getChildren().add(badgePreco);

        // ---- Marca + Modelo (título editorial) ----
        Label marcaModelo = new Label(veiculo.getMarca() + " " + veiculo.getModelo());
        marcaModelo.getStyleClass().add("veiculo-card-marca-modelo");
        marcaModelo.setWrapText(true);

        // ---- Linha de info: ano · transmissão ----
        Label info = new Label(veiculo.getAno() + " · " + veiculo.getTransmissao());
        info.getStyleClass().add("veiculo-card-info");

        // ---- Localização + avaliação, na mesma linha ----
        Label localizacao = new Label("📍 " + veiculo.getLocalizacao());
        localizacao.getStyleClass().add("veiculo-card-localizacao");

        double media = veiculo.getAvaliacaoMedia();
        Label avaliacao = new Label(media < 0 ? "Sem avaliações" : String.format("★ %.1f", media));
        avaliacao.getStyleClass().add("veiculo-card-avaliacao");

        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);
        HBox linhaInferior = new HBox(localizacao, espacador, avaliacao);
        linhaInferior.setAlignment(Pos.CENTER_LEFT);

        VBox textoBox = new VBox(6, marcaModelo, info, linhaInferior);
        textoBox.setPadding(new Insets(14, 16, 16, 16));

        card.getChildren().addAll(fotoWrapper, textoBox);
        card.setOnMouseClicked(e -> abrirDetalhe(veiculo));

        return card;
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