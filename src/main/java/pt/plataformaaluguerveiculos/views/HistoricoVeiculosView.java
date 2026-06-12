package pt.plataformaaluguerveiculos.views;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.UserDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * ALV-181 — Criar endpoint histórico
 * ALV-182 — Associar reservas ao veículo
 * ALV-183 — Mostrar datas
 * ALV-184 — Mostrar utilizador locatário
 * ALV-185 — Mostrar receita gerada
 * ALV-186 — Testar consultas
 *
 * Página que o proprietário usa para ver o histórico de alugueres
 * dos seus veículos, com datas, locatário e receita gerada.
 */
public class HistoricoVeiculosView {

    private final VBox root;
    private final int proprietarioId;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public HistoricoVeiculosView(int proprietarioId) {
        this.proprietarioId = proprietarioId;

        root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // ALV-181 — Construção da página
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        // Cabeçalho
        Label titulo = new Label("Histórico de Alugueres dos Veículos");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label subtitulo = new Label("Histórico de reservas e receita gerada por veículo");
        subtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #777777;");

        root.getChildren().addAll(titulo, subtitulo);

        // Carregar veículos do proprietário — ALV-182
        List<Veiculo> veiculos = carregarVeiculos();

        if (veiculos.isEmpty()) {
            Label vazio = new Label("Ainda não tem veículos registados.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            vazio.setPadding(new Insets(40, 0, 0, 0));
            root.getChildren().add(vazio);
            return;
        }

        // Uma tab por veículo
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: white;");

        for (Veiculo v : veiculos) {
            tabs.getTabs().add(criarTabVeiculo(v));
        }

        root.getChildren().add(tabs);
    }

    // ----------------------------------------------------------------
    // ALV-182 — Tab por veículo com lista de reservas
    // ----------------------------------------------------------------

    private Tab criarTabVeiculo(Veiculo v) {
        Tab tab = new Tab(v.getMarca() + " " + v.getModelo() + " #" + v.getId());

        List<Reserva> reservas = carregarReservasVeiculo(v.getId());

        // ALV-185 — Calcular receita total
        double receitaTotal = reservas.stream()
            .filter(r -> r.getEstado() == Reserva.Estado.CONCLUIDO)
            .mapToDouble(Reserva::getPrecoTotal)
            .sum();

        VBox conteudo = new VBox(12);
        conteudo.setPadding(new Insets(16));
        conteudo.setStyle("-fx-background-color: white;");

        // Card resumo do veículo
        HBox resumo = criarResumoVeiculo(v, reservas.size(), receitaTotal);
        conteudo.getChildren().add(resumo);

        if (reservas.isEmpty()) {
            Label vazio = new Label("Ainda não existem reservas para este veículo.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            vazio.setPadding(new Insets(20, 0, 0, 0));
            conteudo.getChildren().add(vazio);
        } else {
            for (Reserva r : reservas) {
                conteudo.getChildren().add(criarCardReserva(r));
            }
        }

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        scroll.setPrefHeight(500);

        tab.setContent(scroll);
        return tab;
    }

    // ----------------------------------------------------------------
    // ALV-185 — Card resumo do veículo com receita
    // ----------------------------------------------------------------

    private HBox criarResumoVeiculo(Veiculo v, int totalReservas, double receitaTotal) {
        HBox hbox = new HBox(16);
        hbox.setAlignment(Pos.CENTER_LEFT);

        VBox cardInfo = criarCardResumo("Veículo", v.getMarca() + " " + v.getModelo()
            + " (" + v.getAno() + ")", "#1a237e", "#e8eaf6");

        VBox cardReservas = criarCardResumo("Total Reservas",
            totalReservas + " reserva" + (totalReservas != 1 ? "s" : ""), "#e65100", "#fff3e0");

        VBox cardReceita = criarCardResumo("Receita Total",
            String.format("%.2f€", receitaTotal), "#2e7d32", "#e8f5e9");

        hbox.getChildren().addAll(cardInfo, cardReservas, cardReceita);
        return hbox;
    }

    private VBox criarCardResumo(String titulo, String valor, String corTexto, String corFundo) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(
            "-fx-background-color: " + corFundo + ";" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        card.setMinWidth(160);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 11px; -fx-text-fill: #777777;");

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + corTexto + ";");

        card.getChildren().addAll(lblTitulo, lblValor);
        return card;
    }

    // ----------------------------------------------------------------
    // ALV-183 / ALV-184 — Card de reserva com datas e locatário
    // ----------------------------------------------------------------

    private VBox criarCardReserva(Reserva r) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);"
        );

        // Linha topo: id + estado
        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("Reserva #" + r.getId());
        lblId.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        String corEstado = corPorEstado(r.getEstado());
        String fundoEstado = fundoPorEstado(r.getEstado());
        Label badge = new Label(r.getEstado().name());
        badge.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + corEstado + ";" +
            "-fx-background-color: " + fundoEstado + ";" +
            "-fx-background-radius: 4; -fx-padding: 2 8 2 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ALV-185 — receita desta reserva
        Label lblReceita = new Label(String.format("%.2f€", r.getPrecoTotal()));
        lblReceita.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        topo.getChildren().addAll(lblId, badge, spacer, lblReceita);

        // ALV-183 — datas
        Label lblDatas = new Label(
            "Período: " + r.getDataInicio() + " → " + r.getDataFim()
            + "  (" + r.getNumeroDias() + " dias)"
        );
        lblDatas.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

        // ALV-184 — nome do locatário
        String nomeLocatario = obterNomeLocatario(r.getUtilizadorId());
        Label lblLocatario = new Label("Locatário: " + nomeLocatario);
        lblLocatario.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

        card.getChildren().addAll(topo, lblDatas, lblLocatario);
        return card;
    }

    // ----------------------------------------------------------------
    // ALV-181 — Carregar veículos do proprietário
    // ----------------------------------------------------------------

    private List<Veiculo> carregarVeiculos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new VeiculoDAO().listarTodos().stream()
                .filter(v -> v.getProprietarioId() == proprietarioId)
                .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // ALV-182 — Carregar reservas do veículo
    // ----------------------------------------------------------------

    private List<Reserva> carregarReservasVeiculo(int veiculoId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new ReservaDAO(conn).listarPorVeiculo(veiculoId);
        } catch (SQLException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // ALV-184 — Obter nome do locatário pelo id
    // ----------------------------------------------------------------

    private String obterNomeLocatario(int utilizadorId) {
        try {
            Optional<User> user = new UserDAO().findById(utilizadorId);
            return user.map(User::getNome).orElse("Utilizador #" + utilizadorId);
        } catch (SQLException e) {
            return "Utilizador #" + utilizadorId;
        }
    }

    // ----------------------------------------------------------------
    // Auxiliares de estilo
    // ----------------------------------------------------------------

    private String corPorEstado(Reserva.Estado estado) {
        switch (estado) {
            case ACEITE:    return "#2e7d32";
            case PENDENTE:  return "#e65100";
            case CONCLUIDO: return "#1a237e";
            default:        return "#c62828";
        }
    }

    private String fundoPorEstado(Reserva.Estado estado) {
        switch (estado) {
            case ACEITE:    return "#e8f5e9";
            case PENDENTE:  return "#fff3e0";
            case CONCLUIDO: return "#e8eaf6";
            default:        return "#ffebee";
        }
    }

    public VBox getRoot() {
        return root;
    }
}