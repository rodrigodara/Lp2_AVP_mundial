package com.aluguer.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Availability;

/**
 * ALV-172 — Criar endpoint adicionar indisponibilidade
 * ALV-173 — Criar endpoint remover indisponibilidade
 * ALV-174 — Integrar com reservas
 */
public class AvailabilityDAO {

    private final Connection conn;

    public AvailabilityDAO(Connection conn) {
        this.conn = conn;
    }

    // ================================================================
    // ALV-172 — Adicionar indisponibilidade
    // ================================================================

    /**
     * Regista um período de indisponibilidade para um veículo.
     *
     * @param availability período a registar
     * @return true se inserido com sucesso
     */
    public boolean adicionar(Availability availability) {
        String sql = "INSERT INTO indisponibilidade (veiculoId, dataInicio, dataFim, motivo) " +
                     "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, availability.getVeiculoId());
            stmt.setDate(2, Date.valueOf(availability.getDataInicio()));
            stmt.setDate(3, Date.valueOf(availability.getDataFim()));
            stmt.setString(4, availability.getMotivo());

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) availability.setId(keys.getInt(1));
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("[AvailabilityDAO] Erro ao adicionar: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // ALV-173 — Remover indisponibilidade
    // ================================================================

    /**
     * Remove um período de indisponibilidade pelo id.
     *
     * @param id id da indisponibilidade a remover
     * @return true se removido com sucesso
     */
    public boolean remover(int id) {
        String sql = "DELETE FROM indisponibilidade WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AvailabilityDAO] Erro ao remover: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // Listar por veículo
    // ================================================================

    public List<Availability> listarPorVeiculo(int veiculoId) {
        List<Availability> lista = new ArrayList<>();
        String sql = "SELECT * FROM indisponibilidade WHERE veiculoId = ? ORDER BY dataInicio ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[AvailabilityDAO] Erro ao listar: " + e.getMessage());
        }
        return lista;
    }

    // ================================================================
    // ALV-174 — Verificar se veículo está indisponível nas datas
    // ================================================================

    /**
     * Verifica se um veículo tem indisponibilidade registada nas datas indicadas.
     * Usado na validação antes de aceitar uma reserva.
     *
     * @param veiculoId id do veículo
     * @param inicio    data de início da reserva
     * @param fim       data de fim da reserva
     * @return true se existir sobreposição com indisponibilidade
     */
    public boolean estaIndisponivel(int veiculoId, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT 1 FROM indisponibilidade " +
                     "WHERE veiculoId = ? AND dataInicio < ? AND dataFim > ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            stmt.setDate(2, Date.valueOf(fim));
            stmt.setDate(3, Date.valueOf(inicio));
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("[AvailabilityDAO] Erro ao verificar disponibilidade: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // Mapeamento ResultSet → Availability
    // ================================================================

    private Availability mapRow(ResultSet rs) throws SQLException {
        return new Availability(
            rs.getInt("id"),
            rs.getInt("veiculoId"),
            rs.getDate("dataInicio").toLocalDate(),
            rs.getDate("dataFim").toLocalDate(),
            rs.getString("motivo")
        );
    }
}