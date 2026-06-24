package com.aluguer.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.aluguer.dao.DenunciaDAO;
import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.TransactionDAO;
import com.aluguer.dao.UserDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Denuncia;
import com.aluguer.model.Reserva;
import com.aluguer.model.Reserva.Estado;
import com.aluguer.model.Transaction;
import com.aluguer.model.User;
import com.aluguer.model.Veiculo;
import com.aluguer.service.ReservaService.ResultadoOperacao;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

/**
 * DenunciaService — cobre dois tipos de denúncia:
 *
 *  1) Reportar um problema numa reserva (normalmente o proprietário a
 *     denunciar danos causados pelo locatário). Conclui a reserva e
 *     deixa a caução EM_DISPUTA até a administração decidir.
 *
 *  2) Denunciar um utilizador de forma geral (sem reserva associada),
 *     sem qualquer efeito sobre caução — fica só pendente de análise.
 *
 * Em ambos os casos o denunciado pode responder com uma contraprova
 * (texto + foto) antes da administração decidir.
 */
public class DenunciaService {

    private final ReservaService reservaService = new ReservaService();
    private final NotificacaoService notificacaoService = NotificacaoService.getInstance();

    // ================================================================
    // 1) Reportar problema numa reserva (liga-se à caução)
    // ================================================================

