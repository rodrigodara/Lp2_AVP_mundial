package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.User;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class NavbarView {

    private final HBox navbar = new HBox();

    public NavbarView() {
        navbar.getStyleClass().add("navbar");
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        construir();
    }

    public void atualizar() {
        construir();
    }

    private void construir() {
        navbar.getChildren().clear();

        User user = SessionManager.getInstance().getUtilizador();

        // 🔒 utilizador não autenticado
        if (user == null) {
            construirNavbarAnonimo();
            return;
        }

        // admin vs utilizador normal
        if (user.isAdministrador()) {
            construirNavbarAdmin();
        } else {
            construirNavbarUtilizador();
        }
    }

    // =====================================================
    // NAVBAR ANÓNIMO
    // =====================================================
    private void construirNavbarAnonimo() {

        Button btnLogin = new Button("Login");
        Button btnRegisto = new Button("Registo");

        btnLogin.getStyleClass().add("navbar-button");
        btnRegisto.getStyleClass().add("navbar-button");

        btnLogin.setOnAction(e ->
            NavigationManager.getInstance().navegarParaLogin());

        btnRegisto.setOnAction(e ->
            NavigationManager.getInstance().navegarParaRegisto());

        navbar.getChildren().addAll(btnLogin, btnRegisto);
    }

    // =====================================================
    // NAVBAR ADMIN
    // =====================================================
    private void construirNavbarAdmin() {

        Button btnPainel = new Button("Painel de Administração");
        Button btnSair = new Button("Sair");

        btnPainel.getStyleClass().add("navbar-button");
        btnSair.getStyleClass().add("navbar-button");

        btnPainel.setOnAction(e ->
            NavigationManager.getInstance().navegarParaAdmin());

        btnSair.setOnAction(e ->
            NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(btnPainel, btnSair);
    }

    // =====================================================
    // NAVBAR UTILIZADOR
    // =====================================================
    private void construirNavbarUtilizador() {

        User user = SessionManager.getInstance().getUtilizador();

        // segurança extra (boa prática)
        if (user == null) {
            return;
        }

        Button btnDashboard = new Button("Dashboard");
        Button btnProcurar = new Button("Procurar Veículos");
        Button btnReservas = new Button("As Minhas Reservas");
        Button btnMeusVeiculos = new Button("Os Meus Veículos");
        Button btnPedidos = new Button("Pedidos Recebidos");
        Button btnConta = new Button("Conta");
        Button btnHistorico = new Button("Histórico Veículos");
        Button btnNotificacoes = new Button("🔔");
        Button btnSair = new Button("Sair");

        for (Button b : new Button[]{
                btnDashboard, btnProcurar, btnReservas,
                btnMeusVeiculos, btnPedidos, btnConta,
                btnHistorico, btnNotificacoes, btnSair
        }) {
            b.getStyleClass().add("navbar-button");
        }

        btnDashboard.setOnAction(e ->
            NavigationManager.getInstance().navegarParaDashboard());

        btnProcurar.setOnAction(e ->
            NavigationManager.getInstance().navegarParaProcurarVeiculos());

        btnReservas.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMinhasReservas());

        btnMeusVeiculos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMeusVeiculos());

        btnPedidos.setOnAction(e ->
            NavigationManager.getInstance().navegarParaPedidosRecebidos());

        btnConta.setOnAction(e ->
            NavigationManager.getInstance().navegarParaConta());

        btnHistorico.setOnAction(e ->
            NavigationManager.getInstance().navegarParaHistoricoVeiculos());

btnNotificacoes.setOnAction(e -> {
    SinhoNotificacoesView sino = NavigationManager.getInstance().getSinoView();
    if (sino != null) {
        sino.mostrarPopupAncoradoA(btnNotificacoes);
    }
});

        btnSair.setOnAction(e ->
            NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(
            btnDashboard,
            btnProcurar,
            btnReservas,
            btnMeusVeiculos,
            btnPedidos,
            btnConta,
            btnHistorico,
            btnNotificacoes,
            btnSair
        );
    }

    public HBox getNavbar() {
        return navbar;
    }
}