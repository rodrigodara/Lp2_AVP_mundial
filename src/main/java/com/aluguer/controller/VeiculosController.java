package com.aluguer.controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class VeiculosController implements Initializable {

    @FXML private TextField txtMarca;
    @FXML private TextField txtModelo;
    @FXML private TextField txtAno;
    @FXML private TextField txtCombustivel;
    @FXML private TextField txtPreco;
    @FXML private TextField txtLocalizacao;

    @FXML private TableView<Veiculo> tabelaVeiculos;
    @FXML private TableColumn<Veiculo, String> colMarca;
    @FXML private TableColumn<Veiculo, String> colModelo;
    @FXML private TableColumn<Veiculo, Integer> colAno;
    @FXML private TableColumn<Veiculo, String> colCombustivel;
    @FXML private TableColumn<Veiculo, Double> colPreco;
    @FXML private TableColumn<Veiculo, String> colLocalizacao;

    private final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private ObservableList<Veiculo> listaVeiculos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colAno.setCellValueFactory(new PropertyValueFactory<>("ano"));
        colCombustivel.setCellValueFactory(new PropertyValueFactory<>("combustivel"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoDiario"));
        colLocalizacao.setCellValueFactory(new PropertyValueFactory<>("localizacao"));

        carregarVeiculos();
    }

    @FXML
    private void adicionarVeiculo() {
        if (!validarCampos()) return;

        Veiculo v = new Veiculo(
            txtMarca.getText(),
            txtModelo.getText(),
            Integer.parseInt(txtAno.getText()),
            txtCombustivel.getText(),
            Double.parseDouble(txtPreco.getText()),
            txtLocalizacao.getText(),
            1 // TODO: substituir pelo id do utilizador da sessão
        );

        try {
            veiculoDAO.inserir(v);
            carregarVeiculos();
            limparCampos();
        } catch (SQLException e) {
            mostrarErro("Erro ao adicionar veículo: " + e.getMessage());
        }
    }

    @FXML
    private void removerVeiculo() {
        Veiculo selecionado = tabelaVeiculos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarErro("Seleciona um veículo para remover.");
            return;
        }
        try {
            veiculoDAO.apagar(selecionado.getId());
            carregarVeiculos();
        } catch (SQLException e) {
            mostrarErro("Erro ao remover veículo: " + e.getMessage());
        }
    }

    private void carregarVeiculos() {
        try {
            listaVeiculos.setAll(veiculoDAO.listarTodos());
            tabelaVeiculos.setItems(listaVeiculos);
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar veículos: " + e.getMessage());
        }
    }

    private void limparCampos() {
        txtMarca.clear();
        txtModelo.clear();
        txtAno.clear();
        txtCombustivel.clear();
        txtPreco.clear();
        txtLocalizacao.clear();
    }

    private boolean validarCampos() {
        if (txtMarca.getText().isBlank() || txtModelo.getText().isBlank()) {
            mostrarErro("Marca e Modelo são obrigatórios.");
            return false;
        }
        try {
            int ano = Integer.parseInt(txtAno.getText());
            if (ano < 1900 || ano > 2026) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarErro("Ano inválido.");
            return false;
        }
        try {
            double preco = Double.parseDouble(txtPreco.getText());
            if (preco <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarErro("Preço inválido.");
            return false;
        }
        return true;
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
    }
}