    public ResultadoOperacao reportarProblemaReserva(int reservaId, int proprietarioId, String motivo, byte[] foto) {
        if (motivo == null || motivo.isBlank())
            return ResultadoOperacao.erro("Tens de indicar o motivo da denúncia.");

        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO reservaDAO = new ReservaDAO(conn);
            Reserva reserva = reservaDAO.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");

            if (reserva.getEstado() != Estado.ACEITE)
                return ResultadoOperacao.erro("Só é possível reportar um problema numa reserva ACEITE.");

            VeiculoDAO veiculoDAO = new VeiculoDAO();
            Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
            if (veiculo == null || veiculo.getProprietarioId() != proprietarioId)
                return ResultadoOperacao.erro("Apenas o proprietário do veículo pode reportar esta reserva.");

            // Conclui a reserva mas deixa a caução em disputa (não devolve automaticamente)
            ResultadoOperacao concluiu = reservaService.concluirComProblema(reservaId, proprietarioId);
            if (!concluiu.isSucesso())
                return concluiu;

            Denuncia d = new Denuncia(reservaId, proprietarioId, reserva.getUtilizadorId(), motivo.trim(), foto);
            DenunciaDAO denunciaDAO = new DenunciaDAO(conn);
            boolean ok = denunciaDAO.inserir(d);
            if (!ok)
                return ResultadoOperacao.erro("A reserva foi concluída, mas houve uma falha a registar a denúncia. Contacte o suporte.");

            notificacaoService.criarNotificacao(reserva.getUtilizadorId(), "AVISO",
                "🚩 O proprietário reportou um problema no teu aluguer do " +
                veiculo.getMarca() + " " + veiculo.getModelo() +
                ". A caução fica retida até decisão da administração. " +
                "Podes responder em 'As Minhas Reservas' (separador Concluídas).");

            return ResultadoOperacao.sucesso(String.format(
                "Denúncia registada. A caução (%.2f€) fica retida até a administração decidir o caso.",
                reserva.getCaucao()));

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // 1b) Locatário reporta um problema numa reserva já CONCLUÍDA
    //     (ex.: defeito não detetado na devolução, cobrança indevida).
    //     A caução já foi devolvida e o proprietário já foi pago — esta
    //     denúncia não move saldo automaticamente; serve para o admin
    //     avaliar e decidir manualmente (ex.: aviso, compensação à parte).
    //     A reserva fica marcada como "em disputa" (caucaoEstado) até o
    //     admin decidir, sem alterar o estado CONCLUIDO da reserva.
    // ================================================================

    public ResultadoOperacao reportarProblemaPosConclusao(int reservaId, int locatarioId, String motivo, byte[] foto) {
        if (motivo == null || motivo.isBlank())
            return ResultadoOperacao.erro("Tens de indicar o motivo da denúncia.");

        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO reservaDAO = new ReservaDAO(conn);
            Reserva reserva = reservaDAO.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");

            if (reserva.getEstado() != Estado.CONCLUIDO)
                return ResultadoOperacao.erro("Só é possível reportar este tipo de problema numa reserva já CONCLUÍDA.");

            if (reserva.getUtilizadorId() != locatarioId)
                return ResultadoOperacao.erro("Apenas o locatário desta reserva pode reportar este problema.");

            if (reserva.isCaucaoEmDisputa())
                return ResultadoOperacao.erro("Já existe uma denúncia pendente para esta reserva.");

            VeiculoDAO veiculoDAO = new VeiculoDAO();
            Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
            if (veiculo == null)
                return ResultadoOperacao.erro("Veículo associado à reserva não encontrado.");

            // Marca como "em disputa" apenas para sinalizar ao admin — não reverte
            // a devolução/pagamento já feitos quando a reserva foi concluída.
            reservaDAO.atualizarCaucaoEstado(reservaId, "EM_DISPUTA");

            Denuncia d = new Denuncia(reservaId, locatarioId, veiculo.getProprietarioId(), motivo.trim(), foto);
            DenunciaDAO denunciaDAO = new DenunciaDAO(conn);
            boolean ok = denunciaDAO.inserir(d);
            if (!ok)
                return ResultadoOperacao.erro("Falha ao registar a denúncia. Tente novamente.");

            notificacaoService.criarNotificacao(veiculo.getProprietarioId(), "AVISO",
                "🚩 O locatário reportou um problema sobre o aluguer (já concluído) do " +
                veiculo.getMarca() + " " + veiculo.getModelo() +
                ". A administração vai avaliar o caso.");

            return ResultadoOperacao.sucesso(
                "Denúncia registada. A administração vai avaliar o caso e decidir se há lugar a alguma compensação.");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // 2) Denunciar um utilizador (sem ligação a uma caução)
    // ================================================================

    public ResultadoOperacao denunciarUtilizador(int denuncianteId, int denunciadoId, String motivo, byte[] foto) {
        if (motivo == null || motivo.isBlank())
            return ResultadoOperacao.erro("Tens de indicar o motivo da denúncia.");
        if (denuncianteId == denunciadoId)
            return ResultadoOperacao.erro("Não podes denunciar-te a ti próprio.");

        try (Connection conn = DatabaseConnection.getConnection()) {
            Denuncia d = new Denuncia(null, denuncianteId, denunciadoId, motivo.trim(), foto);
            DenunciaDAO dao = new DenunciaDAO(conn);
            boolean ok = dao.inserir(d);
            if (!ok) return ResultadoOperacao.erro("Falha ao registar a denúncia.");

            return ResultadoOperacao.sucesso("Denúncia registada. A administração vai analisar o caso.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Responder a uma denúncia (contraprova do denunciado)
    // ================================================================

    public ResultadoOperacao responder(int denunciaId, int utilizadorId, String respostaTexto, byte[] fotoResposta) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DenunciaDAO dao = new DenunciaDAO(conn);
            Denuncia d = dao.buscarPorId(denunciaId);

            if (d == null) return ResultadoOperacao.erro("Denúncia não encontrada.");
            if (d.getDenunciadoId() != utilizadorId)
                return ResultadoOperacao.erro("Esta denúncia não foi feita contra ti.");
            if (d.getEstado() != Denuncia.Estado.PENDENTE)
                return ResultadoOperacao.erro("Esta denúncia já foi decidida e não pode ser respondida.");

            boolean ok = dao.adicionarResposta(denunciaId, respostaTexto, fotoResposta);
            if (!ok) return ResultadoOperacao.erro("Falha ao guardar a resposta.");

            return ResultadoOperacao.sucesso("Resposta enviada. A administração vai considerar a tua versão.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    /** Devolve a denúncia PENDENTE ligada a uma reserva (ou null se não houver). */
    public Denuncia buscarPendentePorReserva(int reservaId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Denuncia> lista = new DenunciaDAO(conn).listarPorReserva(reservaId);
            return lista.stream()
                .filter(d -> d.getEstado() == Denuncia.Estado.PENDENTE)
                .findFirst().orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================================================================
    // Listagens
    // ================================================================

    public List<Denuncia> listarPendentes() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new DenunciaDAO(conn).listarPendentes();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** Denúncias recebidas — o utilizador é o denunciado. */
    public List<Denuncia> listarRecebidas(int utilizadorId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new DenunciaDAO(conn).listarRecebidasPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** Denúncias feitas — o utilizador é o denunciante. */
    public List<Denuncia> listarFeitas(int utilizadorId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return new DenunciaDAO(conn).listarFeitasPorUtilizador(utilizadorId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ================================================================
    // Decisão da administração
    // ================================================================

    /**
     * @param aprovarDenunciante true  -> denúncia procede (o denunciante tinha razão)
     *                           false -> denúncia rejeitada
     */
    public ResultadoOperacao decidir(int denunciaId, boolean aprovarDenunciante, String decisaoTexto) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DenunciaDAO dao = new DenunciaDAO(conn);
            Denuncia d = dao.buscarPorId(denunciaId);

            if (d == null) return ResultadoOperacao.erro("Denúncia não encontrada.");
            if (d.getEstado() != Denuncia.Estado.PENDENTE)
                return ResultadoOperacao.erro("Esta denúncia já foi decidida.");

            String novoEstado = aprovarDenunciante ? "APROVADA" : "REJEITADA";
            boolean ok = dao.decidir(denunciaId, novoEstado, decisaoTexto);
            if (!ok) return ResultadoOperacao.erro("Falha ao gravar a decisão.");

            // Se ligada a uma reserva, resolve o destino da caução em disputa —
            // mas só no fluxo "problema reportado pelo proprietário a meio da
            // conclusão" (a caução ainda estava em limbo). Quando é o locatário
            // a reportar um problema numa reserva já CONCLUÍDA, a caução já foi
            // devolvida e o proprietário já foi pago — não se move saldo de novo.
            if (d.getReservaId() != null) {
                Reserva reservaDaDenuncia = new ReservaDAO(conn).buscarPorId(d.getReservaId());
                // Critério: no fluxo antigo o denunciante é o proprietário do
                // veículo, ou seja, é diferente do utilizadorId (locatário) da reserva.
                boolean caucaoAindaEmLimbo = reservaDaDenuncia != null
                    && d.getDenuncianteId() != reservaDaDenuncia.getUtilizadorId();

                if (caucaoAindaEmLimbo) {
                    resolverCaucao(conn, d, aprovarDenunciante);
                } else if (reservaDaDenuncia != null) {
                    // A caução já tinha sido devolvida quando a reserva foi
                    // concluída normalmente; repõe o estado (estava "EM_DISPUTA"
                    // só como sinalizador enquanto o admin avaliava).
                    new ReservaDAO(conn).atualizarCaucaoEstado(d.getReservaId(), "DEVOLVIDA");
                }
            }

            notificacaoService.criarNotificacao(d.getDenuncianteId(), "AVISO",
                aprovarDenunciante
                    ? "✅ A tua denúncia #" + denunciaId + " foi aprovada pela administração."
                    : "❌ A tua denúncia #" + denunciaId + " foi rejeitada pela administração.");

            notificacaoService.criarNotificacao(d.getDenunciadoId(), "AVISO",
                aprovarDenunciante
                    ? "⚠️ A denúncia contra ti (#" + denunciaId + ") foi confirmada pela administração."
                    : "✅ A denúncia contra ti (#" + denunciaId + ") foi rejeitada — sem consequências.");

            return ResultadoOperacao.sucesso("Decisão registada (" + novoEstado + ").");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    /**
     * Move a caução para o vencedor da disputa:
     *  - denúncia aprovada  -> caução fica retida a favor do proprietário (denunciante)
     *  - denúncia rejeitada -> caução é devolvida ao locatário (denunciado)
     */
    private void resolverCaucao(Connection conn, Denuncia d, boolean aprovarDenunciante) throws SQLException {
        ReservaDAO reservaDAO = new ReservaDAO(conn);
        Reserva reserva = reservaDAO.buscarPorId(d.getReservaId());
        if (reserva == null) return;

        UserDAO userDAO = new UserDAO();
        double caucao = reserva.getCaucao();
        int beneficiarioId = aprovarDenunciante ? d.getDenuncianteId() : d.getDenunciadoId();

        try {
            Optional<User> beneficiarioOpt = userDAO.findById(beneficiarioId);
            if (beneficiarioOpt.isPresent()) {
                User beneficiario = beneficiarioOpt.get();
                BigDecimal novoSaldo = beneficiario.getSaldo().add(BigDecimal.valueOf(caucao));
                userDAO.atualizarSaldo(beneficiarioId, novoSaldo);

                SessionManager sm = SessionManager.getInstance();
                if (sm.getUtilizador() != null && sm.getUtilizador().getId() == beneficiarioId) {
                    sm.getUtilizador().setSaldo(novoSaldo);
                }

                // Registar a transação: se for o proprietário (denunciante aprovado),
                // é um recebimento; se for o locatário (denúncia rejeitada), é a
                // devolução normal da caução.
                Transaction.Tipo tipoTransacao = aprovarDenunciante
                    ? Transaction.Tipo.recebimento_proprietario
                    : Transaction.Tipo.reembolso_caucao;
                new TransactionDAO(conn).registarParaUtilizador(beneficiarioId, caucao, tipoTransacao);
            }
        } catch (SQLException ex) {
            System.err.println("[DenunciaService] Falha ao mover caução: " + ex.getMessage());
        }

        reservaDAO.atualizarCaucaoEstado(reserva.getId(), aprovarDenunciante ? "RETIDA" : "DEVOLVIDA");
    }
}