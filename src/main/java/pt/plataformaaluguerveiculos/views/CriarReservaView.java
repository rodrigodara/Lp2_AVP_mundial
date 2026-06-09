package pt.plataformaaluguerveiculos.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.aluguer.controller.ReservaService;
import com.aluguer.controller.ReservaService.ReservaException;
import com.aluguer.model.Reserva;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * ALV-64 – Criar Pedido de Reserva
 *   ALV-86 – Formulário de reserva
 *   ALV-87 – Selecionar data de início
 *   ALV-88 – Selecionar data de fim
 *   ALV-89 – Submeter pedido via ReservaService
 *   ALV-90 – Reserva associada ao utilizador
 *   ALV-91 – Reserva associada ao veículo
 */
public class CriarReservaView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VBox root;
    private final ReservaService service;
    private final int utilizadorId;
    private final int veiculoId;
    private final double precoDiaBase;
    private final double consumo;
    private final double precoCombustivel;
    private final double kmDiaMedia;
    private final double saldo;
    private final String nomeVeiculo;

    private DatePicker dpInicio;
    private DatePicker dpFim;
    private Label lblRenda;
    private Label lblCombustivel;
    private Label lblCaucao;
    private Label lblTotal;
    private Label lblNota;
    private Label lblErro;
    private Button btnReservar;
    private ProgressIndicator spinner;

    /**
     * @param service          ReservaService instanciado com a ligação BD
     * @param utilizadorId     id do utilizador autenticado
     * @param veiculoId        id do veículo a reservar
     * @param precoDiaBase     preço/dia base do proprietário
     * @param consumo          L/100km ou kWh/100km
     * @param precoCombustivel preço corrente do combustível
     * @param kmDiaMedia       média histórica km/dia (0 = sem histórico)
     * @param saldo            saldo atual do utilizador
     * @param nomeVeiculo      ex: "Toyota Yaris 2021"
     */
    public CriarReservaView(ReservaService service,
                            int utilizadorId, int veiculoId,
                            double precoDiaBase, double consumo,
                            double precoCombustivel, double kmDiaMedia,
                            double saldo, String nomeVeiculo) {
        this.service          = service;
        this.utilizadorId     = utilizadorId;
        this.veiculoId        = veiculoId;
        this.precoDiaBase     = precoDiaBase;
        this.consumo          = consumo;
        this.precoCombustivel = precoCombustivel;
        this.kmDiaMedia       = kmDiaMedia;
        this.saldo            = saldo;
        this.nomeVeiculo      = nomeVeiculo;

        root = new VBox(16);
        root.setPadding(new Insets(32));
        root.setMaxWidth(540);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("reserva-container");

        construirFormulario();
        atualizarSimulacao();
    }

    private void construirFormulario() {
        Text titulo = new Text("Pedido de Reserva");
        titulo.getStyleClass().add("reserva-titulo");

        Text sub = new Text(nomeVeiculo);
        sub.getStyleClass().add("reserva-subtitulo");

        // ALV-87 – Data de início
        Label lblI = new Label("Data de início");
        lblI.getStyleClass().add("reserva-campo-label");
        dpInicio = new DatePicker(LocalDate.now().plusDays(1));
        dpInicio.setMaxWidth(Double.MAX_VALUE);
        dpInicio.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        // ALV-88 – Data de fim
        Label lblF = new Label("Data de fim");
        lblF.getStyleClass().add("reserva-campo-label");
        dpFim = new DatePicker(LocalDate.now().plusDays(2));
        dpFim.setMaxWidth(Double.MAX_VALUE);
        dpFim.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                LocalDate min = dpInicio.getValue();
                setDisable(empty || (min != null && d.isBefore(min)));
            }
        });

        dpInicio.valueProperty().addListener((o, a, n) -> {
            if (n != null && dpFim.getValue() != null && dpFim.getValue().isBefore(n))
                dpFim.setValue(n.plusDays(1));
            atualizarSimulacao();
        });
        dpFim.valueProperty().addListener((o, a, n) -> atualizarSimulacao());

        VBox painelPreco = construirPainelPreco();

        Label lblSaldo = new Label(String.format("Saldo disponível: %.2f€", saldo));
        lblSaldo.getStyleClass().add("reserva-saldo");

        lblErro = new Label();
        lblErro.getStyleClass().add("mensagem-erro");
        lblErro.setVisible(false);
        lblErro.setWrapText(true);

        spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        spinner.setVisible(false);

        btnReservar = new Button("Pedir Reserva");
        btnReservar.getStyleClass().add("btn-primario");
        btnReservar.setMaxWidth(Double.MAX_VALUE);
        btnReservar.setOnAction(e -> submeter());

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setOnAction(e -> NavigationManager.getInstance().navegarParaDashboard());

        HBox botoes = new HBox(12, btnCancelar, btnReservar);
        HBox.setHgrow(btnReservar, Priority.ALWAYS);
        HBox.setHgrow(btnCancelar, Priority.ALWAYS);

        root.getChildren().addAll(
            titulo, sub, new Separator(),
            lblI, dpInicio,
            lblF, dpFim,
            painelPreco,
            lblSaldo, lblErro, spinner,
            botoes
        );
    }

    private VBox construirPainelPreco() {
        VBox p = new VBox(6);
        p.getStyleClass().add("reserva-painel-preco");
        p.setPadding(new Insets(12));

        Text t = new Text("Estimativa de custo");
        t.getStyleClass().add("reserva-preco-titulo");

        lblRenda       = new Label(); lblRenda.getStyleClass().add("reserva-preco-linha");
        lblCombustivel = new Label(); lblCombustivel.getStyleClass().add("reserva-preco-linha");
        lblCaucao      = new Label(); lblCaucao.getStyleClass().add("reserva-preco-linha");
        lblTotal       = new Label(); lblTotal.getStyleClass().add("reserva-preco-total");
        lblNota        = new Label(); lblNota.getStyleClass().add("reserva-preco-nota");
        lblNota.setWrapText(true);

        p.getChildren().addAll(t, new Separator(),
            lblRenda, lblCombustivel, lblCaucao,
            new Separator(), lblTotal, lblNota);
        return p;
    }

    private void atualizarSimulacao() {
        LocalDate ini = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        if (ini == null || fim == null || fim.isBefore(ini)) {
            lblRenda.setText("Renda: –");
            lblCombustivel.setText("Combustível estimado: –");
            lblCaucao.setText("Caução (20%): –");
            lblTotal.setText("Total a bloquear: –");
            lblNota.setText("");
            return;
        }

        long dias     = ChronoUnit.DAYS.between(ini, fim) + 1;
        double renda  = service.calcularRenda(precoDiaBase, ini, fim);
        double comb   = service.calcularCombustivelEstimado(kmDiaMedia, dias, consumo, precoCombustivel);
        double caucao = service.calcularCaucao(renda, comb);
        double total  = renda + caucao;

        lblRenda.setText(String.format("Renda (%d dia%s): %.2f€", dias, dias > 1 ? "s" : "", renda));
        if (kmDiaMedia <= 0 || consumo <= 0 || precoCombustivel <= 0) {
            lblCombustivel.setText("Combustível estimado: não disponível (sem dados do veículo)");
        } else {
            lblCombustivel.setText(String.format("Combustível estimado: %.2f€", comb));
        }
        lblCaucao.setText(String.format("Caução: 20%% = %.2f€", caucao));
        lblTotal.setText(String.format("Total a bloquear: %.2f€", total));
        lblNota.setText(dias >= 7
            ? "✔ Desconto de longa duração (-10%) aplicado"
            : "A caução é devolvida no final sem incidentes.");

        if (saldo < total) {
            lblErro.setText(String.format("Saldo insuficiente. Necessário %.2f€, disponível %.2f€.", total, saldo));
            lblErro.setVisible(true);
            btnReservar.setDisable(true);
        } else {
            lblErro.setVisible(false);
            btnReservar.setDisable(false);
        }
    }

    private void submeter() {
        LocalDate ini = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        lblErro.setVisible(false);
        btnReservar.setDisable(true);
        spinner.setVisible(true);

        new Thread(() -> {
            try {
                Reserva r = service.criarPedido(
                    utilizadorId, veiculoId, ini, fim,
                    precoDiaBase, consumo, precoCombustivel, kmDiaMedia, saldo);
                Platform.runLater(() -> mostrarSucesso(r));
            } catch (ReservaException ex) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    btnReservar.setDisable(false);
                    lblErro.setText(ex.getMessage());
                    lblErro.setVisible(true);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    btnReservar.setDisable(false);
                    lblErro.setText("Erro inesperado. Tente novamente.");
                    lblErro.setVisible(true);
                });
            }
        }, "thread-reserva").start();
    }

    private void mostrarSucesso(Reserva r) {
        root.getChildren().clear();
        VBox ok = new VBox(16);
        ok.setAlignment(Pos.CENTER);
        ok.setPadding(new Insets(40));

        Text icone = new Text("✔");
        icone.getStyleClass().add("reserva-icone-sucesso");

        Text msg = new Text("Pedido enviado com sucesso!");
        msg.getStyleClass().add("reserva-titulo");

        Text det = new Text(String.format(
            "O proprietário tem 24h para aceitar ou rejeitar.%nPeríodo: %s → %s%nRenda: %.2f€  |  Caução: %.2f€",
            r.getDataInicio().format(FMT), r.getDataFim().format(FMT),
            r.getPrecoTotal(), r.getCaucao()));
        det.getStyleClass().add("reserva-subtitulo");
        det.setWrappingWidth(460);

        Button btnVoltar = new Button("Ir para o Dashboard");
        btnVoltar.getStyleClass().add("btn-primario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaDashboard());

        ok.getChildren().addAll(icone, msg, det, btnVoltar);
        root.getChildren().add(ok);
        spinner.setVisible(false);
    }

    public VBox getRoot() { return root; }
}