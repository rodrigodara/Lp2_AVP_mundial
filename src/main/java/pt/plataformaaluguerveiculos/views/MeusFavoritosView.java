package pt.plataformaaluguerveiculos.views;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.aluguer.model.Veiculo;
import com.aluguer.service.FavoritoService;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Lista de veículos marcados como favoritos pelo utilizador logado.
 * Segue o mesmo estilo de cards usado em ProcurarVeiculosView, mas sem
 * paginação/filtros (lista normalmente pequena) e com botão para remover
 * o veículo dos favoritos diretamente a partir desta vista.
 */
public class MeusFavoritosView {

    private static final double LARGURA_CARD = 230;
    private static final double ALTURA_FOTO = 150;

    private final VBox root;
    private final FlowPane gridCards;
    private final Label lblVazio;
    private final FavoritoService favoritoService;

    public MeusFavoritosView() {
        favoritoService = new FavoritoService();

        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Os Meus Favoritos");
        titulo.getStyleClass().add("dashboard-titulo");

        gridCards = new FlowPane();
        gridCards.setHgap(18);
        gridCards.setVgap(18);
        gridCards.setPadding(new Insets(4));
        gridCards.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(gridCards);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(560);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        lblVazio = new Label("Ainda não marcaste nenhum veículo como favorito.");
        lblVazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");
        lblVazio.setVisible(false);
        lblVazio.setManaged(false);

        carregarFavoritos();

        root.getChildren().addAll(titulo, lblVazio, scroll);
    }

    private void carregarFavoritos() {
        int userId = SessionManager.getInstance().getUtilizador().getId();
        try {
            List<Veiculo> favoritos = favoritoService.listarVeiculosFavoritos(userId);

            gridCards.getChildren().clear();
            for (Veiculo v : favoritos) {
                gridCards.getChildren().add(criarCardVeiculo(v));
            }

            boolean vazio = favoritos.isEmpty();
            lblVazio.setVisible(vazio);
            lblVazio.setManaged(vazio);
        } catch (Exception ex) {
            ex.printStackTrace();
            gridCards.getChildren().clear();
            lblVazio.setText("Não foi possível carregar os favoritos.");
            lblVazio.setVisible(true);
            lblVazio.setManaged(true);
        }
    }

    private VBox criarCardVeiculo(Veiculo v) {
        // ---- Foto (ou placeholder) ----
        StackPane fotoBox = new StackPane();
        fotoBox.setPrefSize(LARGURA_CARD, ALTURA_FOTO);
        fotoBox.setMinSize(LARGURA_CARD, ALTURA_FOTO);
        fotoBox.setMaxSize(LARGURA_CARD, ALTURA_FOTO);

        byte[] foto = v.getFoto1();
        if (foto != null && foto.length > 0) {
            ImageView imageView = new ImageView();
            imageView.setFitWidth(LARGURA_CARD);
            imageView.setFitHeight(ALTURA_FOTO);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            try {
                imageView.setImage(new Image(new ByteArrayInputStream(foto)));
            } catch (Exception ex) {
                imageView.setImage(null);
            }
            fotoBox.getChildren().add(imageView);
        } else {
            fotoBox.setStyle("-fx-background-color: #e8eaf6;");
            Label semFoto = new Label("Sem foto disponível");
            semFoto.setStyle("-fx-text-fill: #9fa8da; -fx-font-size: 12px;");
            fotoBox.getChildren().add(semFoto);
        }

        // ---- Bloco de informação ----
        Label lblNome = new Label(v.getMarca() + " " + v.getModelo());
        lblNome.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
        lblNome.setWrapText(true);

        Label lblAno = new Label(String.valueOf(v.getAno()) + " · " + v.getCombustivel());
        lblAno.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        Label lblLocal = new Label("📍 " + v.getLocalizacao());
        lblLocal.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        double media = v.getAvaliacaoMedia();
        String textoAval = media < 0 ? " Sem avaliações" : String.format(" %.1f ★", media);
        Label lblAval = new Label(textoAval);
        lblAval.setStyle("-fx-font-size: 12px; -fx-text-fill: #f9a825; -fx-font-weight: bold;");

        Label lblPreco = new Label(String.format("%.0f €/dia", v.getPrecoDiario()));
        lblPreco.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        HBox linhaPrecoAval = new HBox(lblPreco);
        linhaPrecoAval.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblPreco, Priority.ALWAYS);
        linhaPrecoAval.getChildren().add(lblAval);

        Button btnVerDetalhes = new Button("Ver Detalhes");
        btnVerDetalhes.getStyleClass().add("btn-primario");
        btnVerDetalhes.setMaxWidth(Double.MAX_VALUE);
        btnVerDetalhes.setOnAction(e -> abrirDetalhe(v));

        // ---- Botão para remover dos favoritos ----
        Button btnRemover = new Button("★ Remover dos Favoritos");
        btnRemover.getStyleClass().add("btn-secundario");
        btnRemover.setMaxWidth(Double.MAX_VALUE);
        btnRemover.setOnAction(e -> {
            e.consume(); // não abrir o detalhe ao clicar neste botão
            removerFavorito(v);
        });

        VBox infoBox = new VBox(6, lblNome, lblAno, lblLocal, linhaPrecoAval, btnVerDetalhes, btnRemover);
        infoBox.setPadding(new Insets(12));

        // ---- Conteúdo do card (foto + info), com cantos arredondados ----
        VBox conteudo = new VBox(fotoBox, infoBox);
        conteudo.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(conteudo.widthProperty());
        clip.heightProperty().bind(conteudo.heightProperty());
        conteudo.setClip(clip);

        // ---- Wrapper externo: só aqui vai a sombra (fora do clip) ----
        VBox card = new VBox(conteudo);
        card.setPrefWidth(LARGURA_CARD);
        card.setMaxWidth(LARGURA_CARD);
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> abrirDetalhe(v));

        return card;
    }

    private void abrirDetalhe(Veiculo veiculo) {
        NavigationManager.getInstance().navegarParaDetalheVeiculo(
            veiculo, DetalheVeiculoView.Origem.PROCURAR_VEICULOS
        );
    }

    private void removerFavorito(Veiculo veiculo) {
        int userId = SessionManager.getInstance().getUtilizador().getId();
        try {
            favoritoService.alternar(userId, veiculo.getId());
            carregarFavoritos();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public VBox getRoot() {
        return root;
    }
}