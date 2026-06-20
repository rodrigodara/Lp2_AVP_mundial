package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.UserDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.service.AvaliacaoService;
import com.aluguer.util.DatabaseConnection;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * ALV-111 — Consulta Detalhada de Veículo.
 * Apresenta todas as especificações técnicas, avaliações (veículo e
 * proprietário) e o histórico de alugueres.
 */
public class DetalheVeiculoView {

    public enum Origem {
        MEUS_VEICULOS,
        PROCURAR_VEICULOS
    }

    private final VBox root;

    /** Construtor de compatibilidade: assume que se veio de "Os Meus Veículos" (uso histórico). */
    public DetalheVeiculoView(Veiculo veiculo) {
        this(veiculo, Origem.MEUS_VEICULOS);
    }

    public DetalheVeiculoView(Veiculo veiculo, Origem origem) {
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
        // CARROSSEL DE FOTOS
        // ============================
        javafx.scene.Node carrossel = construirCarrosselFotos(veiculo);

        // ============================
        // ESPECIFICAÇÕES TÉCNICAS (ALV-151, 152, 153, 154, 155, 156)
        // ============================
        Label lblEspecs = new Label("Especificações Técnicas");
        lblEspecs.getStyleClass().add("section-titulo");

        GridPane grd = new GridPane();
        grd.setHgap(20);
        grd.setVgap(10);
        grd.setPadding(new Insets(10, 0, 10, 10));

        adicionarCampo(grd, 0,  "Marca:",            veiculo.getMarca());
        adicionarCampo(grd, 1,  "Modelo:",           veiculo.getModelo());
        adicionarCampo(grd, 2,  "Ano:",              String.valueOf(veiculo.getAno()));
        adicionarCampo(grd, 3,  "Matrícula:",        veiculo.getMatricula());
        adicionarCampo(grd, 4,  "Combustível:",      veiculo.getCombustivel());
        adicionarCampo(grd, 5,  "Transmissão:",      obterTransmissao(veiculo));
        adicionarCampo(grd, 6,  "Lotação:",          obterLotacao(veiculo));
        adicionarCampo(grd, 7,  "Quilometragem:",    obterQuilometragem(veiculo));
        adicionarCampo(grd, 8,  "Localização:",      veiculo.getLocalizacao());
        adicionarCampo(grd, 9,  "Preço/Dia:",        String.format("%.2f €", veiculo.getPrecoDiario()));
        adicionarCampo(grd, 10, "Estado:",           veiculo.getEstado());
        if (veiculo.getTipoVeiculo() != null && !veiculo.getTipoVeiculo().isBlank()) {
            adicionarCampo(grd, 11, "Tipo de veículo:", veiculo.getTipoVeiculo());
        }

        // ============================
        // AVALIAÇÕES — veículo e proprietário
        // ============================
        Label lblAvaliacoes = new Label("Avaliações");
        lblAvaliacoes.getStyleClass().add("section-titulo");

        HBox painelAvaliacoes = construirPainelAvaliacoes(veiculo);

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
        // RODAPÉ — Voltar (depende da origem) + Reservar (condicional)
        // ============================
        Button btnVoltar = new Button("← Voltar");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> {
            if (origem == Origem.PROCURAR_VEICULOS) {
                NavigationManager.getInstance().navegarParaProcurarVeiculos();
            } else {
                NavigationManager.getInstance().navegarParaMeusVeiculos();
            }
        });

        HBox rodape = new HBox(12, btnVoltar);
        rodape.setAlignment(Pos.CENTER_LEFT);

        boolean ehProprioVeiculo = isDono(veiculo);
        if (origem == Origem.PROCURAR_VEICULOS && !ehProprioVeiculo) {
            Button btnReservar = new Button("Reservar este veículo");
            btnReservar.getStyleClass().add("btn-primario");
            btnReservar.setOnAction(e -> abrirReserva(veiculo));
            rodape.getChildren().add(btnReservar);
        }

        if (ehProprioVeiculo) {
            Button btnEditar = new Button("✎ Editar Veículo");
            btnEditar.getStyleClass().add("btn-primario");
            btnEditar.setOnAction(e ->
                NavigationManager.getInstance().navegarParaEditarVeiculo(veiculo)
            );
            rodape.getChildren().add(btnEditar);
        }

