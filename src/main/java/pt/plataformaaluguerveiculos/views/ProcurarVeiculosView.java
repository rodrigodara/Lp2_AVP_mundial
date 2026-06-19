package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aluguer.controller.ReservaService;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProcurarVeiculosView {

    private VBox root;
    private TableView<Veiculo> tabela;

    // Mapeamento label → preço máximo (null = sem limite)
    private static final Map<String, Double> RANGES_PRECO = new LinkedHashMap<>();
    static {
        RANGES_PRECO.put("Até 30€/dia",   30.0);
        RANGES_PRECO.put("Até 60€/dia",   60.0);
        RANGES_PRECO.put("Até 100€/dia", 100.0);
        RANGES_PRECO.put("Mais de 100€",  null); // tratado à parte
    }

    public ProcurarVeiculosView() {

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Procurar Veículos");
        titulo.getStyleClass().add("dashboard-titulo");

        // ============================
        // BARRA DE PESQUISA
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

        // Pesquisa ao pressionar Enter
        campoPesquisa.setOnAction(e -> btnPesquisar.fire());

        btnLimparPesquisa.setOnAction(e -> {
            campoPesquisa.clear();
            carregarVeiculos();
        });

        // ============================
        // FILTROS
        // ============================
        ComboBox<String> comboMarca = new ComboBox<>();
        comboMarca.setPromptText("Todas as marcas");
        comboMarca.setPrefWidth(180);

        ComboBox<String> comboPreco = new ComboBox<>();
        comboPreco.setPromptText("Qualquer preço");
        comboPreco.setPrefWidth(160);
        comboPreco.setItems(FXCollections.observableArrayList(RANGES_PRECO.keySet()));

        ComboBox<String> comboLocalizacao = new ComboBox<>();
        comboLocalizacao.setPromptText("Todas as localizações");
        comboLocalizacao.setPrefWidth(200);

        Button btnLimpar = new Button("Limpar filtros");
        btnLimpar.getStyleClass().add("btn-secundario");

        HBox filtroBox = new HBox(12,
            new Label("Marca:"), comboMarca,
            new Label("Preço:"), comboPreco,
            new Label("Localização:"), comboLocalizacao,
            btnLimpar
        );
        filtroBox.setAlignment(Pos.CENTER_LEFT);

        carregarMarcas(comboMarca);
        carregarLocalizacoes(comboLocalizacao);

        // Qualquer alteração num filtro dispara a pesquisa combinada
        comboMarca.setOnAction(e -> aplicarFiltros(comboMarca, comboPreco, comboLocalizacao));
        comboPreco.setOnAction(e -> aplicarFiltros(comboMarca, comboPreco, comboLocalizacao));
        comboLocalizacao.setOnAction(e -> aplicarFiltros(comboMarca, comboPreco, comboLocalizacao));

        btnLimpar.setOnAction(e -> {
            comboMarca.setValue(null);
            comboPreco.setValue(null);
            comboLocalizacao.setValue(null);
            carregarVeiculos();
        });

        // ============================
        // TABELA
        // ============================
        tabela = new TableView<>();
        tabela.setPrefHeight(600);
        VBox.setVgrow(tabela, javafx.scene.layout.Priority.ALWAYS);

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

        // TableColumn<Veiculo, String> colEstado = new TableColumn<>("Estado");
        // colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tabela.getColumns().addAll(
            colMarca, colModelo, colAno, colCombustivel, colPreco, colLocalizacao /*, colEstado*/
        );

        // ============================
        // BOTÃO RESERVAR
        // ============================
        Button btnReservar = new Button("Reservar Veículo Selecionado");
        btnReservar.getStyleClass().add("btn-primario");
        btnReservar.setDisable(true);

        tabela.getSelectionModel().selectedItemProperty().addListener(
            (obs, antigo, novo) -> btnReservar.setDisable(novo == null)
        );

        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirReserva(tabela.getSelectionModel().getSelectedItem());
            }
        });

        btnReservar.setOnAction(e ->
            abrirReserva(tabela.getSelectionModel().getSelectedItem())
        );

        carregarVeiculos();

        root.getChildren().addAll(titulo, pesquisaBox, filtroBox, tabela, btnReservar);
    }

    private void aplicarFiltros(ComboBox<String> comboMarca,
                                ComboBox<String> comboPreco,
                                ComboBox<String> comboLocalizacao) {
        String marca       = comboMarca.getValue();
        String localizacao = comboLocalizacao.getValue();
        String precoLabel  = comboPreco.getValue();

        Double precoMax = null;
        Double precoMin = null;
        if (precoLabel != null) {
            if (precoLabel.equals("Mais de 100€")) {
                precoMin = 100.0; // filtrado em memória abaixo
            } else {
                precoMax = RANGES_PRECO.get(precoLabel);
            }
        }

        try {
            VeiculoService service = new VeiculoService();
            List<Veiculo> lista = service.getVehiclesComFiltros(marca, precoMax, localizacao);

            // "Mais de 100€" não tem suporte direto na query → filtra em memória
            if (precoMin != null) {
                final double min = precoMin;
                lista = lista.stream()
                    .filter(v -> v.getPrecoDiario() > min)
                    .toList();
            }

            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirReserva(Veiculo veiculo) {
        if (veiculo == null) return;

        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            NavigationManager.getInstance().navegarParaLogin();
            return;
        }

        try {
            Connection conn = DatabaseConnection.getConnection();
            ReservaService service = new ReservaService(conn);

            double[] combInfo = estimarDadosCombustivel(veiculo.getCombustivel());

            CriarReservaView view = new CriarReservaView(
                service,
                user.getId(),
                veiculo.getId(),
                veiculo.getPrecoDiario(),
                combInfo[0],   // consumo estimado (L/100km ou kWh/100km)
                combInfo[1],   // preço do combustível (€/L ou €/kWh)
                0,             // kmDiaMedia=0 → usa DEFAULT_KM_DIA (200 km/dia)
                user.getSaldo().doubleValue(),
                veiculo.getMarca() + " " + veiculo.getModelo() + " (" + veiculo.getAno() + ")"
            );

            NavigationManager.getInstance().navegarParaCriarReserva(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Devolve [consumo, precoCombustivel] com base no tipo de combustível.
     * Valores típicos médios para estimativa de custo na preview de reserva.
     */
    private double[] estimarDadosCombustivel(String tipo) {
        if (tipo == null) return new double[]{7.0, 1.75};
        switch (tipo.toLowerCase().trim()) {
            case "diesel":            return new double[]{6.0, 1.60};
            case "elétrico":
            case "eletrico":          return new double[]{18.0, 0.25};
            case "híbrido":
            case "hibrido":
            case "híbrido plug-in":
            case "hibrido plug-in":   return new double[]{4.5, 1.75};
            default:                  return new double[]{7.0, 1.75}; // gasolina
        }
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

    private void carregarLocalizacoes(ComboBox<String> combo) {
        try {
            VeiculoService service = new VeiculoService();
            combo.setItems(FXCollections.observableArrayList(service.getLocalizacoes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VBox getRoot() {
        return root;
    }
}
