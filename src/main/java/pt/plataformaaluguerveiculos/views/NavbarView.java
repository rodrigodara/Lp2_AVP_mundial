package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.User;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * ALV-56  — Navbar da aplicação.
 * ALV-90  — Adicionado botão "Pedidos Recebidos".
 * ALV-100 — Adicionado botão "As Minhas Reservas".
 * ALV-134 — Adicionado botão "Os Meus Veículos".
 * ALV-118 — Adicionado botão "Conta" ligado à ContaView.
 *
 * RF6 — O administrador NÃO faz parte do "mercado" da plataforma: não anuncia
 * veículos, não aluga, não tem reservas ou conta/saldo próprios. Por isso a
 * navbar é construída de forma diferente quando a sessão ativa é de um
 * administrador, mostrando apenas o acesso ao Painel de Administração.
 */
public class NavbarView {

    private final HBox navbar = new HBox();

    public NavbarView() {
        navbar.getStyleClass().add("navbar");
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        construir();
    }

    /**
     * Reconstrói os botões da navbar de acordo com o utilizador da sessão atual.
     * Deve ser chamado depois de um login (ou logout), já que o perfil
     * (UTILIZADOR / ADMINISTRADOR) só é conhecido nesse momento.
     */
    public void atualizar() {
        construir();
    }

    private void construir() {
        navbar.getChildren().clear();

        User user = SessionManager.getInstance().getUtilizador();
        boolean isAdmin = user != null && user.isAdministrador();

        if (isAdmin) {
            construirNavbarAdmin();
        } else {
            construirNavbarUtilizador();
        }
    }

    // =========================================================================
    // Navbar do Administrador — apenas Gestão / Utilizadores / Sistema (RF6)
    // =========================================================================
    private void construirNavbarAdmin() {
        Button btnPainel = new Button("Painel de Administração");
        Button btnSair   = new Button("Sair");

        for (Button btn : new Button[]{btnPainel, btnSair}) {
            btn.getStyleClass().add("navbar-button");
        }

        btnPainel.setOnAction(e ->
            NavigationManager.getInstance().navegarParaAdmin());

        btnSair.setOnAction(e ->
            NavigationManager.getInstance().sair());

        navbar.getChildren().addAll(btnPainel, btnSair);
    }

    // =========================================================================
    // Navbar do Utilizador comum — acesso normal ao "mercado" da plataforma
    // =========================================================================
    private void construirNavbarUtilizador() {
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
    }

    public HBox getNavbar() {
        return navbar;
    }
}