        root.getChildren().addAll(
            titulo, subtitulo,
            carrossel,
            new Separator(),
            lblEspecs, grd,
            new Separator(),
            lblAvaliacoes, painelAvaliacoes,
            new Separator(),
            lblHistorico, tabelaHistorico,
            new Separator(),
            rodape
        );
    }

    /** Verifica se o utilizador autenticado é o dono do veículo (para esconder o botão Reservar). */
    private boolean isDono(Veiculo veiculo) {
        com.aluguer.model.User user = com.aluguer.util.SessionManager.getInstance().getUtilizador();
        return user != null && user.getId() == veiculo.getProprietarioId();
    }

    /** Abre o ecrã de criação de reserva para este veículo. */
    private void abrirReserva(Veiculo veiculo) {
        com.aluguer.model.User user = com.aluguer.util.SessionManager.getInstance().getUtilizador();
        if (user == null) {
            NavigationManager.getInstance().navegarParaLogin();
            return;
        }

        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            com.aluguer.controller.ReservaService service = new com.aluguer.controller.ReservaService(conn);

            double[] combInfo = estimarDadosCombustivel(veiculo.getCombustivel());

            CriarReservaView view = new CriarReservaView(
                service,
                user.getId(),
                veiculo.getId(),
                veiculo.getPrecoDiario(),
                combInfo[0],
                combInfo[1],
                0,
                user.getSaldo().doubleValue(),
                veiculo.getMarca() + " " + veiculo.getModelo() + " (" + veiculo.getAno() + ")"
            );

            NavigationManager.getInstance().navegarParaCriarReserva(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private double[] estimarDadosCombustivel(String tipo) {
        if (tipo == null) return new double[]{7.0, 1.75};
        switch (tipo.toLowerCase().trim()) {
            case "diesel":            return new double[]{6.0, 1.60};
            case "elétrico":
            case "eletrico":          return new double[]{18.0, 0.25};
            case "híbrido":
            case "hibrido":           return new double[]{4.5, 1.75};
            default:                  return new double[]{7.0, 1.75};
        }
    }

    // ============================
    // CARROSSEL DE FOTOS
    // ============================

    private javafx.scene.Node construirCarrosselFotos(Veiculo veiculo) {
        List<byte[]> fotos = veiculo.getFotos();

        StackPane palco = new StackPane();
        palco.setPrefSize(600, 340);
        palco.setMaxWidth(600);
        palco.setStyle(
            "-fx-background-color: #f0f0f0;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        if (fotos.isEmpty()) {
            Label semFoto = new Label("Sem fotos disponíveis");
            semFoto.setStyle("-fx-font-size: 14px; -fx-text-fill: #999999;");
            palco.getChildren().add(semFoto);

            VBox container = new VBox(palco);
            container.setAlignment(Pos.CENTER);
            return container;
        }

        ImageView imageView = new ImageView();
        imageView.setFitWidth(600);
        imageView.setFitHeight(340);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        int[] indiceAtual = {0};
        Runnable[] atualizarRef = new Runnable[1];

        HBox pontosBox = new HBox(8);
        pontosBox.setAlignment(Pos.CENTER);
        List<Label> pontos = new java.util.ArrayList<>();
        if (fotos.size() > 1) {
            for (int i = 0; i < fotos.size(); i++) {
                Label ponto = new Label("●");
                ponto.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");
                pontos.add(ponto);
                pontosBox.getChildren().add(ponto);
            }
        }

        Runnable atualizar = () -> {
            byte[] dados = fotos.get(indiceAtual[0]);
            imageView.setImage(new Image(new ByteArrayInputStream(dados)));
            for (int i = 0; i < pontos.size(); i++) {
                pontos.get(i).setStyle(
                    "-fx-font-size: 12px; -fx-text-fill: "
                    + (i == indiceAtual[0] ? "#1a237e" : "#cccccc") + ";"
                );
            }
        };
        atualizarRef[0] = atualizar;
        atualizar.run();

        palco.getChildren().add(imageView);

        if (fotos.size() > 1) {
            Button btnAnterior = new Button("‹");
            btnAnterior.setStyle(
                "-fx-font-size: 22px; -fx-background-color: rgba(255,255,255,0.85);" +
                "-fx-text-fill: #1a237e; -fx-background-radius: 50%; -fx-min-width: 38px;" +
                "-fx-min-height: 38px; -fx-cursor: hand;"
            );
            btnAnterior.setOnAction(e -> {
                indiceAtual[0] = (indiceAtual[0] - 1 + fotos.size()) % fotos.size();
                atualizarRef[0].run();
            });

            Button btnSeguinte = new Button("›");
            btnSeguinte.setStyle(
                "-fx-font-size: 22px; -fx-background-color: rgba(255,255,255,0.85);" +
                "-fx-text-fill: #1a237e; -fx-background-radius: 50%; -fx-min-width: 38px;" +
                "-fx-min-height: 38px; -fx-cursor: hand;"
            );
            btnSeguinte.setOnAction(e -> {
                indiceAtual[0] = (indiceAtual[0] + 1) % fotos.size();
                atualizarRef[0].run();
            });

            StackPane.setAlignment(btnAnterior, Pos.CENTER_LEFT);
            StackPane.setMargin(btnAnterior, new Insets(0, 0, 0, 12));
            StackPane.setAlignment(btnSeguinte, Pos.CENTER_RIGHT);
            StackPane.setMargin(btnSeguinte, new Insets(0, 12, 0, 0));

            palco.getChildren().addAll(btnAnterior, btnSeguinte);
        }

        VBox container = new VBox(10, palco, pontosBox);
        container.setAlignment(Pos.CENTER);
        return container;
    }

    // ============================
    // AVALIAÇÕES
    // ============================

    private HBox construirPainelAvaliacoes(Veiculo veiculo) {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER_LEFT);

        double mediaVeiculo = -1;
        int totalVeiculo = 0;
        double mediaProprietario = -1;
        int totalProprietario = 0;
        String nomeProprietario = "Proprietário #" + veiculo.getProprietarioId();

        try {
            AvaliacaoService avaliacaoService = new AvaliacaoService();
            mediaVeiculo = avaliacaoService.getMediaVeiculo(veiculo.getId());
            totalVeiculo = avaliacaoService.getTotalAvaliacoesVeiculo(veiculo.getId());
            mediaProprietario = avaliacaoService.getMediaProprietario(veiculo.getProprietarioId());
            totalProprietario = avaliacaoService.getTotalAvaliacoesProprietario(veiculo.getProprietarioId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            Optional<User> proprietario = new UserDAO().findById(veiculo.getProprietarioId());
            if (proprietario.isPresent()) {
                nomeProprietario = proprietario.get().getNome();
            }
        } catch (Exception ignored) {
        }

        VBox cardVeiculo = criarCardAvaliacao(
            "Este veículo", mediaVeiculo, totalVeiculo
        );

        Button btnVerTodas = new Button("Ver todas as avaliações");
        btnVerTodas.getStyleClass().add("btn-secundario");
        btnVerTodas.setOnAction(e -> NavigationManager.getInstance().navegarParaAvaliacoes(
            veiculo.getId(),
            veiculo.getMarca() + " " + veiculo.getModelo() + " (" + veiculo.getAno() + ")"
        ));
        cardVeiculo.getChildren().add(btnVerTodas);

        VBox cardProprietario = criarCardAvaliacao(
            "Proprietário: " + nomeProprietario, mediaProprietario, totalProprietario
        );

        box.getChildren().addAll(cardVeiculo, cardProprietario);
        return box;
    }

    private VBox criarCardAvaliacao(String titulo, double media, int total) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(260);
        card.setStyle(
            "-fx-background-color: #f8f9fb;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        lblTitulo.setWrapText(true);

        if (media < 0) {
            Label lblSemNota = new Label("Ainda sem avaliações");
            lblSemNota.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
            card.getChildren().addAll(lblTitulo, lblSemNota);
            return card;
        }

        Label lblMedia = new Label(String.format("%.1f / 5.0", media));
        lblMedia.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        int cheias = (int) Math.round(media);
        StringBuilder estrelas = new StringBuilder();
        for (int i = 0; i < 5; i++) estrelas.append(i < cheias ? "★" : "☆");
        Label lblEstrelas = new Label(estrelas.toString());
        lblEstrelas.setStyle("-fx-font-size: 18px; -fx-text-fill: #f4c542;");

        Label lblTotal = new Label(total + " avaliação" + (total != 1 ? "ões" : ""));
        lblTotal.setStyle("-fx-font-size: 11px; -fx-text-fill: #777777;");

        card.getChildren().addAll(lblTitulo, lblMedia, lblEstrelas, lblTotal);
        return card;
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

    private String obterTransmissao(Veiculo v) {
        return v.getTransmissao() != null ? v.getTransmissao() : "—";
    }

    private String obterLotacao(Veiculo v) {
        return v.getLugares() > 0 ? v.getLugares() + " lugares" : "—";
    }

    private String obterQuilometragem(Veiculo v) {
        return v.getQuilometragem() > 0 ? v.getQuilometragem() + " km" : "—";
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