package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardView {

    private VBox root;

    public DashboardView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(80));
        root.setStyle("-fx-background-color: white;");

        Label titulo = new Label("Bem-vindo ao Dashboard");
        titulo.getStyleClass().add("dashboard-titulo");

        Label subtitulo = new Label("Plataforma de Aluguer de Veículos");
        subtitulo.getStyleClass().add("dashboard-subtitulo");

        root.getChildren().addAll(titulo, subtitulo);
    }

    public VBox getRoot() {
        return root;
    }
}