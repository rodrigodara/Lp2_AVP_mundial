package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.aluguer.dao.TransactionDAO;
import com.aluguer.model.Transaction;
import com.aluguer.model.User;
import com.aluguer.service.ContaService;
import com.aluguer.service.PerfilService;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * ALV-118 — Criar interface de gestão de saldo
 *
 * Página da conta do utilizador com: - Dados de perfil editáveis (nome, email,
 * NIF, carta de condução, foto) - Cartão de saldo com aspeto de cartão bancário
 * - Estatísticas rápidas (total depositado / levantado) - Depósito e
 * levantamento através de modais com dados de pagamento simulados (não são
 * guardados em lado nenhum) - Transações recentes
 */
public class ContaView {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double AVATAR_SIZE = 60;

    private final VBox root;
    private final ContaService contaService;
    private final PerfilService perfilService;

    private Label lblSaldoCartao;
    private VBox transacoesBox;

    // ----------------------------------------------------------------
    // Construtor
    // ----------------------------------------------------------------
    public ContaView() {
        this.contaService = new ContaService();
        this.perfilService = new PerfilService();

        root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("reserva-container");

        construirPagina();
    }

    // ----------------------------------------------------------------
    // Construir a página
    // ----------------------------------------------------------------
    private void construirPagina() {
        root.getChildren().clear();

        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            return;
        }

        Label titulo = new Label("Gestão de Conta");
        titulo.getStyleClass().add("reserva-titulo");

        Label subtitulo = new Label("Os seus dados, o seu saldo e as suas transações, tudo num só sítio.");
        subtitulo.getStyleClass().add("reserva-subtitulo");

        VBox cardPerfil = criarCardPerfil(user);

        VBox cartaoBancario = criarCartaoBancario(user);

        VBox cardTransacoes = criarCardTransacoesRecentes(user);

