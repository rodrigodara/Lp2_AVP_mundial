package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.ReceitaVeiculo;

/**
 * ALV-187 — Calcular receita total
 * ALV-188 — Agrupar por veículo
 * ALV-189 — Criar endpoint estatísticas
 * ALV-191 — Testar cálculos
 *
 * DAO que agrega receitas das transações por veículo do proprietário.
 * Usa JOIN entre transacao, reserva e veiculo para calcular os totais.
 */
public class ReceitaVeiculoDAO {

    private final Connection conn;

    public ReceitaVeiculoDAO(Connection conn) {
        this.conn = conn;
    }

    // ----------------------------------------------------------------
    // ALV-187 + ALV-188 — Receita total agrupada por veículo
    // Devolve lista ordenada por receita decrescente (maior primeiro)
    // ----------------------------------------------------------------

    /**
     * Lista a receita acumulada de cada veículo do proprietário,
     * somando todos os RECEBIMENTOS associados às reservas desse veículo.
     *
     * @param proprietarioId  id do proprietário
     * @return lista de ReceitaVeiculo ordenada por receitaTotal DESC
     */
    public List<ReceitaVeiculo> listarReceitaPorVeiculo(int proprietarioId) {
        List<ReceitaVeiculo> lista = new ArrayList<>();

        // ALV-188: GROUP BY veiculoId agrupa por veículo
        // ALV-187: SUM(t.valor) calcula a receita total por veículo
        String sql = """
                SELECT
                    v.id              AS veiculoId,
                    v.marca,
                    v.modelo,
                    v.ano,
                    COUNT(DISTINCT r.id)  AS totalReservas,
                    COALESCE(SUM(t.valor), 0) AS receitaTotal
                FROM veiculo v
                LEFT JOIN reserva r
                    ON r.veiculoId = v.id
                    AND r.estado = 'ACEITE'
                LEFT JOIN transacao t
                    ON t.reservaId = r.id
                    AND t.utilizadorId = v.proprietarioId
                    AND t.tipo = 'RECEBIMENTO'
                WHERE v.proprietarioId = ?
                GROUP BY v.id, v.marca, v.modelo, v.ano
                ORDER BY receitaTotal DESC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ReceitaVeiculo(
                            rs.getInt("veiculoId"),
                            rs.getString("marca"),
                            rs.getString("modelo"),
                            rs.getInt("ano"),
                            rs.getInt("totalReservas"),
                            rs.getDouble("receitaTotal")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReceitaVeiculoDAO] Erro ao calcular receitas: " + e.getMessage());
        }

        return lista;
    }

    // ----------------------------------------------------------------
    // ALV-189 — Endpoint estatísticas: totais globais do proprietário
    // ----------------------------------------------------------------

    /**
     * Devolve o total acumulado de receitas de todos os veículos
     * do proprietário (valor único para mostrar no card de resumo).
     *
     * @param proprietarioId  id do proprietário
     * @return soma de todos os recebimentos
     */
    public double receitaTotalProprietario(int proprietarioId) {
        String sql = """
                SELECT COALESCE(SUM(t.valor), 0)
                FROM transacao t
                JOIN reserva r  ON t.reservaId = r.id
                JOIN veiculo v  ON r.veiculoId  = v.id
                WHERE v.proprietarioId = ?
                  AND t.tipo = 'RECEBIMENTO'
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[ReceitaVeiculoDAO] Erro ao calcular receita total: " + e.getMessage());
        }

        return 0.0;
    }

    // ----------------------------------------------------------------
    // ALV-189 — Contar total de reservas aceites do proprietário
    // ----------------------------------------------------------------

    public int totalReservasAceitesProprietario(int proprietarioId) {
        String sql = """
                SELECT COUNT(r.id)
                FROM reserva r
                JOIN veiculo v ON r.veiculoId = v.id
                WHERE v.proprietarioId = ?
                  AND r.estado = 'ACEITE'
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ReceitaVeiculoDAO] Erro ao contar reservas: " + e.getMessage());
        }

        return 0;
    }

    // ----------------------------------------------------------------
    // ALV-189 — Contar total de veículos do proprietário
    // ----------------------------------------------------------------

    public int totalVeiculosProprietario(int proprietarioId) {
        String sql = "SELECT COUNT(*) FROM veiculo WHERE proprietarioId = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ReceitaVeiculoDAO] Erro ao contar veículos: " + e.getMessage());
        }

        return 0;
    }
}