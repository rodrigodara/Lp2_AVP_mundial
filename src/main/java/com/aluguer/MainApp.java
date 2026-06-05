package com.aluguer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Aluguer de Veículos");
        showLogin();
        primaryStage.show();
    }

    // ------------------------------------------------------------------
    // Navegação
    // ------------------------------------------------------------------

    public static void showLogin() {
        loadScene("/view/login.fxml", 800, 600);
    }

    public static void showRegisto() {
        loadScene("/view/registo.fxml", 800, 600);
    }

    // ------------------------------------------------------------------
    // Utilitário interno
    // ------------------------------------------------------------------

    private static void loadScene(String fxmlPath, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load(), width, height);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("[MainApp] Erro ao carregar cena: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}