        root.getChildren().addAll(titulo, subtitulo, cardPerfil, cartaoBancario, cardTransacoes);
    }

    // ==================================================================
    // CARD DE PERFIL
    // ==================================================================
    private VBox criarCardPerfil(User user) {
        VBox card = new VBox(14);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(20));

        StackPane avatar = criarAvatar(user, AVATAR_SIZE);

        Label lblNome = new Label(user.getNome() != null ? user.getNome() : "Utilizador");
        lblNome.getStyleClass().add("reserva-card-titulo");
        lblNome.setStyle("-fx-font-size: 17px;");

        Label lblEmail = new Label(user.getEmail());
        lblEmail.getStyleClass().add("reserva-preco-linha");

        VBox nomeEmailBox = new VBox(2, lblNome, lblEmail);
        nomeEmailBox.setAlignment(Pos.CENTER_LEFT);

        Button btnEditar = new Button("Editar perfil");
        btnEditar.getStyleClass().add("btn-secundario");
        btnEditar.setOnAction(e -> abrirModalEditarPerfil(user));

        HBox cabecalho = new HBox(16, avatar, nomeEmailBox, criarEspacador(), btnEditar);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        HBox detalhes = new HBox(40);
        detalhes.setPadding(new Insets(8, 0, 0, 0));
        detalhes.getChildren().addAll(
                criarBlocoDetalhe("NIF", user.getNif() != null ? user.getNif() : "—"),
                criarBlocoCartaConducao(user),
                criarBlocoDetalhe("Membro desde",
                        user.getDataCriacao() != null ? user.getDataCriacao().format(FORMATO_DATA) : "—")
        );

        card.getChildren().addAll(cabecalho, detalhes);
        return card;
    }

    private VBox criarBlocoDetalhe(String label, String valor) {
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-font-weight: bold;");
        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        return new VBox(3, lblLabel, lblValor);
    }

    private VBox criarBlocoCartaConducao(User user) {
        Label lblLabel = new Label("Carta de condução");
        lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-font-weight: bold;");

        String numero = user.getNumeroCarta() != null ? user.getNumeroCarta() : "—";
        Label lblNumero = new Label(numero);
        lblNumero.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        HBox linha = new HBox(8, lblNumero);
        linha.setAlignment(Pos.CENTER_LEFT);

        if (user.getValidadeCarta() != null) {
            boolean valida = user.isCartaValida();
            Label badge = new Label((valida ? "Válida até " : "Expirou em ") + user.getValidadeCarta().format(FORMATO_DATA));
            badge.getStyleClass().add(valida ? "reserva-badge-aceite" : "reserva-badge-rejeitado");
            linha.getChildren().add(badge);
        }

        return new VBox(3, lblLabel, linha);
    }

    private String obterIniciais(String nome) {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }

    // ----------------------------------------------------------------
    // Avatar (foto de perfil, ou iniciais se não houver foto)
    // ----------------------------------------------------------------
    private Node construirAvatarVisual(byte[] foto, String nome, double tamanho) {
        if (foto != null && foto.length > 0) {
            try {
                ImageView iv = new ImageView(new Image(new ByteArrayInputStream(foto)));
                iv.setFitWidth(tamanho);
                iv.setFitHeight(tamanho);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setClip(new Circle(tamanho / 2, tamanho / 2, tamanho / 2));
                return iv;
            } catch (Exception ignored) {
                // imagem corrompida — cai para as iniciais
            }
        }
        Label iniciais = new Label(obterIniciais(nome));
        iniciais.setMinSize(tamanho, tamanho);
        iniciais.setMaxSize(tamanho, tamanho);
        iniciais.setAlignment(Pos.CENTER);
        iniciais.setStyle(
                "-fx-background-color: #1a237e;"
                + "-fx-background-radius: " + (tamanho / 2) + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: " + (tamanho * 0.34) + "px;"
                + "-fx-font-weight: bold;"
        );
        return iniciais;
    }

    private StackPane criarAvatar(User user, double tamanho) {
        StackPane box = new StackPane(construirAvatarVisual(user.getFoto(), user.getNome(), tamanho));
        box.setMinSize(tamanho, tamanho);
        box.setMaxSize(tamanho, tamanho);
        return box;
    }

    // ==================================================================
    // CARTÃO DE SALDO (aspeto de cartão bancário — só visual)
    // ==================================================================
    private VBox criarCartaoBancario(User user) {
        Label lblMarca = new Label("AVL MUNDIAL");
        lblMarca.getStyleClass().add("conta-cartao-marca");

        Region chip = new Region();
        chip.setMinSize(38, 28);
        chip.setMaxSize(38, 28);
        chip.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ffe082, #ffb300);"
                + "-fx-background-radius: 5;"
                + "-fx-border-color: rgba(0,0,0,0.15);"
                + "-fx-border-radius: 5;"
                + "-fx-border-width: 1;"
        );

        HBox linhaTopo = new HBox(chip, criarEspacador(), lblMarca);
        linhaTopo.setAlignment(Pos.CENTER_LEFT);

        Label lblSaldoLabel = new Label("Saldo disponível");
        lblSaldoLabel.getStyleClass().add("conta-cartao-label");

        lblSaldoCartao = new Label(formatarSaldoComPendente(user));
        lblSaldoCartao.getStyleClass().add("conta-cartao-saldo");

        String numeroFalso = String.format("•••• •••• •••• %04d", Math.abs(user.getId()) % 10000);
        Label lblNumero = new Label(numeroFalso);
        lblNumero.getStyleClass().add("conta-cartao-numero");

        LocalDate hoje = LocalDate.now();
        String validadeFalsa = String.format("%02d/%02d", hoje.getMonthValue(), (hoje.getYear() + 4) % 100);

        Label lblTitularLabel = new Label("TITULAR");
        lblTitularLabel.getStyleClass().add("conta-cartao-label");
        Label lblTitular = new Label(user.getNome() != null ? user.getNome().toUpperCase() : "—");
        lblTitular.getStyleClass().add("conta-cartao-titular");

        Label lblValidLabel = new Label("VÁLIDO ATÉ");
        lblValidLabel.getStyleClass().add("conta-cartao-label");
        Label lblValid = new Label(validadeFalsa);
        lblValid.getStyleClass().add("conta-cartao-titular");

        VBox titularBox = new VBox(2, lblTitularLabel, lblTitular);
        VBox validBox = new VBox(2, lblValidLabel, lblValid);

        HBox linhaBase = new HBox(titularBox, criarEspacador(), validBox);
        linhaBase.setAlignment(Pos.BOTTOM_LEFT);

        VBox cartao = new VBox(18, linhaTopo, new VBox(4, lblSaldoLabel, lblSaldoCartao), lblNumero, linhaBase);
        cartao.getStyleClass().add("conta-cartao");
        cartao.setPadding(new Insets(22));
        cartao.setPrefHeight(210);

        // ---- Botões de ação, logo abaixo do cartão ----
        Button btnDepositar = new Button("↑  Depositar");
        btnDepositar.getStyleClass().add("btn-aceitar");
        btnDepositar.setMaxWidth(Double.MAX_VALUE);
        btnDepositar.setOnAction(e -> abrirModalDepositar());

        Button btnLevantar = new Button("↓  Levantar");
        btnLevantar.getStyleClass().add("btn-rejeitar");
        btnLevantar.setMaxWidth(Double.MAX_VALUE);
        btnLevantar.setOnAction(e -> abrirModalLevantar());

        HBox botoes = new HBox(12, btnDepositar, btnLevantar);
        HBox.setHgrow(btnDepositar, Priority.ALWAYS);
        HBox.setHgrow(btnLevantar, Priority.ALWAYS);

        VBox wrapper = new VBox(14, cartao, botoes);
        wrapper.setPrefWidth(380);
        wrapper.setMinWidth(380);
        wrapper.setMaxWidth(380);
        return wrapper;
    }

    private Region criarEspacador() {
        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);
        return espacador;
    }

    private String formatarSaldo(BigDecimal saldo) {
        return String.format("%.2f €", saldo != null ? saldo : BigDecimal.ZERO);
    }

    private String formatarSaldoComPendente(com.aluguer.model.User u) {
        BigDecimal disponivel = u.getSaldoDisponivel();
        BigDecimal pendente   = u.getSaldoPendente();
        String base = String.format("%.2f €", disponivel);
        if (pendente != null && pendente.compareTo(BigDecimal.ZERO) > 0) {
            base += String.format("%n(Pendente: %.2f €)", pendente);
        }
        return base;
    }

    // ==================================================================
    // CARD DE TRANSAÇÕES RECENTES
    // ==================================================================
    private VBox criarCardTransacoesRecentes(User user) {
        VBox card = new VBox(12);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(20));

        Label lblTitulo = new Label("Transações recentes");
        lblTitulo.getStyleClass().add("reserva-card-titulo");

        transacoesBox = new VBox(8);
        carregarTransacoesRecentes(user);

        card.getChildren().addAll(lblTitulo, transacoesBox);
        return card;
    }

    private void carregarTransacoesRecentes(User user) {
        transacoesBox.getChildren().clear();
        List<Transaction> transacoes;

        try (Connection conn = DatabaseConnection.getConnection()) {
            transacoes = new TransactionDAO(conn).listarPorUtilizador(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
            transacoes = List.of();
        }

        if (transacoes.isEmpty()) {
            Label vazio = new Label("Ainda não há transações registadas.");
            vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            transacoesBox.getChildren().add(vazio);
            return;
        }

        transacoes.stream().limit(5).forEach(t -> transacoesBox.getChildren().add(criarLinhaTransacao(t)));
    }

    private HBox criarLinhaTransacao(Transaction t) {
        boolean deposito = t.getTipo() == Transaction.Tipo.deposito;

        Label icone = new Label(deposito ? "↑" : "↓");
        icone.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;"
                + "-fx-background-color: " + (deposito ? "#2e7d32" : "#c62828") + ";"
                + "-fx-background-radius: 12; -fx-min-width: 24; -fx-min-height: 24;"
                + "-fx-alignment: center;"
        );
        icone.setAlignment(Pos.CENTER);

        Label lblTipo = new Label(deposito ? "Depósito" : "Levantamento");
        lblTipo.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-weight: bold;");

        Label lblData = new Label(t.getData() != null ? t.getData().format(FORMATO_DATA_HORA) : "—");
        lblData.setStyle("-fx-font-size: 11px; -fx-text-fill: #999999;");

        VBox infoBox = new VBox(1, lblTipo, lblData);

        Region espacador = criarEspacador();

        Label lblValor = new Label((deposito ? "+ " : "- ") + String.format("%.2f €", t.getValor()));
        lblValor.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (deposito ? "#2e7d32" : "#c62828") + ";"
        );

        HBox linha = new HBox(12, icone, infoBox, espacador, lblValor);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(8, 4, 8, 4));
        linha.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0;");
        return linha;
    }

    // ==================================================================
    // MODAL — EDITAR PERFIL (incluindo foto)
    // ==================================================================
    private void abrirModalEditarPerfil(User user) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label lblTitulo = new Label("Editar perfil");
        lblTitulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");
        VBox topo = new VBox(lblTitulo);
        topo.setPadding(new Insets(20, 24, 18, 24));
        topo.setStyle("-fx-background-color: #1a237e; -fx-background-radius: 12 12 0 0;");

        // ---- Foto de perfil ----
        byte[][] fotoSelecionada = {user.getFoto()};
        StackPane previewBox = new StackPane();
        previewBox.setMinSize(80, 80);
        previewBox.setMaxSize(80, 80);

        Runnable atualizarPreview = ()
                -> previewBox.getChildren().setAll(construirAvatarVisual(fotoSelecionada[0], user.getNome(), 80));
        atualizarPreview.run();

        Button btnEscolherFoto = new Button("Escolher foto…");
        btnEscolherFoto.getStyleClass().add("btn-secundario");
        Button btnRemoverFoto = new Button("Remover foto");
        btnRemoverFoto.getStyleClass().add("btn-secundario");

        btnEscolherFoto.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Escolher foto de perfil");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File ficheiro = chooser.showOpenDialog(dialog);
            if (ficheiro != null) {
                try {
                    fotoSelecionada[0] = Files.readAllBytes(ficheiro.toPath());
                    atualizarPreview.run();
                } catch (IOException ex) {
                    mostrarFeedback(ResultadoOperacao.erro("Não foi possível ler a imagem selecionada."));
                }
            }
        });

        btnRemoverFoto.setOnAction(e -> {
            fotoSelecionada[0] = null;
            atualizarPreview.run();
        });

        VBox botoesFoto = new VBox(8, btnEscolherFoto, btnRemoverFoto);
        HBox linhaFoto = new HBox(16, previewBox, botoesFoto);
        linhaFoto.setAlignment(Pos.CENTER_LEFT);

        // ---- Campos do perfil ----
        TextField txtNome = new TextField(user.getNome());
        txtNome.setPromptText("Nome completo");
        txtNome.getStyleClass().add("campo-texto");

        TextField txtEmail = new TextField(user.getEmail());
        txtEmail.setPromptText("Email");
        txtEmail.getStyleClass().add("campo-texto");

        TextField txtNif = new TextField(user.getNif());
        txtNif.setPromptText("NIF");
        txtNif.getStyleClass().add("campo-texto");

        TextField txtCarta = new TextField(user.getNumeroCarta());
        txtCarta.setPromptText("Número da carta de condução");
        txtCarta.getStyleClass().add("campo-texto");

        DatePicker dpValidade = new DatePicker(user.getValidadeCarta());
        dpValidade.setPromptText("Validade da carta de condução");
        dpValidade.setMaxWidth(Double.MAX_VALUE);

        VBox corpo = new VBox(12, linhaFoto, txtNome, txtEmail, txtNif, txtCarta, dpValidade);
        corpo.setPadding(new Insets(20, 24, 10, 24));

        Button btnGuardar = new Button("Guardar alterações");
        btnGuardar.getStyleClass().add("btn-aceitar");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");

        btnGuardar.setOnAction(e -> {
            ResultadoOperacao resultado = perfilService.atualizarPerfil(
                    txtNome.getText(), txtEmail.getText(), txtNif.getText(),
                    txtCarta.getText(), dpValidade.getValue(), fotoSelecionada[0]
            );
            mostrarFeedback(resultado);
            if (resultado.isSucesso()) {
                dialog.close();
                construirPagina();
            }
        });
        btnCancelar.setOnAction(e -> dialog.close());

        HBox rodape = new HBox(10, btnCancelar, btnGuardar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.setPadding(new Insets(8, 24, 20, 24));

        VBox dialogRoot = new VBox(topo, corpo, rodape);
        dialogRoot.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 20, 0, 0, 8);"
        );

        dialogRoot.setPrefWidth(440);
        dialogRoot.setMinWidth(440);
        Scene scene = new Scene(dialogRoot);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    // ==================================================================
    // MODAL — Helpers de estilo moderno
    // ==================================================================
    private TextField criarCampoModerno(String prompt) {
        TextField campo = new TextField();
        campo.setPromptText(prompt);
        campo.getStyleClass().add("campo-texto");
        campo.setStyle(
                "-fx-background-color: #f5f6fa;"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: #e3e5ec;"
                + "-fx-border-radius: 10;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 11 14 11 14;"
                + "-fx-font-size: 13px;"
        );
        return campo;
    }

    private PasswordField criarCampoPasswordModerno(String prompt) {
        PasswordField campo = new PasswordField();
        campo.setPromptText(prompt);
        campo.getStyleClass().add("campo-texto");
        campo.setStyle(
                "-fx-background-color: #f5f6fa;"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: #e3e5ec;"
                + "-fx-border-radius: 10;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 11 14 11 14;"
                + "-fx-font-size: 13px;"
        );
        return campo;
    }

    private Label criarLabelCampo(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6b6f80; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox criarCampoComLabel(String labelTexto, Node campo) {
        return new VBox(6, criarLabelCampo(labelTexto), campo);
    }

    private VBox criarCabecalhoModal(String emoji, String corPrincipal, String corSecundaria, String titulo) {
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle(
                "-fx-font-size: 22px;"
                + "-fx-background-color: rgba(255,255,255,0.22);"
                + "-fx-background-radius: 14;"
                + "-fx-min-width: 46; -fx-min-height: 46;"
                + "-fx-max-width: 46; -fx-max-height: 46;"
                + "-fx-alignment: center;"
        );
        lblEmoji.setAlignment(Pos.CENTER);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox linha = new HBox(14, lblEmoji, lblTitulo);
        linha.setAlignment(Pos.CENTER_LEFT);

        VBox topo = new VBox(linha);
        topo.setPadding(new Insets(26, 28, 24, 28));
        topo.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + corPrincipal + ", " + corSecundaria + ");"
                + "-fx-background-radius: 18 18 0 0;"
        );
        return topo;
    }

    private void aplicarEstiloRodape(HBox rodape) {
        rodape.setPadding(new Insets(10, 28, 26, 28));
    }

    private void aplicarEstiloDialog(VBox dialogRoot) {
        dialogRoot.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(20,20,40,0.25), 32, 0, 0, 14);"
                + "-fx-border-color: #d8dae3;"
                + "-fx-border-width: 1;"
        );
    }

    private void estilizarBotaoPrimario(Button btn, String corFundo) {
        btn.setStyle(
                "-fx-background-color: " + corFundo + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 10;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 11 22 11 22;"
                + "-fx-cursor: hand;"
        );
    }

    private void estilizarBotaoSecundario(Button btn) {
        btn.setStyle(
                "-fx-background-color: #f0f1f5;"
                + "-fx-text-fill: #45485c;"
                + "-fx-background-radius: 10;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 11 22 11 22;"
                + "-fx-cursor: hand;"
        );
    }

    // ==================================================================
    // MODAL — DEPOSITAR (com dados de pagamento simulados)
    // ==================================================================
    private void abrirModalDepositar() {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox topo = criarCabecalhoModal("💰", "#43a047", "#2e7d32", "Depositar saldo");

        TextField txtMontante = criarCampoModerno("0,00 €");
        VBox campoMontante = criarCampoComLabel("Montante a depositar", txtMontante);

        Label lblAviso = new Label("Dados de pagamento — apenas para simulação, não são guardados.");
        lblAviso.setWrapText(true);
        lblAviso.setMaxWidth(360);
        lblAviso.setStyle(
                "-fx-font-size: 11.5px; -fx-text-fill: #8a8fa3; -fx-font-style: italic;"
                + "-fx-padding: 6 0 2 0;"
        );

        TextField txtCartao = criarCampoModerno("4000 1234 5678 9010");
        VBox campoCartao = criarCampoComLabel("Número do cartão", txtCartao);

        TextField txtNomeCartao = criarCampoModerno("Nome impresso no cartão");
        VBox campoNomeCartao = criarCampoComLabel("Titular do cartão", txtNomeCartao);

        TextField txtValidadeCartao = criarCampoModerno("MM/AA");
        txtValidadeCartao.setPrefWidth(110);
        VBox campoValidade = criarCampoComLabel("Validade", txtValidadeCartao);

        PasswordField txtCvv = criarCampoPasswordModerno("CVV");
        txtCvv.setPrefWidth(90);
        VBox campoCvv = criarCampoComLabel("CVV", txtCvv);

        HBox linhaValidadeCvv = new HBox(12, campoValidade, campoCvv);

        VBox corpo = new VBox(16, campoMontante, lblAviso, campoCartao, campoNomeCartao, linhaValidadeCvv);
        corpo.setPadding(new Insets(26, 28, 12, 28));

        Button btnConfirmar = new Button("Confirmar depósito");
        estilizarBotaoPrimario(btnConfirmar, "#2e7d32");
        Button btnCancelar = new Button("Cancelar");
        estilizarBotaoSecundario(btnCancelar);

        btnConfirmar.setOnAction(e -> {
            BigDecimal montante;
            try {
                montante = new BigDecimal(txtMontante.getText().trim().replace(",", "."));
            } catch (Exception ex) {
                mostrarFeedback(ResultadoOperacao.erro("Montante inválido. Use o formato: 100.00"));
                return;
            }

            String numeroCartao = txtCartao.getText().trim().replaceAll("\\s+", "");
            if (!numeroCartao.matches("\\d{13,19}")) {
                mostrarFeedback(ResultadoOperacao.erro("Número de cartão inválido."));
                return;
            }
            if (!txtValidadeCartao.getText().trim().matches("\\d{2}/\\d{2}")) {
                mostrarFeedback(ResultadoOperacao.erro("Validade do cartão inválida. Use o formato MM/AA."));
                return;
            }
            if (!txtCvv.getText().trim().matches("\\d{3,4}")) {
                mostrarFeedback(ResultadoOperacao.erro("CVV inválido."));
                return;
            }
            if (txtNomeCartao.getText().trim().isEmpty()) {
                mostrarFeedback(ResultadoOperacao.erro("Indique o nome impresso no cartão."));
                return;
            }

            ResultadoOperacao resultado = contaService.depositar(montante);
            if (resultado.isSucesso()) {
                String ultimos4 = numeroCartao.substring(numeroCartao.length() - 4);
                mostrarFeedback(ResultadoOperacao.sucesso(
                        "Depósito de " + String.format("%.2f€", montante)
                        + " efetuado com o cartão terminado em " + ultimos4 + "."
                ));
                atualizarSaldo();
                dialog.close();
            } else {
                mostrarFeedback(resultado);
            }
        });
        btnCancelar.setOnAction(e -> dialog.close());

        HBox rodape = new HBox(10, btnCancelar, btnConfirmar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        aplicarEstiloRodape(rodape);

        VBox dialogRoot = new VBox(topo, corpo, rodape);
        aplicarEstiloDialog(dialogRoot);

        dialogRoot.setPrefWidth(430);
        dialogRoot.setMinWidth(430);
        Scene scene = new Scene(dialogRoot);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    // ==================================================================
    // MODAL — LEVANTAR (com IBAN simulado)
    // ==================================================================
    private void abrirModalLevantar() {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox topo = criarCabecalhoModal("🏦", "#e53935", "#c62828", "Levantar saldo");

        TextField txtMontante = criarCampoModerno("0,00 €");
        VBox campoMontante = criarCampoComLabel("Montante a levantar", txtMontante);

        Label lblAviso = new Label(
                "Indique o IBAN para onde o dinheiro seria enviado. É apenas uma "
                + "simulação — nenhum valor é realmente transferido — mas o saldo "
                + "na aplicação é debitado."
        );
        lblAviso.setWrapText(true);
        lblAviso.setMaxWidth(360);
        lblAviso.setStyle(
                "-fx-font-size: 11.5px; -fx-text-fill: #8a8fa3; -fx-font-style: italic;"
                + "-fx-padding: 6 0 2 0;"
        );

        TextField txtIban = criarCampoModerno("PT50 0000 0000 0000 0000 0000 0");
        VBox campoIban = criarCampoComLabel("IBAN de destino", txtIban);

        VBox corpo = new VBox(16, campoMontante, lblAviso, campoIban);
        corpo.setPadding(new Insets(26, 28, 12, 28));

        Button btnConfirmar = new Button("Confirmar levantamento");
        estilizarBotaoPrimario(btnConfirmar, "#c62828");
        Button btnCancelar = new Button("Cancelar");
        estilizarBotaoSecundario(btnCancelar);

        btnConfirmar.setOnAction(e -> {
            BigDecimal montante;
            try {
                montante = new BigDecimal(txtMontante.getText().trim().replace(",", "."));
            } catch (Exception ex) {
                mostrarFeedback(ResultadoOperacao.erro("Montante inválido. Use o formato: 100.00"));
                return;
            }

            String iban = txtIban.getText().trim().replaceAll("\\s+", "").toUpperCase();
            if (!iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}")) {
                mostrarFeedback(ResultadoOperacao.erro("IBAN inválido."));
                return;
            }

            ResultadoOperacao resultado = contaService.levantar(montante);
            if (resultado.isSucesso()) {
                mostrarFeedback(ResultadoOperacao.sucesso(
                        "Levantamento de " + String.format("%.2f€", montante)
                        + " enviado (simulação) para o IBAN " + iban + "."
                ));
                atualizarSaldo();
                dialog.close();
            } else {
                mostrarFeedback(resultado);
            }
        });
        btnCancelar.setOnAction(e -> dialog.close());

        HBox rodape = new HBox(10, btnCancelar, btnConfirmar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        aplicarEstiloRodape(rodape);

        VBox dialogRoot = new VBox(topo, corpo, rodape);
        aplicarEstiloDialog(dialogRoot);

        dialogRoot.setPrefWidth(420);
        dialogRoot.setMinWidth(420);
        Scene scene = new Scene(dialogRoot);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    // ----------------------------------------------------------------
    // Atualizar UI após operação de depósito/levantamento
    // ----------------------------------------------------------------
    private void atualizarSaldo() {
        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            return;
        }
        if (lblSaldoCartao != null) {
            lblSaldoCartao.setText(formatarSaldoComPendente(user));
        }
        carregarTransacoesRecentes(user);
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