package com.aluguer.service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.aluguer.dao.MensagemDAO;
import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Mensagem;
import com.aluguer.model.Reserva;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

/**
 * Regras de negócio do chat entre locatário e proprietário.
 *
 * Só é permitido conversar sobre uma reserva enquanto esta estiver no
 * estado ACEITE, e apenas entre o locatário da reserva e o proprietário
 * do veículo associado.
 */
public class MensagemService {

    private final MensagemDAO mensagemDAO = new MensagemDAO();

    /**
     * Valida se o utilizador indicado pode aceder ao chat desta reserva
     * (tem de ser o locatário ou o proprietário do veículo) e devolve a
     * reserva, para a view poder usar os dados sem ir buscar de novo.
     */
    public ResultadoConversa validarAcesso(int reservaId, int utilizadorId) {
        try (java.sql.Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO reservaDAO = new ReservaDAO(conn);
            Reserva reserva = reservaDAO.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoConversa.erro("Reserva #" + reservaId + " não encontrada.");

            VeiculoDAO veiculoDAO = new VeiculoDAO();
            Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
            if (veiculo == null)
                return ResultadoConversa.erro("Veículo associado à reserva não encontrado.");

            boolean ehLocatario    = reserva.getUtilizadorId() == utilizadorId;
            boolean ehProprietario = veiculo.getProprietarioId() == utilizadorId;

            if (!ehLocatario && !ehProprietario)
                return ResultadoConversa.erro("Não tem acesso a esta conversa.");

            if (reserva.getEstado() != Reserva.Estado.ACEITE)
                return ResultadoConversa.erro(
                    "O chat só está disponível enquanto a reserva está ACEITE. Estado atual: "
                    + reserva.getEstado());

            int outroUtilizadorId = ehLocatario ? veiculo.getProprietarioId() : reserva.getUtilizadorId();

            return ResultadoConversa.sucesso(reserva, veiculo, outroUtilizadorId);

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoConversa.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    /** Envia uma mensagem na conversa de uma reserva, validando as regras de acesso. */
    public ReservaService.ResultadoOperacao enviarMensagem(int reservaId, int remetenteId, String conteudo) {
        if (conteudo == null || conteudo.isBlank())
            return ReservaService.ResultadoOperacao.erro("A mensagem não pode estar vazia.");

        ResultadoConversa acesso = validarAcesso(reservaId, remetenteId);
        if (!acesso.isSucesso())
            return ReservaService.ResultadoOperacao.erro(acesso.getErro());

        Mensagem mensagem = new Mensagem(reservaId, remetenteId, acesso.getOutroUtilizadorId(), conteudo.trim());
        try {
            boolean ok = mensagemDAO.inserir(mensagem);
            if (!ok)
                return ReservaService.ResultadoOperacao.erro("Falha ao enviar a mensagem. Tente novamente.");

            return ReservaService.ResultadoOperacao.sucesso("Mensagem enviada.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ReservaService.ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    /** Lista o histórico de mensagens de uma reserva (sem validar acesso — usar validarAcesso antes). */
    public List<Mensagem> listarMensagens(int reservaId) {
        try {
            return mensagemDAO.listarPorReserva(reservaId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** Marca como lidas todas as mensagens desta reserva destinadas ao utilizador indicado. */
    public void marcarComoLidas(int reservaId, int utilizadorId) {
        try {
            mensagemDAO.marcarComoLidas(reservaId, utilizadorId);
        } catch (SQLException e) {
            System.err.println("[MensagemService] Erro ao marcar mensagens como lidas: " + e.getMessage());
        }
    }

    /** Total de mensagens não lidas de um utilizador, em todas as conversas (para badges/notificações). */
    public int contarNaoLidasTotal(int utilizadorId) {
        try {
            return mensagemDAO.contarNaoLidasTotal(utilizadorId);
        } catch (SQLException e) {
            System.err.println("[MensagemService] Erro ao contar mensagens não lidas: " + e.getMessage());
            return 0;
        }
    }

    // ================================================================
    // Resultado da validação de acesso a uma conversa
    // ================================================================

    public static class ResultadoConversa {
        private final boolean sucesso;
        private final String erro;
        private final Reserva reserva;
        private final Veiculo veiculo;
        private final int outroUtilizadorId;

        private ResultadoConversa(boolean sucesso, String erro, Reserva reserva, Veiculo veiculo, int outroUtilizadorId) {
            this.sucesso = sucesso;
            this.erro = erro;
            this.reserva = reserva;
            this.veiculo = veiculo;
            this.outroUtilizadorId = outroUtilizadorId;
        }

        public static ResultadoConversa sucesso(Reserva reserva, Veiculo veiculo, int outroUtilizadorId) {
            return new ResultadoConversa(true, null, reserva, veiculo, outroUtilizadorId);
        }

        public static ResultadoConversa erro(String mensagem) {
            return new ResultadoConversa(false, mensagem, null, null, -1);
        }

        public boolean isSucesso() { return sucesso; }
        public String getErro() { return erro; }
        public Reserva getReserva() { return reserva; }
        public Veiculo getVeiculo() { return veiculo; }
        public int getOutroUtilizadorId() { return outroUtilizadorId; }
    }
}