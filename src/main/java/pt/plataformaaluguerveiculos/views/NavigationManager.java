package pt.plataformaaluguerveiculos.views;

import javafx.scene.Node;

/**
 * ALV-57 - Implementar navegação entre páginas
 * Singleton que gere a troca de conteúdo no BaseLayoutView.
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

    public void sair() {
        navegarParaLogin();
    }
    // ALV-64 – ir para formulário de reserva
    public void navegarParaCriarReserva(CriarReservaView view) {
        navegarPara(view.getRoot());
    }

    // ALV-65 – ir para aprovar/rejeitar reservas
    public void navegarParaPedidosRecebidos() {
        int id = utilizadorLogadoId > 0 ? utilizadorLogadoId : 1;
        navegarPara(new PedidosRecebidosView(id).getRoot());
    }

    public void navegarParaAprovarReservas(int proprietarioId) {
        navegarPara(new PedidosRecebidosView(proprietarioId).getRoot());
    }

    // ALV-66 – ir para as minhas reservas
    public void navegarParaMinhasReservas(int utilizadorId) {
        navegarParaDashboard();
    }
}