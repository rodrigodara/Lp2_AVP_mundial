package pt.plataformaaluguerveiculos.views;

import java.math.BigDecimal;

import com.aluguer.model.User;
import com.aluguer.service.ContaService;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * ALV-118 — Criar interface de gestão de saldo
 *
 * Página da conta do utilizador com:
 *   - Saldo atual
 *   - Formulário de depósito
 *   - Formulário de levantamento
 */
public class ContaView {

    private final VBox root;
    private final ContaService contaService;
    private Label lblSaldo;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------

    public ContaView() {
        this.contaService = new ContaService();

        root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reservas-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // ALV-118 — Construir a página
    // ----------------------------------------------------------------

    private void construirPagina() {
        root.getChildren().clear();

        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) return;

        // Cabeçalho
        Label titulo = new Label("Gestão de Conta");
        titulo.getStyleClass().add("reservas-titulo");

        Label subtitulo = new Label("Gerir o saldo da sua conta");
        subtitulo.getStyleClass().add("reservas-subtitulo");

        // Card saldo atual
        VBox cardSaldo = new VBox(8);
        cardSaldo.getStyleClass().add("reserva-card");
        cardSaldo.setPadding(new Insets(20));

        Label lblSaldoTitulo = new Label("Saldo Atual");
        lblSaldoTitulo.getStyleClass().add("reservas-subtitulo");

        lblSaldo = new Label(String.format("%.2f€", user.getSaldo()));
        lblSaldo.getStyleClass().add("reservas-titulo");

        cardSaldo.getChildren().addAll(lblSaldoTitulo, lblSaldo);

        // Formulários lado a lado
        HBox formularios = new HBox(20);
        formularios.getChildren().addAll(
            criarFormulario("Depositar", "Montante a depositar", true),
            criarFormulario("Levantar", "Montante a levantar", false)
        );

        root.getChildren().addAll(titulo, subtitulo, cardSaldo, formularios);
    }

    // ----------------------------------------------------------------
    // Formulário de depósito / levantamento
    // ----------------------------------------------------------------

    private VBox criarFormulario(String titulo, String placeholder, boolean isDeposito) {
        VBox card = new VBox(12);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("reserva-card-id");

        TextField txtMontante = new TextField();
        txtMontante.setPromptText(placeholder);
        txtMontante.getStyleClass().add("campo-texto");

        Button btn = new Button(titulo);
        btn.getStyleClass().add(isDeposito ? "btn-aceitar" : "btn-rejeitar");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            String valor = txtMontante.getText().trim().replace(",", ".");
            try {
                BigDecimal montante = new BigDecimal(valor);
                ResultadoOperacao resultado = isDeposito
                    ? contaService.depositar(montante)
                    : contaService.levantar(montante);

                mostrarFeedback(resultado);

                if (resultado.isSucesso()) {
                    txtMontante.clear();
                    atualizarSaldo();
                }
            } catch (NumberFormatException ex) {
                mostrarFeedback(ResultadoOperacao.erro("Montante inválido. Use o formato: 100.00"));
            }
        });

        card.getChildren().addAll(lblTitulo, txtMontante, btn);
        return card;
    }

    // ----------------------------------------------------------------
    // Atualizar label do saldo após operação
    // ----------------------------------------------------------------

    private void atualizarSaldo() {
        User user = SessionManager.getInstance().getUtilizador();
        if (user != null && lblSaldo != null) {
            lblSaldo.setText(String.format("%.2f€", user.getSaldo()));
        }
    }

    // ----------------------------------------------------------------
    // Feedback ao utilizador
    // ----------------------------------------------------------------

    private void mostrarFeedback(ResultadoOperacao resultado) {
        Alert alert = new Alert(
            resultado.isSucesso() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
        );
        alert.setTitle(resultado.isSucesso() ? "Sucesso" : "Erro");
        alert.setHeaderText(null);
        alert.setContentText(resultado.getMensagem());
        alert.showAndWait();
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public VBox getRoot() {
        return root;
    }
}