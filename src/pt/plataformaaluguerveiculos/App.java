package pt.plataformaaluguerveiculos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.application.Application;
import javafx.stage.Stage;
import pt.plataformaaluguerveiculos.views.NavbarView;

public class App extends Application {

    private NavbarView navbarView;

    @Override
    public void start(Stage stage) {
    stage.setTitle("Plataforma de Aluguer de Veículos");
    BorderPane root = new BorderPane();
    Scene scene = new Scene(root, 800, 600);
    stage.setScene(scene); 
    scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
    navbarView = new NavbarView();
    root.setTop(navbarView.getNavbar());

    stage.show(); 
    }

    public static void main(String[] args) {

        
        launch(args);
    }
}