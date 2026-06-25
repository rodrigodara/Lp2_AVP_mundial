package pt.plataformaaluguerveiculos.views;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aluguer.model.Veiculo;
import com.aluguer.model.VeiculoMetricas;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class MeusVeiculosView {

    private final VBox root;
    private final TableView<Veiculo> tabela;
    private final VeiculoService veiculoService;

    // Métricas (reservas/receita) por id de veículo — carregadas uma vez
    // junto com a lista de veículos e consultadas pelas colunas extra
    // e pelos cards de resumo.
    private Map<Integer, VeiculoMetricas> metricasPorVeiculo = new HashMap<>();

    private final HBox cardsResumo;

    public MeusVeiculosView() {
        veiculoService = new VeiculoService();

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Os Meus Veículos");
        titulo.getStyleClass().add("dashboard-titulo");

        Button btnConsultarReceita = new Button("💰  Consultar Receita por Veículo");
        btnConsultarReceita.getStyleClass().add("btn-secundario");
        btnConsultarReceita.setOnAction(e ->
            NavigationManager.getInstance().navegarParaConsultaReceita()
        );

        HBox linhaTitulo = new HBox(titulo, criarEspacadorMeusVeiculos(), btnConsultarReceita);
        linhaTitulo.setAlignment(Pos.CENTER_LEFT);
        linhaTitulo.setMaxWidth(Double.MAX_VALUE);

        cardsResumo = new HBox(16);
        cardsResumo.setAlignment(Pos.CENTER_LEFT);

        tabela = new TableView<>();
        tabela.setPrefHeight(450);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Veiculo, String> colMatricula = new TableColumn<>("Matrícula");
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));

        TableColumn<Veiculo, String> colMarca = new TableColumn<>("Marca");
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));

        TableColumn<Veiculo, String> colModelo = new TableColumn<>("Modelo");
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));

        TableColumn<Veiculo, Integer> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("ano"));

        TableColumn<Veiculo, String> colCombustivel = new TableColumn<>("Combustível");
        colCombustivel.setCellValueFactory(new PropertyValueFactory<>("combustivel"));

        TableColumn<Veiculo, Double> colPreco = new TableColumn<>("Preço/Dia (€)");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoDiario"));

        TableColumn<Veiculo, String> colLocalizacao = new TableColumn<>("Localização");
        colLocalizacao.setCellValueFactory(new PropertyValueFactory<>("localizacao"));

        TableColumn<Veiculo, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // ---- Colunas de métricas (não vêm do bean Veiculo, vêm do mapa) ----
        TableColumn<Veiculo, Integer> colReservas = new TableColumn<>("Reservas");
        colReservas.setCellValueFactory(data -> {
            VeiculoMetricas m = metricasPorVeiculo.get(data.getValue().getId());
            return new javafx.beans.property.SimpleIntegerProperty(
                m != null ? m.getTotalReservas() : 0
            ).asObject();
        });
        colReservas.setStyle("-fx-alignment: CENTER;");

        TableColumn<Veiculo, String> colReceita = new TableColumn<>("Receita Gerada");
        colReceita.setCellValueFactory(data -> {
            VeiculoMetricas m = metricasPorVeiculo.get(data.getValue().getId());
            double receita = m != null ? m.getReceitaTotal() : 0;
            return new javafx.beans.property.SimpleStringProperty(String.format("%.2f €", receita));
        });
        colReceita.setCellFactory(criarCelulaReceitaDestacada());
        colReceita.setStyle("-fx-alignment: CENTER-RIGHT;");

        tabela.getColumns().addAll(
            colMatricula, colMarca, colModelo, colAno, colCombustivel,
            colPreco, colLocalizacao, colEstado, colReservas, colReceita
        );

        Button btnAdicionar = new Button("+ Adicionar Veículo");
        btnAdicionar.getStyleClass().add("btn-primario");
        btnAdicionar.setOnAction(e ->
            NavigationManager.getInstance().navegarParaAdicionarVeiculo()
        );

        Button btnDetalhes = new Button("Ver Detalhes");
        btnDetalhes.getStyleClass().add("btn-primario");
        btnDetalhes.setDisable(true);

        Button btnRemover = new Button("Remover Veículo");
        btnRemover.getStyleClass().add("btn-perigo");
        btnRemover.setDisable(true);

        Button btnIndisponibilidade = new Button("📅  Gerir Indisponibilidade");
        btnIndisponibilidade.getStyleClass().add("btn-secundario");
        btnIndisponibilidade.setDisable(true);
        btnIndisponibilidade.setOnAction(e -> {
            Veiculo selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                NavigationManager.getInstance().navegarParaIndisponibilidade(selecionado.getId());
            }
        });

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            boolean semSelecao = (novo == null);
            btnDetalhes.setDisable(semSelecao);
            btnRemover.setDisable(semSelecao);
            btnIndisponibilidade.setDisable(semSelecao);
        });

        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirDetalhes(tabela.getSelectionModel().getSelectedItem());
            }
        });

        btnDetalhes.setOnAction(e ->
            abrirDetalhes(tabela.getSelectionModel().getSelectedItem())
        );

        btnRemover.setOnAction(e ->
            removerVeiculo(tabela.getSelectionModel().getSelectedItem())
        );

        HBox acoes = new HBox(15, btnAdicionar, btnDetalhes, btnIndisponibilidade, btnRemover);
        acoes.setAlignment(Pos.CENTER);

        carregarMeusVeiculos();

        root.getChildren().addAll(linhaTitulo, cardsResumo, tabela, acoes);
    }

    private Region criarEspacadorMeusVeiculos() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private void carregarMeusVeiculos() {
        int userId = SessionManager.getInstance().getUtilizador().getId();
        try {
            List<Veiculo> lista = veiculoService.listarPorProprietario(userId);
            ObservableList<Veiculo> obs = FXCollections.observableArrayList(lista);
            tabela.setItems(obs);
            if (lista.isEmpty()) {
                tabela.setPlaceholder(new Label("Ainda não tens veículos registados."));
            }

            carregarMetricas(userId);
            tabela.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Carrega as métricas (reservas/receita) e atualiza os cards de resumo. */
    private void carregarMetricas(int userId) {
        try {
            List<VeiculoMetricas> metricas = veiculoService.obterMetricasPorProprietario(userId);

            metricasPorVeiculo = new HashMap<>();
            int totalReservas = 0;
            double receitaTotal = 0;
            VeiculoMetricas maisRentavel = null;

            for (VeiculoMetricas m : metricas) {
                metricasPorVeiculo.put(m.getVeiculoId(), m);
                totalReservas += m.getTotalReservas();
                receitaTotal += m.getReceitaTotal();
                if (maisRentavel == null || m.getReceitaTotal() > maisRentavel.getReceitaTotal()) {
                    maisRentavel = m;
                }
            }

            atualizarCardsResumo(metricas.size(), totalReservas, receitaTotal, maisRentavel);
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Em caso de erro, esconde os cards em vez de mostrar dados errados.
            cardsResumo.getChildren().clear();
        }
    }

    private void atualizarCardsResumo(int totalVeiculos, int totalReservas,
                                       double receitaTotal, VeiculoMetricas maisRentavel) {
        String descMaisRentavel = (maisRentavel != null && maisRentavel.getReceitaTotal() > 0)
            ? maisRentavel.getMarca() + " " + maisRentavel.getModelo()
            : "—";

        cardsResumo.getChildren().setAll(
            criarCard("🚗 Veículos", String.valueOf(totalVeiculos), "#e3f2fd", "#1565c0"),
            criarCard("📋 Total de Reservas", String.valueOf(totalReservas), "#fff8e1", "#f57f17"),
            criarCard("💰 Receita Total", String.format("%.2f €", receitaTotal), "#e8f5e9", "#2e7d32"),
            criarCard("🏆 Mais Rentável", descMaisRentavel, "#f3e5f5", "#6a1b9a")
        );
    }

    private VBox criarCard(String titulo, String valor, String bg, String cor) {
        Label lv = new Label(valor);
        lv.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");
        lv.setWrapText(true);

        Label lt = new Label(titulo);
        lt.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

        VBox card = new VBox(6, lv, lt);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );
        card.setPrefWidth(190);
        card.setMinHeight(80);

        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Destaca visualmente o valor de receita mais alto da coluna (verde e a negrito), quando > 0. */
    private Callback<TableColumn<Veiculo, String>, TableCell<Veiculo, String>> criarCelulaReceitaDestacada() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vazio) {
                super.updateItem(valor, vazio);
                if (vazio || valor == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(valor);
                boolean temReceita = !valor.startsWith("0,00") && !valor.startsWith("0.00");
                setStyle(temReceita
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;"
                    : "-fx-text-fill: #9e9e9e; -fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    private void abrirDetalhes(Veiculo veiculo) {
        if (veiculo == null) return;
        NavigationManager.getInstance().navegarParaDetalheVeiculo(veiculo);
    }

    private void removerVeiculo(Veiculo veiculo) {
        if (veiculo == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Tens a certeza que queres remover o veículo " +
            veiculo.getMarca() + " " + veiculo.getModelo() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Remoção");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    boolean ok = veiculoService.remover(veiculo.getId());
                    if (ok) {
                        tabela.getItems().remove(veiculo);
                        carregarMetricas(SessionManager.getInstance().getUtilizador().getId());
                    } else {
                        mostrarErro("Não foi possível remover o veículo.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    mostrarErro("Erro ao remover veículo: " + ex.getMessage());
                }
            }
        });
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}