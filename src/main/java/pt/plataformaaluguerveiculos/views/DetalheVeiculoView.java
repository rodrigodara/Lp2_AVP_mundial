package pt.plataformaaluguerveiculos.views;

import java.util.List;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * ALV-111 — Consulta Detalhada de Veículo.
 * Apresenta todas as especificações técnicas e o histórico de alugueres.
 */
public class DetalheVeiculoView {

    private final VBox root;

    public DetalheVeiculoView(Veiculo veiculo) {
        root = new VBox(20);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(30));

        // ============================
        // CABEÇALHO
        // ============================
        Label titulo = new Label(veiculo.getMarca() + " " + veiculo.getModelo());
        titulo.getStyleClass().add("dashboard-titulo");

        Label subtitulo = new Label("Ano: " + veiculo.getAno());
        subtitulo.getStyleClass().add("label-subtitulo");

        // ============================
        // ESPECIFICAÇÕES TÉCNICAS (ALV-151, 152, 153, 154, 155, 156)
        // ============================
        Label lblEspecs = new Label("Especificações Técnicas");
        lblEspecs.getStyleClass().add("section-titulo");

        GridPane grd = new GridPane();
        grd.setHgap(20);
        grd.setVgap(10);
        grd.setPadding(new Insets(10, 0, 10, 10));

        adicionarCampo(grd, 0, "Marca:",        veiculo.getMarca());
        adicionarCampo(grd, 1, "Modelo:",       veiculo.getModelo());
        adicionarCampo(grd, 2, "Ano:",          String.valueOf(veiculo.getAno()));
        adicionarCampo(grd, 3, "Combustível:",  veiculo.getCombustivel());   // ALV-153
        adicionarCampo(grd, 4, "Transmissão:",  obterTransmissao(veiculo));  // ALV-154
        adicionarCampo(grd, 5, "Lotação:",      obterLotacao(veiculo));      // ALV-155
        adicionarCampo(grd, 6, "Quilometragem:",obterQuilometragem(veiculo));// ALV-156
        adicionarCampo(grd, 7, "Localização:",  veiculo.getLocalizacao());
        adicionarCampo(grd, 8, "Preço/Dia:",   String.format("%.2f €", veiculo.getPrecoDiario()));
        adicionarCampo(grd, 9, "Estado:",       veiculo.getEstado());

        // ============================
        // HISTÓRICO DE ALUGUERES (ALV-157)
        // ============================
        Label lblHistorico = new Label("Histórico de Alugueres");
        lblHistorico.getStyleClass().add("section-titulo");

        TableView<Reserva> tabelaHistorico = new TableView<>();
        tabelaHistorico.setPrefHeight(220);
        tabelaHistorico.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reserva, Integer> colId = new TableColumn<>("#");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50);

        TableColumn<Reserva, String> colInicio = new TableColumn<>("Data Início");
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));

        TableColumn<Reserva, String> colFim = new TableColumn<>("Data Fim");
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));

        TableColumn<Reserva, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<Reserva, Double> colPreco = new TableColumn<>("Total (€)");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoTotal"));

        tabelaHistorico.getColumns().addAll(colId, colInicio, colFim, colEstado, colPreco);

        carregarHistorico(veiculo.getId(), tabelaHistorico);

        // ============================
        // BOTÃO VOLTAR
        // ============================
        Button btnVoltar = new Button("← Voltar");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMeusVeiculos()
        );

        HBox rodape = new HBox(btnVoltar);
        rodape.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
            titulo, subtitulo,
            new Separator(),
            lblEspecs, grd,
            new Separator(),
            lblHistorico, tabelaHistorico,
            new Separator(),
            rodape
        );
    }

    // ============================
    // AUXILIARES
    // ============================

    private void adicionarCampo(GridPane grd, int linha, String label, String valor) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("detalhe-label");
        Label val = new Label(valor != null ? valor : "—");
        val.getStyleClass().add("detalhe-valor");
        grd.add(lbl, 0, linha);
        grd.add(val, 1, linha);
    }

    /**
     * Devolve a transmissão do veículo.
     * O campo não existe diretamente no modelo atual, por isso
     * apresenta um valor por omissão até ser adicionado à BD.
     */
    private String obterTransmissao(Veiculo v) {
        // TODO: adicionar campo transmissao ao modelo Veiculo quando a BD for atualizada
        return "Manual";
    }

    /**
     * Devolve a lotação do veículo.
     */
    private String obterLotacao(Veiculo v) {
        // TODO: adicionar campo lotacao ao modelo Veiculo quando a BD for atualizada
        return "5 lugares";
    }

    /**
     * Devolve a quilometragem atual.
     * Usa o campo kmFinal da última reserva concluída, se existir.
     */
    private String obterQuilometragem(Veiculo v) {
        try {
            ReservaDAO dao = new ReservaDAO(DatabaseConnection.getConnection());
            List<Reserva> reservas = dao.listarPorVeiculo(v.getId());
            int maxKm = reservas.stream()
                .mapToInt(Reserva::getKmFinal)
                .max()
                .orElse(0);
            return maxKm > 0 ? maxKm + " km" : "—";
        } catch (Exception ex) {
            return "—";
        }
    }

    private void carregarHistorico(int veiculoId, TableView<Reserva> tabela) {
        try {
            ReservaDAO dao = new ReservaDAO(DatabaseConnection.getConnection());
            List<Reserva> lista = dao.listarPorVeiculo(veiculoId);
            tabela.getItems().addAll(lista);
            if (lista.isEmpty()) {
                tabela.setPlaceholder(new Label("Este veículo ainda não foi alugado."));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            tabela.setPlaceholder(new Label("Erro ao carregar histórico."));
        }
    }

    public VBox getRoot() {
        return root;
    }
}
