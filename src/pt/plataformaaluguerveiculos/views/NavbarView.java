package pt.plataformaaluguerveiculos.views;

import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

public class NavbarView {
    private HBox navbar;

    public NavbarView() {
        navbar = new HBox();

        Button btnDashboard = new Button("Dashboard");
        Button btnProcurarVeiculos = new Button("Procurar Veículos");
        Button btnReservas = new Button("As Minhas Reservas");
        Button btnPerfil = new Button("Perfil");
        Button btnSair = new Button("Sair");

        navbar.getChildren().addAll(btnDashboard, btnProcurarVeiculos, btnReservas, btnPerfil, btnSair);
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(10,20,10,20));
    }

    public HBox getNavbar() {

        return navbar;
    }
}
