package com.aluguer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    // Stage estático para navegação entre ecrãs
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Aluguer de Veículos");

        // Abre no Login (já não no Registo)
        
        showLogin();

        primaryStage.show();
    }

    // ------------------------------------------------------------------
    // Navegação
    // ------------------------------------------------------------------

    /** Mostra o ecrã de login. */
    public static void showLogin() {
        loadScene("/view/login.fxml", 800, 600);
    }

    /** Mostra o ecrã de registo. */
    public static void showRegisto() {
        loadScene("/view/registo.fxml", 800, 600);
    }

    /**
     * TODO: adicionar aqui os métodos de navegação para os teus dashboards.
     *
     * Exemplo:
     *   public static void showDashboardProprietario() {
     *       loadScene("/view/dashboard_proprietario.fxml", 1024, 768);
     *   }
     */

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