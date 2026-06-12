package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.Avaliacao;
import com.aluguer.service.AvaliacaoService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AvaliarView {

    private VBox root;
    private int notaSelecionada = 0;

    public AvaliarView(int reservaId, int avaliadorId, int avaliadoId,
                       Avaliacao.TipoAvaliado tipo, String nomeAvaliado) {

        root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: white;");

        // Título
        Label titulo = new Label("Avaliar " + (tipo == Avaliacao.TipoAvaliado.PROPRIETARIO ? "Proprietário" : "Locatário"));
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Label subtitulo = new Label("A avaliar: " + nomeAvaliado + " | Reserva #" + reservaId);
        subtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #777777;");

        // Estrelas
        Label labelNota = new Label("Nota:");
        labelNota.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        Button[] estrelas = new Button[5];
        HBox estrelasBox = new HBox(6);
        estrelasBox.setAlignment(Pos.CENTER_LEFT);

        Label lblNotaTexto = new Label("Selecione uma nota");
        lblNotaTexto.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999; -fx-font-style: italic;");

        for (int i = 0; i < 5; i++) {
            final int valor = i + 1;
            estrelas[i] = new Button("★");
            estrelas[i].setStyle(
                "-fx-font-size: 28px; -fx-background-color: transparent;" +
                "-fx-text-fill: #cccccc; -fx-cursor: hand; -fx-padding: 2;"
            );
            estrelas[i].setOnAction(e -> {
                notaSelecionada = valor;
                atualizarEstrelas(estrelas, valor);
                String[] textos = {"Muito mau", "Mau", "Razoável", "Bom", "Excelente"};
                lblNotaTexto.setText(valor + "/5 — " + textos[valor - 1]);
                lblNotaTexto.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a237e; -fx-font-weight: bold;");
            });
            estrelasBox.getChildren().add(estrelas[i]);
        }

        // Comentário
        Label labelComentario = new Label("Comentário (opcional):");
        labelComentario.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        TextArea campoComentario = new TextArea();
        campoComentario.setPromptText("Escreva a sua opinião sobre esta experiência...");
        campoComentario.setPrefHeight(100);
        campoComentario.setMaxWidth(500);
        campoComentario.setWrapText(true);

        // Mensagem de feedback
        Label lblFeedback = new Label();
        lblFeedback.setVisible(false);

        // Botões
        Button btnSubmeter = new Button("Submeter Avaliação");
        btnSubmeter.getStyleClass().add("btn-primario");

        Button btnVoltar = new Button("Voltar");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setOnAction(e -> NavigationManager.getInstance().navegarParaMinhasReservas());

        btnSubmeter.setOnAction(e -> {
            if (notaSelecionada == 0) {
                lblFeedback.setText("Por favor selecione uma nota.");
                lblFeedback.setStyle("-fx-text-fill: #c62828; -fx-font-size: 13px;");
                lblFeedback.setVisible(true);
                return;
            }

            try {
                AvaliacaoService service = new AvaliacaoService();
                boolean ok = service.avaliar(
                    reservaId, avaliadorId, avaliadoId,
                    tipo, notaSelecionada, campoComentario.getText().trim()
                );

                if (ok) {
                    lblFeedback.setText("Avaliação submetida com sucesso!");
                    lblFeedback.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 13px;");
                    btnSubmeter.setDisable(true);
                } else {
                    lblFeedback.setText("Já avaliou esta reserva.");
                    lblFeedback.setStyle("-fx-text-fill: #c62828; -fx-font-size: 13px;");
                }
                lblFeedback.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                lblFeedback.setText("Erro ao submeter avaliação.");
                lblFeedback.setStyle("-fx-text-fill: #c62828; -fx-font-size: 13px;");
                lblFeedback.setVisible(true);
            }
        });

        HBox botoesBox = new HBox(12, btnSubmeter, btnVoltar);
        botoesBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
            titulo, subtitulo,
            labelNota, estrelasBox, lblNotaTexto,
            labelComentario, campoComentario,
            lblFeedback, botoesBox
        );
    }

    private void atualizarEstrelas(Button[] estrelas, int nota) {
        for (int i = 0; i < 5; i++) {
            String cor = i < nota ? "#f4c542" : "#cccccc";
            estrelas[i].setStyle(
                "-fx-font-size: 28px; -fx-background-color: transparent;" +
                "-fx-text-fill: " + cor + "; -fx-cursor: hand; -fx-padding: 2;"
            );
        }
    }

    public VBox getRoot() {
        return root;
    }
}
