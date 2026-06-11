package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.Avaliacao;

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

    public void navegarParaDashboard() {
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

    // ALV-64 — ir para formulário de reserva
    public void navegarParaCriarReserva(CriarReservaView view) {
        navegarPara(view.getRoot());
    }

    public void navegarParaPedidosRecebidos() {
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        PedidosRecebidosView pedidos = new PedidosRecebidosView(utilizadorLogadoId);
        navegarPara(pedidos.getRoot());
    }

    public void navegarParaAprovarReservas(int proprietarioId) {
        navegarPara(new PedidosRecebidosView(proprietarioId).getRoot());
    }

    // ALV-100 — Navegar para "As Minhas Reservas"
    public void navegarParaMinhasReservas() {
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        MinhasReservasView minhasReservas = new MinhasReservasView(utilizadorLogadoId);
        navegarPara(minhasReservas.getRoot());
    }

    public void navegarParaProcurarVeiculos() {
        garantirNavbar();
        ProcurarVeiculosView view = new ProcurarVeiculosView();
        navegarPara(view.getRoot());
    }

    public void navegarParaHistoricoTransacoes() {
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        HistoricoTransacoesView view = new HistoricoTransacoesView(utilizadorLogadoId);
        navegarPara(view.getRoot());
    }

    // ALV-118 — Navegar para gestão de conta (saldo)
    public void navegarParaConta() {
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        ContaView view = new ContaView();
        navegarPara(view.getRoot());
    }

    // ALV-171 — Navegar para gestão de indisponibilidade de um veículo
    public void navegarParaIndisponibilidade(int veiculoId) {
        if (utilizadorLogadoId < 0) return;
        garantirNavbar();
        IndisponibilidadeView view = new IndisponibilidadeView(veiculoId);
        navegarPara(view.getRoot());
    }

    // Navegar para página de avaliação
    public void navegarParaAvaliar(int reservaId, int avaliadorId, int avaliadoId,
                                    Avaliacao.TipoAvaliado tipo, String nomeAvaliado) {
        garantirNavbar();
        AvaliarView view = new AvaliarView(reservaId, avaliadorId, avaliadoId, tipo, nomeAvaliado);
        navegarPara(view.getRoot());
    }

    // Auxiliar — garante que a navbar está visível
    private void garantirNavbar() {
        if (baseLayout != null && baseLayout.getRoot().getTop() == null) {
            baseLayout.getRoot().setTop(baseLayout.getNavbarView().getNavbar());
        }
    }

    public void sair() {
        navegarParaLogin();
    }
}