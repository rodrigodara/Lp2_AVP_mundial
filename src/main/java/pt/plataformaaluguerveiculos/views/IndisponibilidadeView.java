package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.aluguer.dao.AvailabilityDAO;
import com.aluguer.model.Availability;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * ALV-171 — Criar calendário de indisponibilidade
 * ALV-172 — Criar endpoint adicionar indisponibilidade
 * ALV-173 — Criar endpoint remover indisponibilidade
 *
 * Página que o proprietário usa para gerir os períodos em que
 * os seus veículos não estão disponíveis para aluguer.
 */
public class IndisponibilidadeView {

    private final VBox root;
    private final int veiculoId;
    private VBox listaIndisponibilidades;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public IndisponibilidadeView(int veiculoId) {
        this.veiculoId = veiculoId;

        root = new VBox(20);
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
        Button btnVoltar = new Button("←  Voltar aos Meus Veículos");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaMeusVeiculos());

        Label titulo = new Label("Gestão de Indisponibilidade");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Defina os períodos em que " + nomeVeiculoAtual() + " não está disponível");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        // Formulário para adicionar
        VBox formulario = criarFormulario();

        // Lista de indisponibilidades
        listaIndisponibilidades = new VBox(10);
        carregarLista();

        ScrollPane scroll = new ScrollPane(listaIndisponibilidades);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("reservas-scroll");
        scroll.setPrefHeight(350);

        root.getChildren().addAll(btnVoltar, titulo, subtitulo, formulario, scroll);
    }

    // ----------------------------------------------------------------
    // ALV-172 — Formulário para adicionar indisponibilidade
    // ----------------------------------------------------------------

    private VBox criarFormulario() {
        VBox card = new VBox(12);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(20));

        Label lblTitulo = new Label("Adicionar Indisponibilidade");
        lblTitulo.getStyleClass().add("reserva-card-id");

        HBox campos = new HBox(12);
        campos.setAlignment(Pos.CENTER_LEFT);

        DatePicker dpInicio = new DatePicker();
        dpInicio.setPromptText("Data início");

        DatePicker dpFim = new DatePicker();
        dpFim.setPromptText("Data fim");

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo (opcional)");
        HBox.setHgrow(txtMotivo, Priority.ALWAYS);

        Button btnAdicionar = new Button("Adicionar");
        btnAdicionar.getStyleClass().add("btn-aceitar");

        btnAdicionar.setOnAction(e -> {
            LocalDate inicio = dpInicio.getValue();
            LocalDate fim    = dpFim.getValue();

            if (inicio == null || fim == null) {
                mostrarErro("Selecione as datas de início e fim.");
                return;
            }
            if (!fim.isAfter(inicio)) {
                mostrarErro("A data de fim tem de ser posterior à data de início.");
                return;
            }
            if (inicio.isBefore(LocalDate.now())) {
                mostrarErro("A data de início não pode ser no passado.");
                return;
            }

            Availability a = new Availability(
                veiculoId, inicio, fim, txtMotivo.getText().trim()
            );

            try (Connection conn = DatabaseConnection.getConnection()) {
                boolean ok = new AvailabilityDAO(conn).adicionar(a);
                if (ok) {
                    dpInicio.setValue(null);
                    dpFim.setValue(null);
                    txtMotivo.clear();
                    carregarLista();
                } else {
                    mostrarErro("Erro ao adicionar indisponibilidade.");
                }
            } catch (SQLException ex) {
                mostrarErro("Erro de base de dados: " + ex.getMessage());
            }
        });

        campos.getChildren().addAll(dpInicio, dpFim, txtMotivo, btnAdicionar);
        card.getChildren().addAll(lblTitulo, campos);
        return card;
    }

    // ----------------------------------------------------------------
    // ALV-173 — Carregar e mostrar lista com botão remover
    // ----------------------------------------------------------------

    private void carregarLista() {
        listaIndisponibilidades.getChildren().clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Availability> lista = new AvailabilityDAO(conn).listarPorVeiculo(veiculoId);

            if (lista.isEmpty()) {
                Label vazio = new Label("Sem períodos de indisponibilidade registados.");
                vazio.getStyleClass().add("reservas-vazio");
                vazio.setPadding(new Insets(20));
                listaIndisponibilidades.getChildren().add(vazio);
                return;
            }

            for (Availability a : lista) {
                listaIndisponibilidades.getChildren().add(criarCardIndisponibilidade(a));
            }

        } catch (SQLException e) {
            mostrarErro("Erro ao carregar indisponibilidades: " + e.getMessage());
        }
    }

    private HBox criarCardIndisponibilidade(Availability a) {
        HBox card = new HBox(12);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);

        Label lblDatas = new Label(a.getDataInicio() + " → " + a.getDataFim()
                + "  (" + a.getNumeroDias() + " dias)");
        lblDatas.getStyleClass().add("reserva-card-detalhe");

        Label lblMotivo = new Label(
            a.getMotivo() != null && !a.getMotivo().isEmpty() ? a.getMotivo() : "Sem motivo"
        );
        lblMotivo.getStyleClass().add("reserva-card-detalhe");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRemover = new Button("✘  Remover");
        btnRemover.getStyleClass().add("btn-rejeitar");

        btnRemover.setOnAction(e -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                boolean ok = new AvailabilityDAO(conn).remover(a.getId());
                if (ok) {
                    carregarLista();
                } else {
                    mostrarErro("Erro ao remover indisponibilidade.");
                }
            } catch (SQLException ex) {
                mostrarErro("Erro de base de dados: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(lblDatas, lblMotivo, spacer, btnRemover);
        return card;
    }

    // ----------------------------------------------------------------
    // Auxiliar
    // ----------------------------------------------------------------

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private String nomeVeiculoCache;

    private String nomeVeiculoAtual() {
        if (nomeVeiculoCache != null) return nomeVeiculoCache;
        try {
            com.aluguer.model.Veiculo v = new com.aluguer.dao.VeiculoDAO().buscarPorId(veiculoId);
            nomeVeiculoCache = (v != null) ? (v.getMarca() + " " + v.getModelo()) : "o veículo";
        } catch (Exception ex) {
            nomeVeiculoCache = "o veículo";
        }
        return nomeVeiculoCache;
    }

    public VBox getRoot() {
        return root;
    }
}