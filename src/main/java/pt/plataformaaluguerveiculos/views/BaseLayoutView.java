package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class BaseLayoutView {

    private BorderPane root;
    private NavbarView navbarView;
    private ScrollPane scrollPane;

    public BaseLayoutView() {
        root = new BorderPane();
        root.getStyleClass().add("root");

        navbarView = new NavbarView();
        root.setTop(navbarView.getNavbar());

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);   // preenche altura toda
        scrollPane.getStyleClass().add("scroll-pane");
        root.setCenter(scrollPane);
    }

    public void setContent(Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.getStyleClass().add("root");
        // Garante que o wrapper cresce com o ScrollPane
        wrapper.setMinHeight(ScrollPane.USE_COMPUTED_SIZE);
        scrollPane.setContent(wrapper);
    }

    public BorderPane getRoot() { return root; }
    public NavbarView getNavbarView() { return navbarView; }
}
