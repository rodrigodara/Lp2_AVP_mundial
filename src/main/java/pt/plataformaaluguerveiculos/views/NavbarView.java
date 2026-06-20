package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.User;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class NavbarView {

    private final HBox navbar = new HBox();

    public NavbarView() {
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        construir();
    }

    public void atualizar() {
        construir();
    }

    private void construir() {
        navbar.getChildren().clear();

        User user = SessionManager.getInstance().getUtilizador();

        Label logo = criarLogo();

        if (user == null) {
            construirNavbarAnonimo(logo);
            return;
        }

        if (user.isAdministrador()) {
            construirNavbarAdmin(logo);
        } else {
            construirNavbarUtilizador(logo);
        }
    }

    /** Logótipo "AVL Mundial", sempre à esquerda da navbar. */
    private Label criarLogo() {
        Label logo = new Label("AVL Mundial");
        logo.getStyleClass().add("navbar-logo");
        return logo;
    }

    /** Pequeno círculo com ícone de utilizador — substitui o antigo botão de texto solto. */
    private StackPane criarAvatar() {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("navbar-avatar");
        Label icone = new Label("\u25CF");
        icone.getStyleClass().add("navbar-avatar-icone");
        avatar.getChildren().add(icone);
        return avatar;
    }

    private Region criarSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    // =====================================================
    // NAVBAR ANÓNIMO
    // =====================================================
    private void construirNavbarAnonimo(Label logo) {
        Button btnLogin = new Button("Login");
        Button btnRegisto = new Button("Registo");

        btnLogin.getStyleClass().add("navbar-button");
        btnRegisto.getStyleClass().add("navbar-button");

        btnLogin.setOnAction(e -> NavigationManager.getInstance().navegarParaLogin());
        btnRegisto.setOnAction(e -> NavigationManager.getInstance().navegarParaRegisto());

        navbar.getChildren().addAll(logo, criarSpacer(), btnLogin, btnRegisto);
    }

    // =====================================================
    // NAVBAR ADMIN
    // =====================================================
    private void construirNavbarAdmin(Label logo) {
        Button btnPainel = new Button("Painel de Administração");
        Button btnSair = new Button("Sair");

        btnPainel.getStyleClass().add("navbar-button");
        btnSair.getStyleClass().add("navbar-button");

        btnPainel.setOnAction(e -> NavigationManager.getInstance().navegarParaAdmin());
        btnSair.setOnAction(e -> NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(
            logo, criarSpacer(), btnPainel, criarAvatar(), btnSair
        );
    }

    // =====================================================
    // NAVBAR UTILIZADOR
    // =====================================================
    private void construirNavbarUtilizador(Label logo) {
        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) return;

        Button btnDashboard    = new Button("Dashboard");
        Button btnProcurar     = new Button("Procurar Veículos");
        Button btnReservas     = new Button("As Minhas Reservas");
        Button btnMeusVeiculos = new Button("Os Meus Veículos");
        Button btnPedidos      = new Button("Pedidos Recebidos");
        Button btnConta        = new Button("Conta");
        Button btnHistorico    = new Button("Histórico Veículos");
        Button btnSair         = new Button("Sair");

        for (Button b : new Button[]{
                btnDashboard, btnProcurar, btnReservas,
                btnMeusVeiculos, btnPedidos, btnConta,
                btnHistorico, btnSair
        }) {
            b.getStyleClass().add("navbar-button");
        }

        btnDashboard.setOnAction(e    -> NavigationManager.getInstance().navegarParaDashboard());
        btnProcurar.setOnAction(e     -> NavigationManager.getInstance().navegarParaProcurarVeiculos());
        btnReservas.setOnAction(e     -> NavigationManager.getInstance().navegarParaMinhasReservas());
        btnMeusVeiculos.setOnAction(e -> NavigationManager.getInstance().navegarParaMeusVeiculos());
        btnPedidos.setOnAction(e      -> NavigationManager.getInstance().navegarParaPedidosRecebidos());
        btnConta.setOnAction(e        -> NavigationManager.getInstance().navegarParaConta());
        btnHistorico.setOnAction(e    -> NavigationManager.getInstance().navegarParaHistoricoVeiculos());
        btnSair.setOnAction(e         -> NavigationManager.getInstance().sair());

        // Sino com badge — usa o StackPane do SinhoNotificacoesView
        SinhoNotificacoesView sinoView = NavigationManager.getInstance().getSinoView();
        javafx.scene.layout.StackPane sino = sinoView != null ? sinoView.getSino() : null;

        if (sino != null) {
            HBox.setMargin(sino, new Insets(0, 4, 0, 4));
        }

        navbar.getChildren().add(logo);

        HBox linksBox = new HBox(22,
            btnDashboard, btnProcurar, btnReservas,
            btnMeusVeiculos, btnPedidos, btnConta, btnHistorico
        );
        linksBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(linksBox, new Insets(0, 0, 0, 36));
        navbar.getChildren().add(linksBox);

        navbar.getChildren().add(criarSpacer());

        if (sino != null) {
            navbar.getChildren().add(sino);
        }
        navbar.getChildren().addAll(criarAvatar(), btnSair);
    }

    public HBox getNavbar() {
        return navbar;
    }
}
