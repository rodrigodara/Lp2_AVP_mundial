package pt.plataformaaluguerveiculos.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * ALV-56  — Navbar da aplicação.
 * ALV-90  — Adicionado botão "Pedidos Recebidos".
 * ALV-100 — Adicionado botão "As Minhas Reservas".
 * ALV-134 — Adicionado botão "Os Meus Veículos".
 * ALV-118 — Adicionado botão "Conta" ligado à ContaView.
 */
public class NavbarView {

    private HBox navbar;

    public NavbarView() {
        navbar = new HBox();

        Button btnDashboard        = new Button("Dashboard");
        Button btnProcurarVeiculos = new Button("Procurar Veículos");
        Button btnReservas         = new Button("As Minhas Reservas");
        Button btnMeusVeiculos     = new Button("Os Meus Veículos");
        Button btnPedidos          = new Button("Pedidos Recebidos");
        Button btnConta            = new Button("Conta");
        Button btnHistorico        = new Button("Histórico Veículos");
        Button btnSair             = new Button("Sair");

        for (Button btn : new Button[]{btnDashboard, btnProcurarVeiculos,
                                       btnReservas, btnMeusVeiculos,
                                       btnPedidos, btnConta, btnHistorico, btnSair}) {
            btn.getStyleClass().add("navbar-button");
        }

        btnDashboard.setOnAction(e ->
            NavigationManager.getInstance().navegarParaDashboard());

        btnProcurarVeiculos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaProcurarVeiculos());

        btnReservas.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMinhasReservas());

        // ALV-134: navegar para os meus veículos
        btnMeusVeiculos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMeusVeiculos());

        btnPedidos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaPedidosRecebidos());

        btnHistorico.setOnAction(e ->
            NavigationManager.getInstance().navegarParaHistoricoVeiculos());

        // ALV-118: navegar para gestão de conta
        btnConta.setOnAction(e ->
            NavigationManager.getInstance().navegarParaConta());

        btnSair.setOnAction(e ->
            NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(
            btnDashboard, btnProcurarVeiculos, btnReservas,
            btnMeusVeiculos, btnPedidos, btnConta, btnHistorico, btnSair
        );
        navbar.getStyleClass().add("navbar");
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(10, 20, 10, 20));
    }

    public HBox getNavbar() {
        return navbar;
    }
}