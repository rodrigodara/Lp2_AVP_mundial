package pt.plataformaaluguerveiculos;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.plataformaaluguerveiculos.views.BaseLayoutView;
import pt.plataformaaluguerveiculos.views.LoginView;
import pt.plataformaaluguerveiculos.views.NavigationManager;
import pt.plataformaaluguerveiculos.views.NavbarView;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Plataforma de Aluguer de Veículos");

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

    public static void main(String[] args) {
        launch(args);
    }
}
