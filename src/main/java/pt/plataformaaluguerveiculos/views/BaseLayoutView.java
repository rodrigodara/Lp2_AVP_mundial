package pt.plataformaaluguerveiculos.views;

import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

/**
 * ALV-58 - Estrutura base de layout (header + content)
 * Reutilizada por todas as páginas autenticadas.
 */
public class BaseLayoutView {

    private BorderPane root;
    private NavbarView navbarView;

    public BaseLayoutView() {
        root = new BorderPane();
        navbarView = new NavbarView();
        root.setTop(navbarView.getNavbar());
    }

    public void setContent(Node content) {
        root.setCenter(content);
    }

    public BorderPane getRoot() {
        return root;
    }

    public NavbarView getNavbarView() {
        return navbarView;
    }
}
