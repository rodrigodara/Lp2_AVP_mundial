package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ContaDAO {

    private final Connection conn;

    public ContaDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Atualiza o saldo da conta do utilizador.
     * valor > 0 → adiciona
     * valor < 0 → subtrai
     */
    public boolean atualizarSaldo(int utilizadorId, double valor) {
        String sql = "UPDATE conta SET saldo = saldo + ? WHERE utilizadorId = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, valor);
            stmt.setInt(2, utilizadorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ContaDAO] Erro ao atualizar saldo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Devolve o id da conta (tabela `conta`) associada a este utilizador.
     * Se o utilizador ainda não tiver uma linha em `conta` (ex.: contas
     * criadas antes desta funcionalidade existir), cria-a automaticamente
     * com saldo 0 e devolve o novo id — assim a listagem de transações
     * nunca fica "presa" por falta de conta.
     */
    public int obterOuCriarContaId(int utilizadorId) throws SQLException {
        String sqlBuscar = "SELECT id FROM conta WHERE utilizadorId = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlBuscar)) {
            ps.setInt(1, utilizadorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String sqlCriar = "INSERT INTO conta (utilizadorId, saldo) VALUES (?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sqlCriar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, utilizadorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        throw new SQLException("Não foi possível obter nem criar conta para o utilizador #" + utilizadorId);
    }
}