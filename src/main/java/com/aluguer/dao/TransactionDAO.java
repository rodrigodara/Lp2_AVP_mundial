package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Transaction;

/**
 * DAO para operações sobre a tabela transacao.
 * A tabela transacao regista depósitos e levantamentos da carteira do utilizador.
 * Cada transação está ligada a uma conta (tabela conta).
 */
public class TransactionDAO {

    private final Connection conn;

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    // ----------------------------------------------------------------
    // Registar depósito
    // ----------------------------------------------------------------

    public boolean registarDeposito(int contaId, double valor) {
        return inserir(new Transaction(contaId, valor, Transaction.Tipo.deposito));
    }

    // ----------------------------------------------------------------
    // Registar levantamento
    // ----------------------------------------------------------------

    public boolean registarLevantamento(int contaId, double valor) {
        return inserir(new Transaction(contaId, valor, Transaction.Tipo.levantamento));
    }

    // ----------------------------------------------------------------
    // Listar todas as transações de um utilizador (via JOIN com conta)
    // ----------------------------------------------------------------

    public List<Transaction> listarPorUtilizador(int utilizadorId) {
        List<Transaction> lista = new ArrayList<>();

        String sql = """
                SELECT t.* FROM transacao t
                JOIN conta c ON t.contaId = c.id
                WHERE c.utilizadorId = ?
                ORDER BY t.data DESC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao listar transações: " + e.getMessage());
        }

        return lista;
    }

    // ----------------------------------------------------------------
    // Listar só depósitos de um utilizador
    // ----------------------------------------------------------------

    public List<Transaction> listarDepositosPorUtilizador(int utilizadorId) {
        return listarPorUtilizadorETipo(utilizadorId, Transaction.Tipo.deposito);
    }

    // ----------------------------------------------------------------
    // Listar só levantamentos de um utilizador
    // ----------------------------------------------------------------

    public List<Transaction> listarLevantamentosPorUtilizador(int utilizadorId) {
        return listarPorUtilizadorETipo(utilizadorId, Transaction.Tipo.levantamento);
    }

    // ----------------------------------------------------------------
    // Total depositado por um utilizador
    // ----------------------------------------------------------------

    public double totalDepositadoPorUtilizador(int utilizadorId) {
        String sql = """
                SELECT SUM(t.valor) FROM transacao t
                JOIN conta c ON t.contaId = c.id
                WHERE c.utilizadorId = ? AND t.tipo = 'deposito'
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao calcular total depositado: " + e.getMessage());
        }
        return 0.0;
    }

    // ----------------------------------------------------------------
    // Total levantado por um utilizador
    // ----------------------------------------------------------------

    public double totalLevantadoPorUtilizador(int utilizadorId) {
        String sql = """
                SELECT SUM(t.valor) FROM transacao t
                JOIN conta c ON t.contaId = c.id
                WHERE c.utilizadorId = ? AND t.tipo = 'levantamento'
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao calcular total levantado: " + e.getMessage());
        }
        return 0.0;
    }

    // ----------------------------------------------------------------
    // Interno — inserir transação genérica
    // ----------------------------------------------------------------

    private boolean inserir(Transaction t) {
        String sql = "INSERT INTO transacao (contaId, valor, tipo) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, t.getContaId());
            stmt.setDouble(2, t.getValor());
            stmt.setString(3, t.getTipo().name());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) t.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao inserir transação: " + e.getMessage());
        }
        return false;
    }

    private List<Transaction> listarPorUtilizadorETipo(int utilizadorId, Transaction.Tipo tipo) {
        List<Transaction> lista = new ArrayList<>();

        String sql = """
                SELECT t.* FROM transacao t
                JOIN conta c ON t.contaId = c.id
                WHERE c.utilizadorId = ? AND t.tipo = ?
                ORDER BY t.data DESC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao listar por tipo: " + e.getMessage());
        }
        return lista;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("data");
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("contaId"),
                rs.getDouble("valor"),
                Transaction.Tipo.valueOf(rs.getString("tipo")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
