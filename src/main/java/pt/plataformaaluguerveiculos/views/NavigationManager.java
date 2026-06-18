package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.Avaliacao;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.util.SessionManager;

import javafx.scene.Node;

public class NavigationManager {

    private static NavigationManager instance;
    private BaseLayoutView baseLayout;
    private int utilizadorLogadoId = -1;

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

    // =========================
    // AUTENTICAÇÃO / BASE
    // =========================

    public void navegarParaRecuperarPassword() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        RecuperarPasswordView view = new RecuperarPasswordView();
        navegarPara(view.getRoot());
    }

    public void navegarParaDashboard() {
        if (bloquearSeAdmin("acede ao dashboard")) return;
        DashboardView dashboard = new DashboardView();
        navegarPara(dashboard.getRoot());
    }

    public void navegarParaLogin() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        LoginView login = new LoginView();
        navegarPara(login.getRoot());
    }

    public void navegarParaRegisto() {
        if (baseLayout != null) {
            baseLayout.getRoot().setTop(null);
        }
        RegistoView registo = new RegistoView();
        navegarPara(registo.getRoot());
    }

    // =========================
    // ADMIN
    // =========================

    public void navegarParaAdmin() {

        User user = SessionManager.getInstance().getUtilizador();

        if (user == null || !user.isAdministrador()) {
            System.err.println("[NavManager] Acesso negado: utilizador não é administrador.");
            return;
        }

        garantirNavbar();

        AdminView view = new AdminView();
        navegarPara(view.getRoot());
    }

    // =========================
    // RESERVAS
    // =========================

    public void navegarParaCriarReserva(CriarReservaView view) {
        if (bloquearSeAdmin("aluga veículos")) return;
        navegarPara(view.getRoot());
    }

    public void navegarParaPedidosRecebidos() {
        if (bloquearSeAdmin("vê pedidos de reserva recebidos")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        PedidosRecebidosView pedidos = new PedidosRecebidosView(utilizadorLogadoId);
        navegarPara(pedidos.getRoot());
    }

    public void navegarParaAprovarReservas(int proprietarioId) {
        if (bloquearSeAdmin("aprova reservas")) return;
        navegarPara(new PedidosRecebidosView(proprietarioId).getRoot());
    }

    public void navegarParaMinhasReservas() {
        if (bloquearSeAdmin("tem reservas próprias")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        MinhasReservasView minhasReservas = new MinhasReservasView(utilizadorLogadoId);
        navegarPara(minhasReservas.getRoot());
    }

    // =========================
    // VEÍCULOS
    // =========================

    public void navegarParaAdicionarVeiculo() {
        if (bloquearSeAdmin("adiciona veículos")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        AdicionarVeiculoView view = new AdicionarVeiculoView();
        navegarPara(view.getRoot());
    }

    public void navegarParaMeusVeiculos() {
        if (bloquearSeAdmin("tem veículos próprios")) return;
        if (utilizadorLogadoId < 0) {
            System.err.println("[NavManager] Utilizador não autenticado.");
            return;
        }
        garantirNavbar();
        MeusVeiculosView view = new MeusVeiculosView();
        navegarPara(view.getRoot());
    }

    public void navegarParaDetalheVeiculo(Veiculo veiculo) {
        if (bloquearSeAdmin("aluga veículos")) return;
        garantirNavbar();
        DetalheVeiculoView view = new DetalheVeiculoView(veiculo);
        navegarPara(view.getRoot());
    }

    public void navegarParaProcurarVeiculos() {
        if (bloquearSeAdmin("faz parte do mercado de aluguer")) return;
        garantirNavbar();
        ProcurarVeiculosView view = new ProcurarVeiculosView();
        navegarPara(view.getRoot());
    }

    // =========================
    // TRANSAÇÕES
    // =========================

    public void navegarParaHistoricoTransacoes() {
        if (bloquearSeAdmin("tem transações próprias")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        HistoricoTransacoesView view = new HistoricoTransacoesView(utilizadorLogadoId);
        navegarPara(view.getRoot());
    }

    public void navegarParaConta() {
        if (bloquearSeAdmin("gere saldo/conta própria")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        ContaView view = new ContaView();
        navegarPara(view.getRoot());
    }

    // =========================
    // DISPONIBILIDADE
    // =========================

    public void navegarParaIndisponibilidade(int veiculoId) {
        if (bloquearSeAdmin("gere disponibilidade de veículos")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        IndisponibilidadeView view = new IndisponibilidadeView(veiculoId);
        navegarPara(view.getRoot());
    }

    // =========================
    // AVALIAÇÕES
    // =========================

    public void navegarParaAvaliar(int reservaId, int avaliadorId, int avaliadoId,
                                   Avaliacao.TipoAvaliado tipo, String nomeAvaliado) {
        if (bloquearSeAdmin("avalia outros utilizadores")) return;
        garantirNavbar();
        AvaliarView view = new AvaliarView(reservaId, avaliadorId, avaliadoId, tipo, nomeAvaliado);
        navegarPara(view.getRoot());
    }

    // =========================
    // HISTÓRICO
    // =========================

    public void navegarParaHistoricoVeiculos() {
        if (bloquearSeAdmin("tem histórico de veículos próprios")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        HistoricoVeiculosView view = new HistoricoVeiculosView(utilizadorLogadoId);
        navegarPara(view.getRoot());
    }

    // =========================
    // AUXILIAR
    // =========================

    /**
     * RF6 — Bloqueia o acesso de administradores a funcionalidades de "mercado"
     * (comprar/alugar/anunciar veículos entre utilizadores). Um administrador
     * gere a plataforma, mas não é um participante do mercado.
     *
     * @param acaoDescricao descrição da ação bloqueada, usada apenas para log
     * @return true se o utilizador é administrador e a navegação foi bloqueada
     *         (e redirecionada para o Painel de Administração); false caso
     *         contrário, permitindo que a navegação original continue.
     */
    private boolean bloquearSeAdmin(String acaoDescricao) {
        User user = SessionManager.getInstance().getUtilizador();
        if (user != null && user.isAdministrador()) {
            System.err.println("[NavManager] Acesso negado: administrador não pode " + acaoDescricao + ".");
            navegarParaAdmin();
            return true;
        }
        return false;
    }

    private void garantirNavbar() {
        if (baseLayout != null && baseLayout.getRoot().getTop() == null) {
            baseLayout.getRoot().setTop(baseLayout.getNavbarView().getNavbar());
        }
    }

    public void sair() {
        SessionManager.getInstance().terminarSessao();
        utilizadorLogadoId = -1;
        navegarParaLogin();
    }
}