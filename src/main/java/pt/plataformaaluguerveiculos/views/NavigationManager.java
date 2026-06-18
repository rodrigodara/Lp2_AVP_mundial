package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.Avaliacao;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.util.SessionManager;

import javafx.scene.Node;

public class NavigationManager {

    private SinhoNotificacoesView sinoView;
    private static NavigationManager instance;
    private BaseLayoutView baseLayout;
    private int utilizadorLogadoId = -1;

    private NavigationManager() {
    }

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void init(BaseLayoutView baseLayout) {
        this.baseLayout = baseLayout;

        // cria o sino UMA vez (estado global)
        this.sinoView = new SinhoNotificacoesView();
    }

    public SinhoNotificacoesView getSinoView() {
        return sinoView;
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

    // =========================
    // NOTIFICAÇÕES (NOVO)
    // =========================
    public void navegarParaNotificacoes() {

        if (bloquearSeAdmin("ver notificações")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();

        SinhoNotificacoesView view
                = new SinhoNotificacoesView();

        navegarPara(sinoView.getSino());

        if (sinoView == null) {
            sinoView = new SinhoNotificacoesView();
        }
        navegarPara(sinoView.getSino());
    }
    

    public void navegarParaAdicionarVeiculo() {
        navegarPara(new AdicionarVeiculoView().getRoot());
    }

    public void navegarParaAvaliar(int reservaId, int avaliadorId, int avaliadoId,
            Avaliacao.TipoAvaliado tipo, String nome) {
        navegarPara(new AvaliarView(reservaId, avaliadorId, avaliadoId, tipo, nome).getRoot());
    }

    public void navegarParaCriarReserva(CriarReservaView view) {
        navegarPara(view.getRoot());
    }

    // =========================
    // AUTENTICAÇÃO / BASE
    // =========================
    public void navegarParaRecuperarPassword() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        navegarPara(new RecuperarPasswordView().getRoot());
    }

    public void navegarParaDashboard() {
        if (bloquearSeAdmin("acede ao dashboard")) {
            return;
        }
        navegarPara(new DashboardView().getRoot());
    }

    public void navegarParaLogin() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        navegarPara(new LoginView().getRoot());
    }

    public void navegarParaRegisto() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        navegarPara(new RegistoView().getRoot());
    }

    // =========================
    // ADMIN
    // =========================
    public void navegarParaAdmin() {

        User user = SessionManager.getInstance().getUtilizador();

        if (user == null || !user.isAdministrador()) {
            System.err.println("[NavManager] Acesso negado.");
            return;
        }

        garantirNavbar();
        navegarPara(new AdminView().getRoot());
    }

    // =========================
    // RESERVAS
    // =========================
    public void navegarParaPedidosRecebidos() {
        if (bloquearSeAdmin("vê pedidos")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();
        navegarPara(new PedidosRecebidosView(utilizadorLogadoId).getRoot());
    }

    public void navegarParaMinhasReservas() {
        if (bloquearSeAdmin("ver reservas")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();
        navegarPara(new MinhasReservasView(utilizadorLogadoId).getRoot());
    }

    // =========================
    // VEÍCULOS
    // =========================
    public void navegarParaMeusVeiculos() {
        if (bloquearSeAdmin("ver veículos")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();
        navegarPara(new MeusVeiculosView().getRoot());
    }

    public void navegarParaProcurarVeiculos() {
        if (bloquearSeAdmin("procurar veículos")) {
            return;
        }

        garantirNavbar();
        navegarPara(new ProcurarVeiculosView().getRoot());
    }

    public void navegarParaDetalheVeiculo(Veiculo veiculo) {
        if (bloquearSeAdmin("ver veículo")) {
            return;
        }

        garantirNavbar();
        navegarPara(new DetalheVeiculoView(veiculo).getRoot());
    }

    // =========================
    // CONTROLO DE ACESSO ADMIN
    // =========================
    private boolean bloquearSeAdmin(String acaoDescricao) {
        User user = SessionManager.getInstance().getUtilizador();

        if (user != null && user.isAdministrador()) {
            System.err.println("[NavManager] Admin bloqueado: " + acaoDescricao);
            navegarParaAdmin();
            return true;
        }
        return false;
    }

    private void garantirNavbar() {
        if (baseLayout != null && baseLayout.getRoot().getTop() == null) {
            baseLayout.getRoot().setTop(
                    baseLayout.getNavbarView().getNavbar()
            );
        }
    }// =========================
// CONTA
// =========================

    public void navegarParaConta() {
        if (bloquearSeAdmin("ver conta")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();

        ContaView view = new ContaView();
        navegarPara(view.getRoot());
    }

// =========================
// HISTÓRICO VEÍCULOS
// =========================
    public void navegarParaHistoricoVeiculos() {
        if (bloquearSeAdmin("ver histórico de veículos")) {
            return;
        }
        if (utilizadorLogadoId < 0) {
            return;
        }

        garantirNavbar();

        HistoricoVeiculosView view
                = new HistoricoVeiculosView(utilizadorLogadoId);

        navegarPara(view.getRoot());
    }

    public void sair() {
        SessionManager.getInstance().terminarSessao();
        utilizadorLogadoId = -1;
        navegarParaLogin();
    }
}
