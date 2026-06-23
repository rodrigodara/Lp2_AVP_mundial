package com.aluguer.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.aluguer.dao.TransactionDAO;
import com.aluguer.model.User;
import com.aluguer.util.DatabaseConnection;
import com.aluguer.util.SessionManager;

/**
 * ALV-114 — Criar endpoint depósito
 * ALV-115 — Criar endpoint levantamento
 * ALV-116 — Validar montantes
 * ALV-117 — Atualizar saldo do utilizador
 *
 * Camada de serviço para gestão do saldo da conta do utilizador.
 */
public class ContaService {

    // ================================================================
    // ALV-114 — Depositar
    // ================================================================

    /**
     * Deposita um montante na conta do utilizador autenticado.
     *
     * Regras (ALV-116):
     *   - Montante tem de ser positivo
     *   - Montante máximo por operação: 10.000€
     *
     * @param montante valor a depositar
     * @return ResultadoOperacao com sucesso/erro
     */
    public ReservaService.ResultadoOperacao depositar(BigDecimal montante) {
        // ALV-116: validar montante
        ResultadoValidacao validacao = validarMontante(montante);
        if (!validacao.valido) {
            return ReservaService.ResultadoOperacao.erro(validacao.mensagem);
        }

        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            return ReservaService.ResultadoOperacao.erro("Não existe sessão ativa.");
        }

        // ALV-117: atualizar saldo na BD
        BigDecimal novoSaldo = user.getSaldo().add(montante);
        boolean ok = atualizarSaldo(user.getId(), novoSaldo);

        if (ok) {
            user.setSaldo(novoSaldo); // atualizar sessão local
            registarTransacao(user.getId(), montante.doubleValue(), com.aluguer.model.Transaction.Tipo.deposito);
            return ReservaService.ResultadoOperacao.sucesso(
                String.format("Depósito de %.2f€ efetuado com sucesso. Saldo atual: %.2f€",
                    montante, novoSaldo)
            );
        }
        return ReservaService.ResultadoOperacao.erro("Erro ao efetuar depósito. Tente novamente.");
    }

    // ================================================================
    // ALV-115 — Levantar
    // ================================================================

    /**
     * Levanta um montante da conta do utilizador autenticado.
     *
     * Regras (ALV-116):
     *   - Montante tem de ser positivo
     *   - Saldo tem de ser suficiente
     *
     * @param montante valor a levantar
     * @return ResultadoOperacao com sucesso/erro
     */
    public ReservaService.ResultadoOperacao levantar(BigDecimal montante) {
        // ALV-116: validar montante
        ResultadoValidacao validacao = validarMontante(montante);
        if (!validacao.valido) {
            return ReservaService.ResultadoOperacao.erro(validacao.mensagem);
        }

        User user = SessionManager.getInstance().getUtilizador();
        if (user == null) {
            return ReservaService.ResultadoOperacao.erro("Não existe sessão ativa.");
        }

        // ALV-116: validar saldo suficiente (excluindo saldo pendente/bloqueado)
        if (!user.temSaldoSuficiente(montante)) {
            java.math.BigDecimal pendente = user.getSaldoPendente();
            if (pendente.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return ReservaService.ResultadoOperacao.erro(
                    String.format(
                        "Saldo insuficiente. Saldo total: %.2f€ | Bloqueado (reserva pendente): %.2f€ | Disponível para levantar: %.2f€",
                        user.getSaldo(), pendente, user.getSaldoDisponivel())
                );
            }
            return ReservaService.ResultadoOperacao.erro(
                String.format("Saldo insuficiente. Saldo disponível: %.2f€", user.getSaldoDisponivel())
            );
        }

        // ALV-117: atualizar saldo na BD
        BigDecimal novoSaldo = user.getSaldo().subtract(montante);
        boolean ok = atualizarSaldo(user.getId(), novoSaldo);

        if (ok) {
            user.setSaldo(novoSaldo); // atualizar sessão local
            registarTransacao(user.getId(), montante.doubleValue(), com.aluguer.model.Transaction.Tipo.levantamento);
            return ReservaService.ResultadoOperacao.sucesso(
                String.format("Levantamento de %.2f€ efetuado com sucesso. Saldo atual: %.2f€",
                    montante, novoSaldo)
            );
        }
        return ReservaService.ResultadoOperacao.erro("Erro ao efetuar levantamento. Tente novamente.");
    }

    // ================================================================
    // ALV-116 — Validar montante
    // ================================================================

    private ResultadoValidacao validarMontante(BigDecimal montante) {
        if (montante == null || montante.compareTo(BigDecimal.ZERO) <= 0) {
            return ResultadoValidacao.invalido("O montante tem de ser superior a 0€.");
        }
        if (montante.compareTo(new BigDecimal("10000")) > 0) {
            return ResultadoValidacao.invalido("O montante máximo por operação é 10.000€.");
        }
        return ResultadoValidacao.valido();
    }

    // ================================================================
    // ALV-117 — Atualizar saldo na BD
    // ================================================================

    private boolean atualizarSaldo(int utilizadorId, BigDecimal novoSaldo) {
        String sql = "UPDATE utilizadores SET saldo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, novoSaldo);
            ps.setInt(2, utilizadorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ContaService] Erro ao atualizar saldo: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // Auxiliar — registar transação (não bloqueia a operação principal
    // se falhar; só regista o erro na consola)
    // ================================================================

    private void registarTransacao(int utilizadorId, double valor, com.aluguer.model.Transaction.Tipo tipo) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            new TransactionDAO(conn).registarParaUtilizador(utilizadorId, valor, tipo);
        } catch (SQLException e) {
            System.err.println("[ContaService] Falha ao registar transação: " + e.getMessage());
        }
    }

    // ================================================================
    // Auxiliar de validação
    // ================================================================

    private static class ResultadoValidacao {
        final boolean valido;
        final String mensagem;

        private ResultadoValidacao(boolean valido, String mensagem) {
            this.valido   = valido;
            this.mensagem = mensagem;
        }

        static ResultadoValidacao valido() {
            return new ResultadoValidacao(true, null);
        }

        static ResultadoValidacao invalido(String mensagem) {
            return new ResultadoValidacao(false, mensagem);
        }
    }
}