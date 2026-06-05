package pt.plataformaaluguerveiculos.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.List;

import com.aluguer.model.Veiculo;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.DatabaseConnection;

public class ProcurarVeiculosView {

    private VBox root;
    private TableView<Veiculo> tabela;

    public ProcurarVeiculosView() {

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Procurar Veículos");
        titulo.getStyleClass().add("dashboard-titulo");

        tabela = new TableView<>();
        tabela.setPrefHeight(500);

        // ============================
        // COLUNAS DA TABELA
        // ============================
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

TableColumn<Veiculo, String> colEstado = new TableColumn<>("Estado");
colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

tabela.getColumns().addAll(
    colMarca, colModelo, colAno, colCombustivel, colPreco, colLocalizacao, colEstado
);


        // ============================
        // CARREGAR DADOS DA BD
        // ============================
        carregarVeiculos();

        root.getChildren().addAll(titulo, tabela);
    }

    private void carregarVeiculos() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            VeiculoService service = new VeiculoService(conn);

            List<Veiculo> lista = service.getAllVehicles();
            ObservableList<Veiculo> obs = FXCollections.observableArrayList(lista);

            tabela.setItems(obs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VBox getRoot() {
        return root;
    }
}
