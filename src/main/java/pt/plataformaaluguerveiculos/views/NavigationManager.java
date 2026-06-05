package pt.plataformaaluguerveiculos.views;

import javafx.scene.Node;

/**
 * ALV-57 - Implementar navegação entre páginas
 * Singleton que gere a troca de conteúdo no BaseLayoutView.
<<<<<<< HEAD:src/main/java/pt/plataformaaluguerveiculos/views/NavigationManager.java
=======
 *
 * ALV-90 — Adicionado navegarParaPedidosRecebidos()
 * ALV-100 — Adicionado navegarParaMinhasReservas()
>>>>>>> origin/main:src/pt/plataformaaluguerveiculos/views/NavigationManager.java
 */
public class NavigationManager {

    private int utilizadorLogadoId = -1;

    private static NavigationManager instance;
    private BaseLayoutView baseLayout;

<<<<<<< HEAD:src/main/java/pt/plataformaaluguerveiculos/views/NavigationManager.java
=======
    // ALV-90: guarda o id do utilizador logado (definido após login)
    private int utilizadorLogadoId = -1;

>>>>>>> origin/main:src/pt/plataformaaluguerveiculos/views/NavigationManager.java
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

<<<<<<< HEAD:src/main/java/pt/plataformaaluguerveiculos/views/NavigationManager.java
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
=======
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

>>>>>>> origin/main:src/pt/plataformaaluguerveiculos/views/NavigationManager.java
}