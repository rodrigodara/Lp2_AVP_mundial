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

    /**
     * @param veiculoId   id do veículo cujas avaliações serão mostradas
     * @param nomeVeiculo nome a exibir no título (ex: "Toyota Corolla (2021)")
     */
    public AvaliacoesView(int veiculoId, String nomeVeiculo) {
        root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label titulo = new Label("Avaliações de " + nomeVeiculo);
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        Button btnVoltar = new Button("← Voltar");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaDashboard());

        root.getChildren().addAll(titulo, btnVoltar);

        try {
            AvaliacaoService service = new AvaliacaoService();
            double media = service.getMediaVeiculo(veiculoId);
            List<Avaliacao> lista = service.getAvaliacoesVeiculo(veiculoId);

            // Painel de média
            VBox painelMedia = criarPainelMedia(media, lista.size());
            root.getChildren().add(painelMedia);

            if (lista.isEmpty()) {
                Label vazio = new Label("Ainda não tem avaliações.");
                vazio.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
                root.getChildren().add(vazio);
                return;
            }

            Label labelLista = new Label("Todas as avaliações");
            labelLista.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

            VBox listaCards = new VBox(12);
            for (Avaliacao a : lista) {
                listaCards.getChildren().add(criarCardAvaliacao(a));
            }

            ScrollPane scroll = new ScrollPane(listaCards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: #F8FAFC; -fx-background: #F8FAFC;");

            root.getChildren().addAll(labelLista, scroll);

        } catch (Exception e) {
            e.printStackTrace();
            Label erro = new Label("Erro ao carregar avaliações.");
            erro.setStyle("-fx-text-fill: #EF4444;");
            root.getChildren().add(erro);
        }
    }

    private VBox criarPainelMedia(double media, int total) {
        VBox painel = new VBox(8);
        painel.setPadding(new Insets(20));
        painel.setMaxWidth(340);
        painel.setStyle(
            "-fx-background-color: #EAF2FF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #93C5FD;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        if (media < 0) {
            Label semNota = new Label("Sem avaliações ainda");
            semNota.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
            painel.getChildren().add(semNota);
            return painel;
        }

        Label lblMedia = new Label(String.format("%.1f / 5.0", media));
        lblMedia.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        Label lblEstrelas = new Label(estrelasTexto(media));
        lblEstrelas.setStyle("-fx-font-size: 22px; -fx-text-fill: #F59E0B;");

        Label lblTotal = new Label("Baseado em " + total + " avaliação" + (total != 1 ? "ões" : ""));
        lblTotal.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        painel.getChildren().addAll(lblMedia, lblEstrelas, lblTotal);
        return painel;
    }

    private VBox criarCardAvaliacao(Avaliacao a) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);"
        );

        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label lblEstrelas = new Label(estrelasTexto(a.getClassificacao()));
        lblEstrelas.setStyle("-fx-font-size: 18px; -fx-text-fill: #F59E0B;");

        Label lblNota = new Label(a.getClassificacao() + "/5");
        lblNota.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        topo.getChildren().addAll(lblEstrelas, lblNota);

        card.getChildren().add(topo);

        if (a.getComentario() != null && !a.getComentario().isBlank()) {
            Label lblComentario = new Label("\"" + a.getComentario() + "\"");
            lblComentario.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937; -fx-font-style: italic;");
            lblComentario.setWrapText(true);
            card.getChildren().add(lblComentario);
        }

        if (a.getDataAvaliacao() != null) {
            Label lblData = new Label(a.getDataAvaliacao().toLocalDate().toString());
            lblData.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
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