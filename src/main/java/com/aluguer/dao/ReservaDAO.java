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

import com.aluguer.model.Reserva;

public class ReservaDAO {

    private final Connection conn;

    public Connection getConnection() { return conn; }

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
        // Quando muda para ACEITE, REJEITADO ou CANCELADO grava o timestamp e limpa a flag de lida
        boolean registarData = (estado == Reserva.Estado.ACEITE
            || estado == Reserva.Estado.REJEITADO
            || estado == Reserva.Estado.CANCELADO);
        String sql = registarData
            ? "UPDATE reserva SET estado = ?, estado_data = NOW(), notif_lida = 0 WHERE id = ?"
            : "UPDATE reserva SET estado = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar estado da reserva: " + e.getMessage());
            return false;
        }
    }

    /** Marca a notificação de uma reserva como lida. */
    public boolean marcarNotifLida(int reservaId) {
        String sql = "UPDATE reserva SET notif_lida = 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao marcar notif como lida: " + e.getMessage());
            return false;
        }
    }
    
    public boolean cancelar(int id) {
    return atualizarEstado(id, Reserva.Estado.CANCELADO);
    }

    /** Atualiza o estado da caução: DEVOLVIDA | RETIDA | EM_DISPUTA. */
    public boolean atualizarCaucaoEstado(int reservaId, String caucaoEstado) {
        String sql = "UPDATE reserva SET caucao_estado = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, caucaoEstado);
            stmt.setInt(2, reservaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar estado da caução: " + e.getMessage());
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

    // ------------------------------------------------------------------
    // ALV-129 — Verificar sobreposição de pedidos do mesmo utilizador
    // ------------------------------------------------------------------

    /**
     * Verifica se o utilizador já tem um pedido (PENDENTE ou ACEITE)
     * para o mesmo veículo que se sobreponha ao período indicado.
     * Usado para impedir pedidos duplicados ou sobrepostos pelo mesmo utilizador.
     */
    public boolean existeSobreposicaoPorUtilizador(int utilizadorId, int veiculoId,
                                                    LocalDate inicio, LocalDate fim,
                                                    int excluirId) {
        String sql = """
                SELECT 1 FROM reserva
                WHERE utilizadorId = ?
                  AND veiculoId    = ?
                  AND estado IN ('PENDENTE', 'ACEITE')
                  AND dataInicio <= ?
                  AND dataFim    >= ?
                  AND id != ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setInt(2, veiculoId);
            stmt.setDate(3, Date.valueOf(fim));
            stmt.setDate(4, Date.valueOf(inicio));
            stmt.setInt(5, excluirId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao verificar sobreposição por utilizador: " + e.getMessage());
            return false;
        }
    }
    // ------------------------------------------------------------------
    // Bloquear reserva dupla — verificar se o utilizador já tem
    // alguma reserva ACEITE, independentemente do veículo. O utilizador
    // pode ter vários pedidos PENDENTES em simultâneo (em carros
    // diferentes ou no mesmo), mas só pode ter UMA reserva ACEITE de
    // cada vez, pois ao aceitar fica com o carro e só pode devolvê-lo
    // depois de o período terminar.
    // ------------------------------------------------------------------

    /**
     * Verifica se o utilizador já tem alguma reserva no estado ACEITE,
     * em qualquer veículo, excluindo opcionalmente uma reserva específica
     * (útil em atualizações).
     *
     * @param utilizadorId id do utilizador
     * @param excluirId    id de reserva a ignorar na verificação (-1 para nenhuma)
     */
    public boolean existeReservaAceitePorUtilizador(int utilizadorId, int excluirId) {
        String sql = """
                SELECT 1 FROM reserva
                WHERE utilizadorId = ?
                  AND estado = 'ACEITE'
                  AND id != ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setInt(2, excluirId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao verificar reserva aceite do utilizador: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Rejeição automática — listar todos os pedidos PENDENTES de um
    // utilizador (em qualquer veículo), usado quando um dos seus
    // pedidos é aceite para rejeitar automaticamente os restantes.
    // ------------------------------------------------------------------

    /**
     * Devolve todos os pedidos PENDENTES de um utilizador, em qualquer
     * veículo. Usado para rejeitar automaticamente os restantes pedidos
     * pendentes do utilizador quando um deles é aceite.
     */
    public List<Reserva> listarPendentesPorUtilizador(int utilizadorId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = """
                SELECT * FROM reserva
                WHERE utilizadorId = ?
                  AND estado = 'PENDENTE'
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar pendentes do utilizador: " + e.getMessage());
        }

        return reservas;
    }

    // ------------------------------------------------------------------
    // ALV-143 — Registar km inicial
    // ------------------------------------------------------------------

    /**
     * Regista a quilometragem inicial do veículo na reserva, no momento
     * em que o aluguer é aceite.
     *
     * O valor corresponde à quilometragem atual do veículo e é usado
     * posteriormente para calcular os quilómetros percorridos.
     */
    public boolean atualizarKmInicial(int reservaId, int kmInicial) {
        String sql = "UPDATE reserva SET kmInicial = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, kmInicial);
            stmt.setInt(2, reservaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar kmInicial: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // ALV-144 — Registar km final
    // ------------------------------------------------------------------

    /**
     * Atualiza o kmFinal da reserva no encerramento do aluguer.
     * O valor deve ser >= kmInicial.
     */
    public boolean atualizarKmFinal(int reservaId, int kmFinal) {
        String sql = "UPDATE reserva SET kmFinal = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, kmFinal);
            stmt.setInt(2, reservaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar kmFinal: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarPrecoTotal(int reservaId, double precoTotal) {
        String sql = "UPDATE reserva SET precoTotal = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, precoTotal);
            stmt.setInt(2, reservaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao atualizar precoTotal: " + e.getMessage());
            return false;
        }
    }




    // ------------------------------------------------------------------
    // ALV-127 — Verificar reservas existentes para um veículo
    // ------------------------------------------------------------------

    /**
     * Devolve todas as reservas PENDENTES ou ACEITES de um veículo,
     * ordenadas por data de início.
     * Usado para mostrar ao utilizador quais períodos já estão ocupados.
     */
    public List<Reserva> listarReservasAtivasPorVeiculo(int veiculoId) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = """
                SELECT * FROM reserva
                WHERE veiculoId = ?
                  AND estado IN ('PENDENTE', 'ACEITE')
                ORDER BY dataInicio ASC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar reservas ativas: " + e.getMessage());
        }

        return reservas;
    }

    // ------------------------------------------------------------------
    // ALV-128 — Verificar datas bloqueadas (apenas reservas ACEITES)
    // ------------------------------------------------------------------

    /**
     * Devolve todas as reservas ACEITES de um veículo num dado intervalo.
     * Permite à UI mostrar exatamente quais datas estão bloqueadas
     * (confirmadas pelo proprietário) vs. apenas pendentes.
     *
     * @param veiculoId  id do veículo
     * @param de         início do intervalo de pesquisa (inclusive)
     * @param ate        fim do intervalo de pesquisa (inclusive)
     */
    public List<Reserva> listarDatasBloquadas(int veiculoId, LocalDate de, LocalDate ate) {
        List<Reserva> reservas = new ArrayList<>();

        String sql = """
                SELECT * FROM reserva
                WHERE veiculoId = ?
                  AND estado = 'ACEITE'
                  AND dataInicio <= ?
                  AND dataFim    >= ?
                ORDER BY dataInicio ASC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            stmt.setDate(2, Date.valueOf(ate));
            stmt.setDate(3, Date.valueOf(de));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReservaDAO] Erro ao listar datas bloqueadas: " + e.getMessage());
        }

        return reservas;
    }

    private Reserva mapRow(ResultSet rs) throws SQLException {
        Reserva r = new Reserva(
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
        // campos novos — podem ser NULL se a reserva ainda é PENDENTE
        java.sql.Timestamp ts = rs.getTimestamp("estado_data");
        if (ts != null) r.setEstadoData(ts.toLocalDateTime());
        r.setNotifLida(rs.getBoolean("notif_lida"));
        r.setCaucaoEstado(rs.getString("caucao_estado"));
        return r;
    }
}