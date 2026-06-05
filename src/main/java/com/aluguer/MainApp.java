package com.aluguer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import pt.plataformaaluguerveiculos.views.BaseLayoutView;
import pt.plataformaaluguerveiculos.views.LoginView;
import pt.plataformaaluguerveiculos.views.NavigationManager;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Plataforma de Aluguer de Veículos");

        BaseLayoutView baseLayout = new BaseLayoutView();

        NavigationManager nav = NavigationManager.getInstance();
        nav.init(baseLayout);

        baseLayout.getRoot().setTop(null);

        LoginView loginView = new LoginView();
        baseLayout.setContent(loginView.getRoot());

        Scene scene = new Scene(baseLayout.getRoot(), 900, 650);

        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}