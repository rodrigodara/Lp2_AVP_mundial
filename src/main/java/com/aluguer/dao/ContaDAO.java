package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
