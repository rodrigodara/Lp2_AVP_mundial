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

        // Layout base com navbar
        BaseLayoutView baseLayout = new BaseLayoutView();

        // Inicializar o NavigationManager com o layout base
        NavigationManager nav = NavigationManager.getInstance();
        nav.init(baseLayout);

        // Começar no Login (sem navbar visível)
        baseLayout.getRoot().setTop(null);
        LoginView loginView = new LoginView();
        baseLayout.setContent(loginView.getRoot());

        Scene scene = new Scene(baseLayout.getRoot(), 900, 650);
        scene.getStylesheets().add(
            getClass().getResource("/resources/styles.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}