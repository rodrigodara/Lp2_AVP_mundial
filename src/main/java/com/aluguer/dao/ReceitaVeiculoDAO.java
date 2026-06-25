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

        // Nota: a tabela `transacao` não tem reservaId nem utilizadorId — tem
        // contaId (que se liga a conta.utilizadorId) e tipo
        // 'recebimento_proprietario' para os pagamentos recebidos pelo dono.
        // Como a transação não está ligada a uma reserva/veículo específico,
        // a receita por veículo é calculada a partir de reserva.precoTotal
        // (consistente com "Os Meus Veículos"), não da tabela transacao.
        String sql = """
                SELECT
                    v.id              AS veiculoId,
                    v.marca,
                    v.modelo,
                    v.ano,
                    COUNT(r.id)  AS totalReservas,
                    COALESCE(SUM(CASE WHEN r.estado IN ('ACEITE', 'CONCLUIDO')
                                       THEN r.precoTotal * 0.85 ELSE 0 END), 0) AS receitaTotal
                FROM veiculo v
                LEFT JOIN reserva r
                    ON r.veiculoId = v.id
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
    // Evolução temporal da receita de um veículo, para o gráfico
    // (Dia / Semana / Mês / Ano), usado na comparação entre veículos.
    // ----------------------------------------------------------------

    /**
     * Devolve a evolução da receita (85% do precoTotal, já sem comissão)
     * de um veículo específico, agrupada pelo período indicado.
     *
     * @param veiculoId   id do veículo
     * @param agrupamento "DAY", "WEEK", "MONTH" ou "YEAR"
     * @return lista de Object[]{periodo (String), receita (Double)}, ordenada
     *         cronologicamente (mais antigo primeiro), limitada às últimas 30 entradas.
     */
    public List<Object[]> evolucaoReceitaPorVeiculo(int veiculoId, String agrupamento) {
        List<Object[]> lista = new ArrayList<>();

        String fmt = switch (agrupamento == null ? "MONTH" : agrupamento) {
            case "DAY"   -> "%Y-%m-%d";
            case "WEEK"  -> "%Y-S%u";
            case "YEAR"  -> "%Y";
            default      -> "%Y-%m"; // MONTH
        };

        // fmt vem de um switch interno fixo, não de input do utilizador — seguro
        // concatenar diretamente na query.
        String sql = "SELECT DATE_FORMAT(r.dataInicio, '" + fmt + "') AS periodo, "
                   + "       COALESCE(SUM(r.precoTotal * 0.85), 0) AS receita "
                   + "FROM reserva r "
                   + "WHERE r.veiculoId = ? "
                   + "  AND r.estado IN ('ACEITE', 'CONCLUIDO') "
                   + "GROUP BY periodo "
                   + "ORDER BY periodo ASC "
                   + "LIMIT 30";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getString("periodo"),
                        rs.getDouble("receita")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReceitaVeiculoDAO] Erro ao calcular evolução de receita: " + e.getMessage());
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
                SELECT COALESCE(SUM(r.precoTotal * 0.85), 0)
                FROM reserva r
                JOIN veiculo v ON r.veiculoId = v.id
                WHERE v.proprietarioId = ?
                  AND r.estado IN ('ACEITE', 'CONCLUIDO')
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
                  AND r.estado IN ('ACEITE', 'CONCLUIDO')
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