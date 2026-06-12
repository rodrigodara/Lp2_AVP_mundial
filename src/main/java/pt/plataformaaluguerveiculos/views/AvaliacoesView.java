package pt.plataformaaluguerveiculos.views;

import java.util.List;

import com.aluguer.model.Avaliacao;
import com.aluguer.service.AvaliacaoService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AvaliacoesView {

    private VBox root;

    public AvaliacoesView(int avaliadoId, String nomeAvaliado) {
        root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: white;");

        Label titulo = new Label("Avaliações de " + nomeAvaliado);
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Button btnVoltar = new Button("← Voltar");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaDashboard());

        root.getChildren().addAll(titulo, btnVoltar);

        try {
            AvaliacaoService service = new AvaliacaoService();
            double media = service.getMedia(avaliadoId);
            List<Avaliacao> lista = service.getAvaliacoes(avaliadoId);

            // Painel de média
            VBox painelMedia = criarPainelMedia(media, lista.size());
            root.getChildren().add(painelMedia);

            if (lista.isEmpty()) {
                Label vazio = new Label("Ainda não tem avaliações.");
                vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-font-style: italic;");
                root.getChildren().add(vazio);
                return;
            }

            Label labelLista = new Label("Todas as avaliações");
            labelLista.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");

            VBox listaCards = new VBox(12);
            for (Avaliacao a : lista) {
                listaCards.getChildren().add(criarCardAvaliacao(a));
            }

            ScrollPane scroll = new ScrollPane(listaCards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: white; -fx-background: white;");

            root.getChildren().addAll(labelLista, scroll);

        } catch (Exception e) {
            e.printStackTrace();
            Label erro = new Label("Erro ao carregar avaliações.");
            erro.setStyle("-fx-text-fill: #c62828;");
            root.getChildren().add(erro);
        }
    }

    private VBox criarPainelMedia(double media, int total) {
        VBox painel = new VBox(8);
        painel.setPadding(new Insets(20));
        painel.setMaxWidth(340);
        painel.setStyle(
            "-fx-background-color: #e8eaf6;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #c5cae9;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        if (media < 0) {
            Label semNota = new Label("Sem avaliações ainda");
            semNota.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777;");
            painel.getChildren().add(semNota);
            return painel;
        }

        Label lblMedia = new Label(String.format("%.1f / 5.0", media));
        lblMedia.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label lblEstrelas = new Label(estrelasTexto(media));
        lblEstrelas.setStyle("-fx-font-size: 22px; -fx-text-fill: #f4c542;");

        Label lblTotal = new Label("Baseado em " + total + " avaliação" + (total != 1 ? "ões" : ""));
        lblTotal.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");

        painel.getChildren().addAll(lblMedia, lblEstrelas, lblTotal);
        return painel;
    }

    private VBox criarCardAvaliacao(Avaliacao a) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);"
        );

        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label lblEstrelas = new Label(estrelasTexto(a.getNota()));
        lblEstrelas.setStyle("-fx-font-size: 18px; -fx-text-fill: #f4c542;");

        Label lblNota = new Label(a.getNota() + "/5");
        lblNota.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        String tipoTexto = a.getTipo() == Avaliacao.TipoAvaliado.PROPRIETARIO
            ? "por Locatário" : "por Proprietário";
        Label lblTipo = new Label(tipoTexto);
        lblTipo.setStyle("-fx-font-size: 11px; -fx-text-fill: #999999;");

        topo.getChildren().addAll(lblEstrelas, lblNota, lblTipo);

        card.getChildren().add(topo);

        if (a.getComentario() != null && !a.getComentario().isBlank()) {
            Label lblComentario = new Label("\"" + a.getComentario() + "\"");
            lblComentario.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555; -fx-font-style: italic;");
            lblComentario.setWrapText(true);
            card.getChildren().add(lblComentario);
        }

        if (a.getDataCriacao() != null) {
            Label lblData = new Label(a.getDataCriacao().toLocalDate().toString());
            lblData.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa;");
            card.getChildren().add(lblData);
        }

        return card;
    }

    private String estrelasTexto(double nota) {
        int cheias = (int) Math.round(nota);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < cheias ? "★" : "☆");
        return sb.toString();
    }

    public VBox getRoot() {
        return root;
    }
}
