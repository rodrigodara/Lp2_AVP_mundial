package pt.plataformaaluguerveiculos;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import pt.plataformaaluguerveiculos.views.BaseLayoutView;
import pt.plataformaaluguerveiculos.views.LoginView;
import pt.plataformaaluguerveiculos.views.NavigationManager;
import pt.plataformaaluguerveiculos.views.NavbarView;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Plataforma de Aluguer de Veículos");

        // ALV-XX (redesign): carregar as fontes do redesign (Flamenco = títulos, Federo = corpo)
        // antes de aplicar o styles.css, para que -fx-font-family: "Flamenco"/"Federo" funcione.
        carregarFontes();

        // ALV-58: layout base com navbar
        BaseLayoutView baseLayout = new BaseLayoutView();

        // ALV-57: inicializa o NavigationManager com o layout base
        NavigationManager nav = NavigationManager.getInstance();
        nav.init(baseLayout);

        // ALV-59: começa no Login (sem navbar visível)
        baseLayout.getRoot().setTop(null);
        LoginView loginView = new LoginView();
        baseLayout.setContent(loginView.getRoot());

        Scene scene = new Scene(baseLayout.getRoot(), 900, 650);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Regista as fontes do redesign no JavaFX. O CSS por si só não consegue
     * carregar @font-face com ficheiros locais de forma fiável em todas as
     * plataformas, por isso usamos Font.loadFont — depois disso, qualquer
     * "-fx-font-family: Flamenco;" ou "-fx-font-family: Federo;" no
     * styles.css passa a funcionar normalmente.
     */
    private void carregarFontes() {
        Font flamenco = Font.loadFont(
            getClass().getResourceAsStream("/fonts/Flamenco-Regular.ttf"), 14
        );
        Font federo = Font.loadFont(
            getClass().getResourceAsStream("/fonts/Federo-Regular.ttf"), 14
        );
        if (flamenco == null) {
            System.err.println("Aviso: não foi possível carregar a fonte Flamenco (/fonts/Flamenco-Regular.ttf).");
        }
        if (federo == null) {
            System.err.println("Aviso: não foi possível carregar a fonte Federo (/fonts/Federo-Regular.ttf).");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
