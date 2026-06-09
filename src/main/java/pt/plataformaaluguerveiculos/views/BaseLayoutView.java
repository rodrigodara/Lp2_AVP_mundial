package pt.plataformaaluguerveiculos.views;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

/**
 * ALV-58 - Estrutura base de layout (header + content)
 * Reutilizada por todas as páginas autenticadas.
 */
public class BaseLayoutView {

    private BorderPane root;
    private NavbarView navbarView;
    private ScrollPane scrollPane;

    public BaseLayoutView() {
        root = new BorderPane();
        navbarView = new NavbarView();
        root.setTop(navbarView.getNavbar());

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("reserva-scroll");
        root.setCenter(scrollPane);
    }

    public void setContent(Node content) {
        scrollPane.setContent(content);
    }

    public BorderPane getRoot() {
        return root;
    }

    public NavbarView getNavbarView() {
        return navbarView;
    }
}