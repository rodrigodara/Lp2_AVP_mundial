package com.aluguer.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Reserva;

/**
 * ALV-47 / ALV-52 — DAO para operações de reserva na base de dados.
 *
 * Responsabilidades:
 *   - Criar pedido de reserva (ALV-52)
 *   - Buscar reserva por id
 *   - Listar reservas por utilizador (locatário)
 *   - Listar reservas por veículo (proprietário)
 *   - Atualizar estado — aceite/rejeitado (ALV-51 / ALV-89)
 *   - Verificar sobreposição de datas (RF4)
 *   - Listar pedidos recebidos por proprietário (ALV-90)
 */
public class ReservaDAO {

    private Connection conn;

    public ReservaDAO(Connection conn) {
        this.conn = conn;
    }

    // ============================
    // 1. INSERIR RESERVA — ALV-52
    // ============================
    public boolean inserir(Reserva r) {
        if (existeSobreposicao(r.getVeiculoId(), r.getDataInicio(), r.getDataFim(), -1)) {
            System.err.println("[ReservaDAO] Sobreposição de datas detetada.");
            return false;
        }

        String sql = "INSERT INTO reserva (utilizadorId, veiculoId, dataInicio, dataFim, " +
                     "estado, precoTotal, caucao, kmInicial, kmFinal) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, r.getUtilizadorId());
            stmt.setInt(2, r.getVeiculoId());
            stmt.setDate(3, Date.valueOf(r.getDataInicio()));
            stmt.setDate(4, Date.valueOf(r.getDataFim()));
            stmt.setString(5, r.getEstado().name());
            stmt.setDouble(6, r.getPrecoTotal());
            stmt.setDouble(7, r.getCaucao());
            stmt.setInt(8, r.getKmInicial());
            stmt.setInt(9, r.getKmFinal());

            boolean ok = stmt.executeUpdate() > 0;

            if (ok) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) r.setId(keys.getInt(1));
            }
            return ok;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================
    // 2. BUSCAR POR ID — ALV-52
    // ============================
    public Reserva buscarPorId(int id) {
        String sql = "SELECT * FROM reserva WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ============================
    // 3. LISTAR POR UTILIZADOR
    // ============================
    public List<Reserva> listarPorUtilizador(int utilizadorId) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT * FROM reserva WHERE utilizadorId = ? ORDER BY dataInicio DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapRow(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ============================
    // 4. LISTAR POR VEÍCULO
    // ============================
    public List<Reserva> listarPorVeiculo(int veiculoId) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT * FROM reserva WHERE veiculoId = ? ORDER BY dataInicio DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapRow(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ============================
    // 5. ATUALIZAR ESTADO — ALV-51 / ALV-89
    // ============================
    public boolean atualizarEstado(int id, Reserva.Estado estado) {
        String sql = "UPDATE reserva SET estado = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================
    // 6. VERIFICAR SOBREPOSIÇÃO
    // ============================
    /**
     * Verifica se existe reserva ACEITE para o veículo nas datas indicadas.
     * Lógica: duas reservas sobrepõem-se se inicio1 < fim2 AND fim1 > inicio2
     *
     * @param excluirId id a ignorar na verificação (-1 para nenhum)
     */
    public boolean existeSobreposicao(int veiculoId, LocalDate inicio, LocalDate fim, int excluirId) {
        String sql = "SELECT 1 FROM reserva " +
                     "WHERE veiculoId = ? AND estado = 'ACEITE' " +
                     "AND dataInicio < ? AND dataFim > ? AND id != ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            stmt.setDate(2, Date.valueOf(fim));
            stmt.setDate(3, Date.valueOf(inicio));
            stmt.setInt(4, excluirId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================
    // 7. LISTAR PENDENTES POR PROPRIETÁRIO — ALV-90
    // ============================
    /**
     * Devolve todas as reservas PENDENTES dos veículos pertencentes
     * ao proprietário indicado. Usado na página "Pedidos Recebidos".
     */
    public List<Reserva> listarPendentesPorProprietario(int proprietarioId) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.* FROM reserva r " +
                     "JOIN veiculo v ON r.veiculoId = v.id " +
                     "WHERE v.proprietarioId = ? AND r.estado = 'PENDENTE' " +
                     "ORDER BY r.dataInicio ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ============================
    // 8. LISTAR TODAS POR PROPRIETÁRIO — ALV-90
    // ============================
    /**
     * Devolve todas as reservas (todos os estados) dos veículos do proprietário.
     * Permite filtrar por estado na camada de UI.
     */
    public List<Reserva> listarTodasPorProprietario(int proprietarioId) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.* FROM reserva r " +
                     "JOIN veiculo v ON r.veiculoId = v.id " +
                     "WHERE v.proprietarioId = ? " +
                     "ORDER BY r.estado ASC, r.dataInicio DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ============================
    // MAPEAMENTO ResultSet → Reserva
    // ============================
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
