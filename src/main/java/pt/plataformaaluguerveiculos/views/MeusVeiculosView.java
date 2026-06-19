package pt.plataformaaluguerveiculos.views;

import java.sql.SQLException;
import java.util.List;

import com.aluguer.model.Veiculo;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MeusVeiculosView {

    private final VBox root;
    private final TableView<Veiculo> tabela;
    private final VeiculoService veiculoService;

    public MeusVeiculosView() {
        veiculoService = new VeiculoService();

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Os Meus Veículos");
        titulo.getStyleClass().add("dashboard-titulo");

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

        tabela.getColumns().addAll(
            colMatricula, colMarca, colModelo, colAno, colCombustivel, colPreco, colLocalizacao, colEstado
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

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            boolean semSelecao = (novo == null);
            btnDetalhes.setDisable(semSelecao);
            btnRemover.setDisable(semSelecao);
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

        HBox acoes = new HBox(15, btnAdicionar, btnDetalhes, btnRemover);
        acoes.setAlignment(Pos.CENTER);

        carregarMeusVeiculos();

        root.getChildren().addAll(titulo, tabela, acoes);
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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