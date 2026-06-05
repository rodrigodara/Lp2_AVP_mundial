package pt.plataformaaluguerveiculos.views;

import javafx.scene.Node;

/**
 * ALV-57 - Implementar navegação entre páginas
 * Singleton que gere a troca de conteúdo no BaseLayoutView.
 *
 * ALV-90 — Adicionado navegarParaPedidosRecebidos()
 * ALV-100 — Adicionado navegarParaMinhasReservas()
 */
public class NavigationManager {

    private int utilizadorLogadoId = -1;

    private static NavigationManager instance;
    private BaseLayoutView baseLayout;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void init(BaseLayoutView baseLayout) {
        this.baseLayout = baseLayout;
    }

    public BaseLayoutView getBaseLayout() {
        return baseLayout;
    }

    public void setUtilizadorLogado(int utilizadorId) {
        this.utilizadorLogadoId = utilizadorId;
    }

    public int getUtilizadorLogado() {
        return utilizadorLogadoId;
    }

    public void navegarPara(Node pagina) {
        if (baseLayout != null) {
            baseLayout.setContent(pagina);
        }
    }

    public void navegarParaDashboard() {
        DashboardView dashboard = new DashboardView();
        navegarPara(dashboard.getRoot());
    }

    public void navegarParaLogin() {
        // Esconde navbar e mostra login
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        LoginView login = new LoginView();
        navegarPara(login.getRoot());
    }

    // ALV-64 – ir para formulário de reserva
    public void navegarParaCriarReserva(CriarReservaView view) {
        navegarPara(view.getRoot());
    }

    // ----------------------------------------------------------------
    // ALV-90 — Navegar para a página de pedidos recebidos
    // ----------------------------------------------------------------
    public void navegarParaPedidosRecebidos() {
        if (utilizadorLogadoId < 0) {
            System.err.println("[NavManager] Utilizador não autenticado.");
            return;
        }
        garantirNavbar();
        PedidosRecebidosView pedidos = new PedidosRecebidosView(utilizadorLogadoId);
        navegarPara(pedidos.getRoot());
    }

    public void navegarParaAprovarReservas(int proprietarioId) {
        navegarPara(new PedidosRecebidosView(proprietarioId).getRoot());
    }

    // ----------------------------------------------------------------
    // ALV-100 — Navegar para "As Minhas Reservas"
    // ----------------------------------------------------------------
    public void navegarParaMinhasReservas() {
        if (utilizadorLogadoId < 0) {
            System.err.println("[NavManager] Utilizador não autenticado.");
            return;
        }
        garantirNavbar();
        MinhasReservasView minhasReservas = new MinhasReservasView(utilizadorLogadoId);
        navegarPara(minhasReservas.getRoot());
    }

    // ----------------------------------------------------------------
    // Auxiliar — garante que a navbar está visível
    // ----------------------------------------------------------------
    private void garantirNavbar() {
        if (baseLayout != null && baseLayout.getRoot().getTop() == null) {
            baseLayout.getRoot().setTop(baseLayout.getNavbarView().getNavbar());
        }
    }

    public void sair() {
        navegarParaLogin();
    }

    public void navegarParaProcurarVeiculos() {
        garantirNavbar();
        ProcurarVeiculosView view = new ProcurarVeiculosView();
        navegarPara(view.getRoot());
    }
}
