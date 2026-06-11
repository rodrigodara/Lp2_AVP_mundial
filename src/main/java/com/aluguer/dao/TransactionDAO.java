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
 * ALV-166 — Registar pagamentos
 * ALV-167 — Registar recebimentos
 * ALV-169 — Testar consultas
 */
public class TransactionDAO {

    private final Connection conn;

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    // ----------------------------------------------------------------
    // ALV-166 — Registar pagamento (locatário pagou uma reserva)
    // ----------------------------------------------------------------

    public boolean registarPagamento(int reservaId, int utilizadorId, double valor) {
        Transaction t = new Transaction(
                reservaId,
                utilizadorId,
                Transaction.Tipo.PAGAMENTO,
                valor,
                "Pagamento pela reserva #" + reservaId
        );
        return inserir(t);
    }

    // ----------------------------------------------------------------
    // ALV-167 — Registar recebimento (proprietário recebeu dinheiro)
    // ----------------------------------------------------------------

    public boolean registarRecebimento(int reservaId, int proprietarioId, double valor) {
        Transaction t = new Transaction(
                reservaId,
                proprietarioId,
                Transaction.Tipo.RECEBIMENTO,
                valor,
                "Recebimento da reserva #" + reservaId
        );
        return inserir(t);
    }

    // ----------------------------------------------------------------
    // ALV-169 — Listar todas as transações de um utilizador
    // ----------------------------------------------------------------

    public List<Transaction> listarPorUtilizador(int utilizadorId) {
        List<Transaction> lista = new ArrayList<>();

        String sql = "SELECT * FROM transacao WHERE utilizadorId = ? ORDER BY data DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao listar transações: " + e.getMessage());
        }

        return lista;
    }

    // ALV-169 — Listar só pagamentos de um utilizador
    public List<Transaction> listarPagamentosPorUtilizador(int utilizadorId) {
        return listarPorUtilizadorETipo(utilizadorId, Transaction.Tipo.PAGAMENTO);
    }

    // ALV-169 — Listar só recebimentos de um utilizador
    public List<Transaction> listarRecebimentosPorUtilizador(int utilizadorId) {
        return listarPorUtilizadorETipo(utilizadorId, Transaction.Tipo.RECEBIMENTO);
    }

    // ALV-169 — Total pago por um utilizador
    public double totalPagoPorUtilizador(int utilizadorId) {
        String sql = "SELECT SUM(valor) FROM transacao WHERE utilizadorId = ? AND tipo = 'PAGAMENTO'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao calcular total pago: " + e.getMessage());
        }

        return 0.0;
    }

    // ALV-169 — Total recebido por um proprietário
    public double totalRecebidoPorUtilizador(int utilizadorId) {
        String sql = "SELECT SUM(valor) FROM transacao WHERE utilizadorId = ? AND tipo = 'RECEBIMENTO'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao calcular total recebido: " + e.getMessage());
        }

        return 0.0;
    }

    // ----------------------------------------------------------------
    // Interno — inserir transação genérica
    // ----------------------------------------------------------------

    private boolean inserir(Transaction t) {
        String sql = """
                INSERT INTO transacao (reservaId, utilizadorId, tipo, valor, data, descricao)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, t.getReservaId());
            stmt.setInt(2, t.getUtilizadorId());
            stmt.setString(3, t.getTipo().name());
            stmt.setDouble(4, t.getValor());
            stmt.setTimestamp(5, Timestamp.valueOf(t.getData()));
            stmt.setString(6, t.getDescricao());

            int linhas = stmt.executeUpdate();

            if (linhas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        t.setId(keys.getInt(1));
                    }
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

        String sql = "SELECT * FROM transacao WHERE utilizadorId = ? AND tipo = ? ORDER BY data DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Erro ao listar por tipo: " + e.getMessage());
        }

        return lista;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("reservaId"),
                rs.getInt("utilizadorId"),
                Transaction.Tipo.valueOf(rs.getString("tipo")),
                rs.getDouble("valor"),
                rs.getTimestamp("data").toLocalDateTime(),
                rs.getString("descricao")
        );
    }
}