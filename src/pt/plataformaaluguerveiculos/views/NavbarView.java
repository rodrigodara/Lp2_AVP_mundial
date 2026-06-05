package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * ALV-56  — Navbar da aplicação.
 * ALV-90  — Adicionado botão "Pedidos Recebidos" ligado ao NavigationManager.
 * ALV-100 — Adicionado botão "As Minhas Reservas" ligado ao NavigationManager.
 */
public class NavbarView {

    private HBox navbar;

    public NavbarView() {
        navbar = new HBox();

        Button btnDashboard        = new Button("Dashboard");
        Button btnProcurarVeiculos = new Button("Procurar Veículos");
        Button btnReservas         = new Button("As Minhas Reservas");
        Button btnPedidos          = new Button("Pedidos Recebidos");
        Button btnPerfil           = new Button("Perfil");
        Button btnSair             = new Button("Sair");

        // Estilos
        for (Button btn : new Button[]{btnDashboard, btnProcurarVeiculos,
                                       btnReservas, btnPedidos, btnPerfil, btnSair}) {
            btn.getStyleClass().add("navbar-button");
        }

        // Ações de navegação
        btnDashboard.setOnAction(e ->
            NavigationManager.getInstance().navegarParaDashboard());

        // ALV-100: navegar para as minhas reservas
        btnReservas.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMinhasReservas());

        // ALV-90: navegar para pedidos recebidos
        btnPedidos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaPedidosRecebidos());

        btnSair.setOnAction(e ->
            NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(
            btnDashboard, btnProcurarVeiculos, btnReservas, btnPedidos, btnPerfil, btnSair
        );
        navbar.getStyleClass().add("navbar");
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(10, 20, 10, 20));
    }

    public HBox getNavbar() {
        return navbar;
    }
}