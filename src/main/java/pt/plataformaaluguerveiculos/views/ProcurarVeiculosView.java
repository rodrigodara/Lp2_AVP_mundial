package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aluguer.model.Veiculo;
import com.aluguer.service.FavoritoService;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ProcurarVeiculosView {

    private static final double LARGURA_CARD = 230;
    private static final double ALTURA_FOTO = 150;
    private static final int TAMANHO_PAGINA = 10;

    private VBox root;
    private FlowPane gridCards;
    private ScrollPane scrollCards;

    // Paginação
    private List<Veiculo> listaCompleta = java.util.Collections.emptyList();
    private int paginaAtual = 0;
    private HBox paginacaoBox;
    private Label lblPaginacao;
    private Button btnPaginaAnterior;
    private Button btnPaginaSeguinte;

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

    // Ordenação
    private ComboBox<String> comboOrdenar;
    private static final String ORD_RELEVANCIA   = "Relevância";
    private static final String ORD_PRECO_ASC    = "Preço: mais baratos primeiro";
    private static final String ORD_PRECO_DESC   = "Preço: mais caros primeiro";
    private static final String ORD_AVALIACAO    = "Melhor avaliados";

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

        Button btnToggleFiltros = new Button("Filtros ▾");
        btnToggleFiltros.getStyleClass().add("btn-secundario");

        comboOrdenar = new ComboBox<>();
        comboOrdenar.getStyleClass().add("combo-filtro");
        comboOrdenar.setPromptText("Ordenar por...");
        comboOrdenar.setItems(FXCollections.observableArrayList(
            ORD_RELEVANCIA, ORD_PRECO_ASC, ORD_PRECO_DESC, ORD_AVALIACAO
        ));
        comboOrdenar.setValue(ORD_RELEVANCIA);
        comboOrdenar.setPrefWidth(220);
        comboOrdenar.setOnAction(e -> {
            // Reaplica a ordenação sobre a lista já carregada, sem repetir
            // a pesquisa/filtros — só muda a ordem de apresentação.
            ordenarListaCompleta();
            paginaAtual = 0;
            renderizarPaginaAtual();
        });

        HBox pesquisaBox = new HBox(8, campoPesquisa, btnPesquisar, btnLimparPesquisa, btnToggleFiltros, comboOrdenar);
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

        // ---- Começa escondido para a lista de veículos aparecer logo no topo ----
        filtrosBox.setVisible(false);
        filtrosBox.setManaged(false);

        btnToggleFiltros.setOnAction(e -> {
            boolean aVisivel = !filtrosBox.isVisible();
            filtrosBox.setVisible(aVisivel);
            filtrosBox.setManaged(aVisivel);
            btnToggleFiltros.setText(aVisivel ? "Filtros ▲" : "Filtros ▾");
        });

        btnAplicarFiltros.setOnAction(e -> aplicarFiltros());
        btnLimparFiltros.setOnAction(e -> {
            limparFiltros();
            carregarVeiculos();
        });

        // ============================
        // GRELHA DE CARDS DE VEÍCULOS
        // ============================
        gridCards = new FlowPane();
        gridCards.setHgap(18);
        gridCards.setVgap(18);
        gridCards.setPadding(new Insets(4));
        gridCards.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(gridCards);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(560);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        this.scrollCards = scroll;

        // ============================
        // CONTROLOS DE PAGINAÇÃO
        // ============================
        btnPaginaAnterior = new Button("‹ Anterior");
        btnPaginaAnterior.getStyleClass().add("btn-secundario");
        btnPaginaAnterior.setOnAction(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                renderizarPaginaAtual();
                scrollCards.setVvalue(0);
            }
        });

        btnPaginaSeguinte = new Button("Seguinte ›");
        btnPaginaSeguinte.getStyleClass().add("btn-secundario");
        btnPaginaSeguinte.setOnAction(e -> {
            int totalPaginas = totalPaginas();
            if (paginaAtual < totalPaginas - 1) {
                paginaAtual++;
                renderizarPaginaAtual();
                scrollCards.setVvalue(0);
            }
        });

        lblPaginacao = new Label();
        lblPaginacao.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        paginacaoBox = new HBox(14, btnPaginaAnterior, lblPaginacao, btnPaginaSeguinte);
        paginacaoBox.setAlignment(Pos.CENTER);
        paginacaoBox.setPadding(new Insets(6, 0, 0, 0));

        carregarVeiculos();

        root.getChildren().addAll(titulo, pesquisaBox, filtrosBox, scrollCards, paginacaoBox);
    }

    // ============================
    // CONSTRUÇÃO DE UM CARD DE VEÍCULO
    // ============================
    private VBox criarCardVeiculo(Veiculo v) {
        // ---- Foto (ou placeholder) ----
        StackPane fotoBox = new StackPane();
        fotoBox.setPrefSize(LARGURA_CARD, ALTURA_FOTO);
        fotoBox.setMinSize(LARGURA_CARD, ALTURA_FOTO);
        fotoBox.setMaxSize(LARGURA_CARD, ALTURA_FOTO);

        byte[] foto = v.getFoto1();
        if (foto != null && foto.length > 0) {
            ImageView imageView = new ImageView();
            imageView.setFitWidth(LARGURA_CARD);
            imageView.setFitHeight(ALTURA_FOTO);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            try {
                imageView.setImage(new Image(new ByteArrayInputStream(foto)));
            } catch (Exception ex) {
                imageView.setImage(null);
            }
            fotoBox.getChildren().add(imageView);
        } else {
            fotoBox.setStyle("-fx-background-color: #e8eaf6;");
            Label semFoto = new Label("Sem foto disponível");
            semFoto.setStyle("-fx-text-fill: #9fa8da; -fx-font-size: 12px;");
            fotoBox.getChildren().add(semFoto);
        }

        // ---- Bloco de informação ----
        Label lblNome = new Label(v.getMarca() + " " + v.getModelo());
        lblNome.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
        lblNome.setWrapText(true);

        Label lblAno = new Label(String.valueOf(v.getAno()) + " · " + v.getCombustivel());
        lblAno.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        Label lblLocal = new Label("📍 " + v.getLocalizacao());
        lblLocal.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        double media = v.getAvaliacaoMedia();
        String textoAval = media < 0 ? " Sem avaliações" : String.format(" %.1f ★", media);
        Label lblAval = new Label(textoAval);
        lblAval.setStyle("-fx-font-size: 12px; -fx-text-fill: #f9a825; -fx-font-weight: bold;");

        Label lblPreco = new Label(String.format("%.0f €/dia", v.getPrecoDiario()));
        lblPreco.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        HBox linhaPrecoAval = new HBox(lblPreco);
        linhaPrecoAval.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblPreco, Priority.ALWAYS);
        linhaPrecoAval.getChildren().add(lblAval);

        Button btnVerDetalhes = new Button("Ver Detalhes");
        btnVerDetalhes.getStyleClass().add("btn-primario");
        btnVerDetalhes.setMaxWidth(Double.MAX_VALUE);
        btnVerDetalhes.setOnAction(e -> abrirDetalhe(v));

        // ---- Botão de favorito ----
        int utilizadorId = SessionManager.getInstance().getUtilizador().getId();
        Button btnFavorito = new Button("☆ Guardar");
        btnFavorito.getStyleClass().add("btn-secundario");
        btnFavorito.setMaxWidth(Double.MAX_VALUE);
        try {
            boolean jaFavorito = new FavoritoService().isFavorito(utilizadorId, v.getId());
            btnFavorito.setText(jaFavorito ? "★ Guardado" : "☆ Guardar");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        btnFavorito.setOnAction(e -> {
            e.consume(); // não abrir o detalhe ao clicar neste botão
            try {
                boolean novoEstado = new FavoritoService().alternar(utilizadorId, v.getId());
                btnFavorito.setText(novoEstado ? "★ Guardado" : "☆ Guardar");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        VBox infoBox = new VBox(6, lblNome, lblAno, lblLocal, linhaPrecoAval, btnVerDetalhes, btnFavorito);
        infoBox.setPadding(new Insets(12));

        // ---- Conteúdo do card (foto + info), com cantos arredondados ----
        VBox conteudo = new VBox(fotoBox, infoBox);
        conteudo.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(conteudo.widthProperty());
        clip.heightProperty().bind(conteudo.heightProperty());
        conteudo.setClip(clip);

        // ---- Wrapper externo: só aqui vai a sombra (fora do clip) ----
        VBox card = new VBox(conteudo);
        card.setPrefWidth(LARGURA_CARD);
        card.setMaxWidth(LARGURA_CARD);
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> abrirDetalhe(v));

        return card;
    }

    /** Recebe a lista completa (de uma pesquisa/filtro) e mostra a partir da página 1. */
    private void atualizarCards(List<Veiculo> lista) {
        this.listaCompleta = (lista != null) ? new java.util.ArrayList<>(lista) : new java.util.ArrayList<>();
        ordenarListaCompleta();
        this.paginaAtual = 0;
        renderizarPaginaAtual();
    }

    /** Ordena listaCompleta de acordo com a opção escolhida em comboOrdenar (em memória, sem nova query). */
    private void ordenarListaCompleta() {
        if (comboOrdenar == null || listaCompleta.isEmpty()) return;
        String ordem = comboOrdenar.getValue();
        if (ordem == null) ordem = ORD_RELEVANCIA;

        switch (ordem) {
            case ORD_PRECO_ASC ->
                listaCompleta.sort(java.util.Comparator.comparingDouble(Veiculo::getPrecoDiario));
            case ORD_PRECO_DESC ->
                listaCompleta.sort(java.util.Comparator.comparingDouble(Veiculo::getPrecoDiario).reversed());
            case ORD_AVALIACAO ->
                // Sem avaliações (média < 0) fica sempre no fim, independentemente da ordenação.
                listaCompleta.sort((a, b) -> {
                    double mA = a.getAvaliacaoMedia();
                    double mB = b.getAvaliacaoMedia();
                    boolean semA = mA < 0, semB = mB < 0;
                    if (semA && semB) return 0;
                    if (semA) return 1;
                    if (semB) return -1;
                    return Double.compare(mB, mA); // descendente: melhor avaliados primeiro
                });
            default -> { /* ORD_RELEVANCIA — mantém a ordem original devolvida pela pesquisa/filtro */ }
        }
    }

    private int totalPaginas() {
        if (listaCompleta.isEmpty()) return 1;
        return (int) Math.ceil(listaCompleta.size() / (double) TAMANHO_PAGINA);
    }

    /** Constrói os cards apenas para a página atual e atualiza os controlos de paginação. */
    private void renderizarPaginaAtual() {
        gridCards.getChildren().clear();

        if (listaCompleta.isEmpty()) {
            Label vazio = new Label("Nenhum veículo encontrado com os critérios indicados.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            vazio.setPadding(new Insets(30));
            gridCards.getChildren().add(vazio);

            paginacaoBox.setVisible(false);
            paginacaoBox.setManaged(false);
            return;
        }

        int totalPaginas = totalPaginas();
        if (paginaAtual >= totalPaginas) paginaAtual = totalPaginas - 1;
        if (paginaAtual < 0) paginaAtual = 0;

        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, listaCompleta.size());

        for (Veiculo v : listaCompleta.subList(inicio, fim)) {
            gridCards.getChildren().add(criarCardVeiculo(v));
        }

        lblPaginacao.setText(
            "Página " + (paginaAtual + 1) + " de " + totalPaginas +
            "  ·  " + listaCompleta.size() + " veículos"
        );
        btnPaginaAnterior.setDisable(paginaAtual == 0);
        btnPaginaSeguinte.setDisable(paginaAtual >= totalPaginas - 1);

        boolean mostrarPaginacao = totalPaginas > 1;
        paginacaoBox.setVisible(mostrarPaginacao);
        paginacaoBox.setManaged(mostrarPaginacao);
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

        // ---- Estilo: tirar o visual "default" do JavaFX dos combos e campos ----
        for (ComboBox<?> combo : new ComboBox<?>[]{
                comboMarca, comboModelo, comboPrecoPreset, comboLocalizacao,
                comboTipoVeiculo, comboCombustivel, comboLugares, comboTransmissao,
                comboAvaliacaoMin, comboAvaliacaoMax}) {
            combo.getStyleClass().add("combo-filtro");
        }
        campoPrecoMin.getStyleClass().add("campo-texto");
        campoPrecoMax.getStyleClass().add("campo-texto");

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
            atualizarCards(lista);
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
            atualizarCards(lista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarVeiculos() {
        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.getAllVehicles();
            atualizarCards(lista);
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