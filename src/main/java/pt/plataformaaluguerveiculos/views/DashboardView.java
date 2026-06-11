package pt.plataformaaluguerveiculos.views;

import java.sql.Connection;
import java.util.List;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.User;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardView {

    private VBox root;

    public DashboardView() {
        root = new VBox(35);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("dashboard-container");

        User user = SessionManager.getInstance().getUtilizador();
        String nome  = user != null ? user.getNome() : "Utilizador";
        String saldo = user != null ? String.format("%.2f €", user.getSaldo()) : "—";

        // ============================
        // HERO — Saudação + Saldo
        // ============================
        Label titulo = new Label("Bem-vindo, " + nome + "!");
        titulo.getStyleClass().add("dashboard-titulo");

        Label saldoLabel = new Label("Saldo disponível: " + saldo);
        saldoLabel.getStyleClass().add("dashboard-subtitulo");

        VBox hero = new VBox(6, titulo, saldoLabel);
        hero.setAlignment(Pos.CENTER_LEFT);

        // ============================
        // ESTATÍSTICAS
        // ============================
        int[] stats = carregarStats(user);

        HBox statsBox = new HBox(20,
            criarStatCard("Reservas Pendentes", stats[0], "#ffebee", "#e60000"),
            criarStatCard("Reservas Aceites",   stats[1], "#e8f5e9", "#2e7d32"),
            criarStatCard("Pedidos Recebidos",  stats[2], "#e8eaf6", "#1a237e")
        );
        statsBox.setAlignment(Pos.CENTER_LEFT); 

        // ============================
        // ACESSO RÁPIDO
        // ============================
        Label labelAcoes = new Label("Acesso Rápido");
        labelAcoes.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        HBox acoesBox = new HBox(20,
            criarAcaoCard(
                "Procurar Veículos",
                "Encontre o veículo ideal para a sua viagem",
                () -> NavigationManager.getInstance().navegarParaProcurarVeiculos()
            ),
            criarAcaoCard(
                "As Minhas Reservas",
                "Consulte e acompanhe o estado das suas reservas",
                () -> NavigationManager.getInstance().navegarParaMinhasReservas()
            ),
            criarAcaoCard(
                "Pedidos Recebidos",
                "Aceite ou rejeite pedidos dos seus veículos",
                () -> NavigationManager.getInstance().navegarParaPedidosRecebidos()
            )
        );
        acoesBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(hero, statsBox, labelAcoes, acoesBox);
    }

    private int[] carregarStats(User user) {
        int pendentes = 0, aceites = 0, pedidosRecebidos = 0;
        if (user == null) return new int[]{0, 0, 0};
        try {
            Connection conn = DatabaseConnection.getConnection();
            ReservaDAO dao = new ReservaDAO(conn);
            List<Reserva> minhas = dao.listarPorUtilizador(user.getId());
            for (Reserva r : minhas) {
                if (r.getEstado() == Reserva.Estado.PENDENTE) pendentes++;
                if (r.getEstado() == Reserva.Estado.ACEITE)   aceites++;
            }
            pedidosRecebidos = dao.listarPendentesPorProprietario(user.getId()).size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new int[]{pendentes, aceites, pedidosRecebidos};
    }

    private VBox criarStatCard(String titulo, int valor, String bgColor, String corTexto) {
        Label lblValor = new Label(String.valueOf(valor));
        lblValor.setStyle(
            "-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: " + corTexto + ";"
        );

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");

        VBox card = new VBox(4, lblValor, lblTitulo);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 35, 20, 35));
        card.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(210);
        return card;
    }

    private VBox criarAcaoCard(String titulo, String descricao, Runnable acao) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label lblDesc = new Label(descricao);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777; -fx-wrap-text: true;");
        lblDesc.setMaxWidth(210);

        Button btn = new Button("Ir");
        btn.getStyleClass().add("btn-primario");
        btn.setOnAction(e -> acao.run());

        VBox card = new VBox(12, lblTitulo, lblDesc, btn);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(240);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 3);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    public VBox getRoot() {
        return root;
    }
}
