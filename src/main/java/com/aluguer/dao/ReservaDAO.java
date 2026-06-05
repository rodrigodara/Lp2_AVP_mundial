package com.aluguer.dao;

import com.aluguer.model.Reserva;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservaDAO {

    private final Connection conn;

    public ReservaDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean inserir(Reserva reserva) {
        if (existeSobreposicao(reserva.getVeiculoId(), reserva.getDataInicio(), reserva.getDataFim(), -1)) {
            System.err.println("[ReservaDAO] Sobreposição de datas detetada.");
            return false;
        }

        String sql = """
                INSERT INTO reserva 
                (utilizadorId, veiculoId, dataInicio, dataFim, estado, precoTotal, caucao, kmInicial, kmFinal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reserva.getUtilizadorId());
            stmt.setInt(2, reserva.getVeiculoId());
            stmt.setDate(3, Date.valueOf(reserva.getDataInicio()));
            stmt.setDate(4, Date.valueOf(reserva.getDataFim()));
            stmt.setString(5, reserva.getEstado().name());
            stmt.setDouble(6, reserva.getPrecoTotal());
            stmt.setDouble(7, reserva.getCaucao());
            stmt.setInt(8, reserva.getKmInicial());
            stmt.setInt(9, reserva.getKmFinal());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        reserva.setId(keys.getInt(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao inserir reserva: " + e.getMessage());
        }

        return false;
    }

    public Reserva buscarPorId(int id) {
        String sql = "SELECT * FROM reserva WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao buscar reserva: " + e.getMessage());
        }

        return null;
    }

    public List<Reserva> listarPorUtilizador(int utilizadorId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = "SELECT * FROM reserva WHERE utilizadorId = ? ORDER BY dataInicio DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar reservas do utilizador: " + e.getMessage());
        }

        return reservas;
    }

    public List<Reserva> listarPorVeiculo(int veiculoId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = "SELECT * FROM reserva WHERE veiculoId = ? ORDER BY dataInicio DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar reservas do veículo: " + e.getMessage());
        }

        return reservas;
    }

    public boolean atualizarEstado(int id, Reserva.Estado estado) {
        String sql = "UPDATE reserva SET estado = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setInt(2, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar estado da reserva: " + e.getMessage());
            return false;
        }
    }

    public boolean existeSobreposicao(int veiculoId, LocalDate inicio, LocalDate fim, int excluirId) {
        String sql = """
                SELECT 1 FROM reserva
                WHERE veiculoId = ?
                  AND estado = 'ACEITE'
                  AND dataInicio <= ?
                  AND dataFim >= ?
                  AND id != ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, veiculoId);
            stmt.setDate(2, Date.valueOf(fim));
            stmt.setDate(3, Date.valueOf(inicio));
            stmt.setInt(4, excluirId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao verificar sobreposição: " + e.getMessage());
            return false;
        }
    }

    public List<Reserva> listarPendentesPorProprietario(int proprietarioId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = """
                SELECT r.* FROM reserva r
                JOIN veiculo v ON r.veiculoId = v.id
                WHERE v.proprietarioId = ?
                  AND r.estado = 'PENDENTE'
                ORDER BY r.dataInicio ASC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar reservas pendentes: " + e.getMessage());
        }

        return reservas;
    }

    public List<Reserva> listarTodasPorProprietario(int proprietarioId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = """
                SELECT r.* FROM reserva r
                JOIN veiculo v ON r.veiculoId = v.id
                WHERE v.proprietarioId = ?
                ORDER BY r.estado ASC, r.dataInicio DESC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar reservas do proprietário: " + e.getMessage());
        }

        return reservas;
    }

    private Reserva mapRow(ResultSet rs) throws SQLException {
        return new Reserva(
                rs.getInt("id"),
                rs.getInt("utilizadorId"),
                rs.getInt("veiculoId"),
                rs.getDate("dataInicio").toLocalDate(),
                rs.getDate("dataFim").toLocalDate(),
                Reserva.Estado.valueOf(rs.getString("estado")),
                rs.getDouble("precoTotal"),
                rs.getDouble("caucao"),
                rs.getInt("kmInicial"),
                rs.getInt("kmFinal")
        );
    }
}