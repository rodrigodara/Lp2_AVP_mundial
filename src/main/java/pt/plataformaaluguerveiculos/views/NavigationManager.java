package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.util.SessionManager;

import javafx.scene.Node;

public class NavigationManager {

    private SinhoNotificacoesView sinoView;
    private static NavigationManager instance;
    private BaseLayoutView baseLayout;
    private int utilizadorLogadoId = -1;
    private ConversaView conversaAberta;

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

        if (sinoView == null) {
            sinoView = new SinhoNotificacoesView();
        }
        sinoView.atualizarBadge();
        navegarPara(sinoView.getSino());
    }
    

    public void navegarParaAdicionarVeiculo() {
        navegarPara(new AdicionarVeiculoView().getRoot());
    }

    public void navegarParaAvaliar(int reservaId, int utilizadorId, int veiculoId, String nomeVeiculo) {
        navegarPara(new AvaliarView(reservaId, utilizadorId, veiculoId, nomeVeiculo).getRoot());
    }

    public void navegarParaAvaliacoes(int veiculoId, String nomeVeiculo) {
        garantirNavbar();
        navegarPara(new AvaliacoesView(veiculoId, nomeVeiculo).getRoot());
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
        if (bloquearSeAdmin("ver reservas")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        navegarPara(new MinhasReservasView(utilizadorLogadoId).getRoot());
    }

    /** Navega para Minhas Reservas abrindo diretamente na tab do estado indicado.
     *  0=Pendentes, 1=Aceites, 2=Rejeitadas, 3=Canceladas, 4=Concluídas */
    public void navegarParaMinhasReservasEstado(int tab) {
        if (bloquearSeAdmin("ver reservas")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        navegarPara(new MinhasReservasView(utilizadorLogadoId, tab).getRoot());
    }

    /** Navega para a conversa (chat) de uma reserva ACEITE, entre locatário e proprietário. */
    public void navegarParaConversa(int reservaId) {
        if (bloquearSeAdmin("ver conversa")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();

        if (conversaAberta != null) {
            conversaAberta.pararAutoRefresh();
        }
        conversaAberta = new ConversaView(reservaId, utilizadorLogadoId);
        navegarPara(conversaAberta.getRoot());
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

    /** Navega para a gestão de indisponibilidade de um veículo específico (proprietário). */
    public void navegarParaIndisponibilidade(int veiculoId) {
        if (bloquearSeAdmin("gerir indisponibilidade")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        navegarPara(new IndisponibilidadeView(veiculoId).getRoot());
    }

    /** Navega para a consulta de receita por veículo do proprietário logado. */
    public void navegarParaConsultaReceita() {
        if (bloquearSeAdmin("consultar receita")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        navegarPara(new ConsultaReceitaView(utilizadorLogadoId).getRoot());
    }

    /** Navega para a lista de veículos que o utilizador marcou como favoritos. */
    public void navegarParaMeusFavoritos() {
        if (bloquearSeAdmin("ver favoritos")) return;
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        navegarPara(new MeusFavoritosView().getRoot());
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
        navegarPara(new DetalheVeiculoView(carregarComFotos(veiculo)).getRoot());
    }

    public void navegarParaDetalheVeiculo(Veiculo veiculo, DetalheVeiculoView.Origem origem) {
        if (bloquearSeAdmin("ver veículo")) {
            return;
        }

        garantirNavbar();
        navegarPara(new DetalheVeiculoView(carregarComFotos(veiculo), origem).getRoot());
    }

    public void navegarParaEditarVeiculo(Veiculo veiculo) {
        if (bloquearSeAdmin("editar veículo")) {
            return;
        }

        Veiculo completo = carregarComFotos(veiculo);
        User user = SessionManager.getInstance().getUtilizador();
        if (user == null || completo == null || user.getId() != completo.getProprietarioId()) {
            return; // só o dono pode editar
        }

        garantirNavbar();
        navegarPara(new AdicionarVeiculoView(completo).getRoot());
    }

    /**
     * Garante que o veículo a mostrar no detalhe tem as fotos carregadas.
     * Listagens (ex: "Os Meus Veículos") usam uma query leve sem fotos por
     * performance; aqui recarrega-se sempre por ID antes de abrir o detalhe.
     */
    private Veiculo carregarComFotos(Veiculo veiculo) {
        if (veiculo == null) return null;
        try {
            Veiculo completo = new com.aluguer.dao.VeiculoDAO().buscarPorId(veiculo.getId());
            return completo != null ? completo : veiculo;
        } catch (Exception ex) {
            ex.printStackTrace();
            return veiculo;
        }
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