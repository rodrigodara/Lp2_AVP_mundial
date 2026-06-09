package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.util.List;

import com.aluguer.controller.ReservaService;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

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
        // BOTÃO RESERVAR
        // ============================
        Button btnReservar = new Button("Reservar Veículo Selecionado");
        btnReservar.getStyleClass().add("btn-primario");
        btnReservar.setDisable(true);

        // Ativa o botão quando há seleção
        tabela.getSelectionModel().selectedItemProperty().addListener(
            (obs, antigo, novo) -> btnReservar.setDisable(novo == null)
        );

        // Duplo clique na linha também abre reserva
        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirReserva(tabela.getSelectionModel().getSelectedItem());
            }
        });

        btnReservar.setOnAction(e ->
            abrirReserva(tabela.getSelectionModel().getSelectedItem())
        );

        // ============================
        // CARREGAR DADOS DA BD
        // ============================
        carregarVeiculos();

        root.getChildren().addAll(titulo, tabela, btnReservar);
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

            CriarReservaView view = new CriarReservaView(
                service,
                user.getId(),
                veiculo.getId(),
                veiculo.getPrecoDiario(),
                0,        // consumo: sem histórico
                0,        // precoCombustivel: sem histórico
                0,        // kmDiaMedia: sem histórico
                user.getSaldo().doubleValue(),
                veiculo.getMarca() + " " + veiculo.getModelo() + " (" + veiculo.getAno() + ")"
            );

            NavigationManager.getInstance().navegarParaCriarReserva(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void carregarVeiculos() {
        try {
            VeiculoService service = new VeiculoService();
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