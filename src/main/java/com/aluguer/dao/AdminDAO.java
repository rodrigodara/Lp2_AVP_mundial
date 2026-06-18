package com.aluguer.dao;

import com.aluguer.model.User;
import com.aluguer.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminDAO — operações de base de dados exclusivas do painel de administração.
 *
 * Tabelas usadas (schema aluguer_veiculos):
 *   utilizadores  — id, email, nome, nif, numero_carta, validade_carta,
 *                   password_hash, tipo, saldo, perfil, ativo, data_criacao
 *   veiculo       — id, marca, modelo, ano, combustivel, precoDiario,
 *                   localizacao, proprietarioId, estado
 *   reserva       — id, utilizadorId, veiculoId, dataInicio, dataFim,
 *                   estado (PENDENTE/ACEITE/REJEITADO/CONCLUIDO),
 *                   precoTotal, caucao, kmInicial, kmFinal
 *   avisos_admin  — id, utilizadorId, motivo, data_aviso  [criada via SQL]
 *
 * Lógica de avisos:
 *   1.º e 2.º aviso → só regista
 *   3.º aviso       → bane automaticamente (ativo = 0)
 */
public class AdminDAO {

    // =========================================================================
    // 1. GESTÃO DE UTILIZADORES
    // =========================================================================

    /**
     * Lista todos os utilizadores, exceto o admin logado,
     * ordenados por nome. Inclui o campo 'tipo' da BD.
     */
    public List<User> listarUtilizadores(int adminId) throws SQLException {
        List<User> lista = new ArrayList<>();
        String sql = "SELECT * FROM utilizadores WHERE id <> ? ORDER BY nome";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapUser(rs));
            }
        }
        return lista;
    }

    /** Pesquisa utilizadores por nome ou email (LIKE, case-insensitive via utf8mb4). */
    public List<User> pesquisarUtilizadores(String termo, int adminId) throws SQLException {
        List<User> lista = new ArrayList<>();
        String like = "%" + termo.trim() + "%";
        String sql = "SELECT * FROM utilizadores "
                   + "WHERE id <> ? AND (nome LIKE ? OR email LIKE ?) "
                   + "ORDER BY nome";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapUser(rs));
            }
        }
        return lista;
    }

    /**
     * Bloqueia (ativo=0) ou desbloqueia (ativo=1) a conta de um utilizador.
     * Devolve true se a linha foi atualizada.
     */
    public boolean setAtivo(int userId, boolean ativo) throws SQLException {
        String sql = "UPDATE utilizadores SET ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 2. SISTEMA DE AVISOS  (tabela avisos_admin — ver avisos_admin.sql)
    // =========================================================================

    /** Conta quantos avisos foram emitidos a um utilizador. */
    public int contarAvisos(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM avisos_admin WHERE utilizadorId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Emite um aviso ao utilizador.
     * Regra de negócio:
     *   - 1.º e 2.º aviso → só regista, conta permanece ativa
     *   - 3.º aviso       → bane automaticamente (ativo = 0)
     *
     * @return true se o utilizador foi banido nesta chamada, false caso contrário
     */
    public boolean emitirAviso(int userId, String motivo) throws SQLException {
        String ins = "INSERT INTO avisos_admin (utilizadorId, motivo, data_aviso) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setInt(1, userId);
            ps.setString(2, motivo);
            ps.executeUpdate();
        }
        int total = contarAvisos(userId);
        if (total >= 3) {
            setAtivo(userId, false);
            return true;   // banido
        }
        return false;
    }

    /**
     * Lista o histórico de avisos de um utilizador, do mais recente para o mais antigo.
     * Cada entrada é String[]{motivo, data_aviso}.
     */
    public List<String[]> listarAvisos(int userId) throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT motivo, data_aviso "
                   + "FROM avisos_admin WHERE utilizadorId = ? "
                   + "ORDER BY data_aviso DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new String[]{
                        rs.getString("motivo"),
                        rs.getTimestamp("data_aviso").toString()
                    });
                }
            }
        }
        return lista;
    }

    // =========================================================================
    // 3. GESTÃO DE VEÍCULOS
    // =========================================================================

    /**
     * Lista todos os veículos com o nome e id do proprietário.
     * Devolve Object[]{id, marca, modelo, ano, combustivel,
     *                   precoDiario, localizacao, estado,
     *                   nomeProprietario, proprietarioId}
     */
    public List<Object[]> listarVeiculosComProprietario() throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT v.id, v.marca, v.modelo, v.ano, v.combustivel, "
                   + "       v.precoDiario, v.localizacao, v.estado, "
                   + "       u.nome AS proprietario, u.id AS proprietarioId "
                   + "FROM veiculo v "
                   + "JOIN utilizadores u ON v.proprietarioId = u.id "
                   + "ORDER BY v.marca, v.modelo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getInt("ano"),
                    rs.getString("combustivel"),
                    rs.getDouble("precoDiario"),
                    rs.getString("localizacao"),
                    rs.getString("estado"),
                    rs.getString("proprietario"),
                    rs.getInt("proprietarioId")
                });
            }
        }
        return lista;
    }

    /** Pesquisa veículos por marca, modelo, localização ou nome do proprietário. */
    public List<Object[]> pesquisarVeiculos(String termo) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String like = "%" + termo.trim() + "%";
        String sql = "SELECT v.id, v.marca, v.modelo, v.ano, v.combustivel, "
                   + "       v.precoDiario, v.localizacao, v.estado, "
                   + "       u.nome AS proprietario, u.id AS proprietarioId "
                   + "FROM veiculo v "
                   + "JOIN utilizadores u ON v.proprietarioId = u.id "
                   + "WHERE v.marca LIKE ? OR v.modelo LIKE ? "
                   + "   OR v.localizacao LIKE ? OR u.nome LIKE ? "
                   + "ORDER BY v.marca, v.modelo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("ano"),
                        rs.getString("combustivel"),
                        rs.getDouble("precoDiario"),
                        rs.getString("localizacao"),
                        rs.getString("estado"),
                        rs.getString("proprietario"),
                        rs.getInt("proprietarioId")
                    });
                }
            }
        }
        return lista;
    }

    /**
     * Detalhe completo de um veículo, para o Admin analisar se está
     * em conformidade com as regras da plataforma. Apenas leitura —
     * o Admin não edita dados de veículos, só consulta.
     *
     * @return Object[]{
     *   0 id, 1 marca, 2 modelo, 3 ano, 4 combustivel, 5 precoDiario,
     *   6 localizacao, 7 estado, 8 matricula, 9 quilometragem,
     *   10 proprietarioNome, 11 proprietarioEmail, 12 proprietarioId,
     *   13 totalReservas, 14 receitaTotal
     * }, ou null se o veículo não existir.
     */
    public Object[] obterDetalheVeiculo(int veiculoId) throws SQLException {
        String sql = "SELECT v.id, v.marca, v.modelo, v.ano, v.combustivel, v.precoDiario, "
                   + "       v.localizacao, v.estado, v.matricula, v.quilometragem, "
                   + "       u.nome AS proprietarioNome, u.email AS proprietarioEmail, u.id AS proprietarioId, "
                   + "       (SELECT COUNT(*) FROM reserva r WHERE r.veiculoId = v.id) AS totalReservas, "
                   + "       (SELECT COALESCE(SUM(r.precoTotal), 0) FROM reserva r "
                   + "          WHERE r.veiculoId = v.id AND r.estado IN ('ACEITE', 'CONCLUIDO')) AS receitaTotal "
                   + "FROM veiculo v "
                   + "JOIN utilizadores u ON v.proprietarioId = u.id "
                   + "WHERE v.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, veiculoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("ano"),
                        rs.getString("combustivel"),
                        rs.getDouble("precoDiario"),
                        rs.getString("localizacao"),
                        rs.getString("estado"),
                        rs.getString("matricula"),
                        rs.getInt("quilometragem"),
                        rs.getString("proprietarioNome"),
                        rs.getString("proprietarioEmail"),
                        rs.getInt("proprietarioId"),
                        rs.getInt("totalReservas"),
                        rs.getDouble("receitaTotal")
                    };
                }
            }
        }
        return null;
    }

    /**
     * Remove um veículo por violação de regras.
     *
     * Ordem de eliminação para respeitar as FK do schema:
     *   1. pagamento  (FK → reserva)
     *   2. avaliacao  (FK → reserva)
     *   3. reserva    (FK → veiculo)
     *   4. indisponibilidade (FK → veiculo)
     *   5. veiculo
     *
     * Usa uma única Connection para garantir consistência.
     */
    public boolean removerVeiculo(int veiculoId) throws SQLException {
        String delPagamento        = "DELETE FROM pagamento WHERE reservaId IN "
                                   + "(SELECT id FROM reserva WHERE veiculoId = ?)";
        String delAvaliacao        = "DELETE FROM avaliacao WHERE reservaId IN "
                                   + "(SELECT id FROM reserva WHERE veiculoId = ?)";
        String delReserva          = "DELETE FROM reserva WHERE veiculoId = ?";
        String delIndisponibilidade = "DELETE FROM indisponibilidade WHERE veiculoId = ?";
        String delVeiculo          = "DELETE FROM veiculo WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : new String[]{
                        delPagamento, delAvaliacao,
                        delReserva, delIndisponibilidade, delVeiculo}) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, veiculoId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // 4. ESTATÍSTICAS
    // =========================================================================

    /**
     * Totais gerais da plataforma.
     * Devolve int[6]:
     *   [0] total utilizadores
     *   [1] contas ativas
     *   [2] contas bloqueadas
     *   [3] total veículos
     *   [4] total reservas
     *   [5] receita total (ACEITE + CONCLUIDO) — arredondada para inteiro
     */
    public int[] estatisticasGerais() throws SQLException {
        int[] s = new int[6];
        try (Connection conn = DatabaseConnection.getConnection()) {

            // utilizadores: ativo é tinyint(1) → SUM funciona diretamente
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) AS total, "
                  + "       SUM(ativo) AS ativos, "
                  + "       SUM(1 - ativo) AS bloqueados "
                  + "FROM utilizadores");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s[0] = rs.getInt("total");
                    s[1] = rs.getInt("ativos");
                    s[2] = rs.getInt("bloqueados");
                }
            }

            // veículos
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM veiculo");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) s[3] = rs.getInt(1);
            }

            // reservas
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reserva");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) s[4] = rs.getInt(1);
            }

            // receita (estado ENUM: ACEITE ou CONCLUIDO — sem acento, conforme schema)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(precoTotal), 0) "
                  + "FROM reserva "
                  + "WHERE estado IN ('ACEITE', 'CONCLUIDO')");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) s[5] = rs.getInt(1);
            }
        }
        return s;
    }

    /**
     * Reservas agrupadas por período temporal.
     *
     * @param agrupamento "DAY" | "MONTH" | "YEAR"
     * @return Lista de Object[]{String periodo, int total, double receita}
     *         ordenada do mais recente para o mais antigo (máx. 24 linhas)
     */
    public List<Object[]> estatisticasPorPeriodo(String agrupamento) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String fmt = switch (agrupamento) {
            case "DAY"  -> "%Y-%m-%d";
            case "YEAR" -> "%Y";
            default     -> "%Y-%m";      // MONTH (default)
        };
        // Construção segura: fmt vem de um switch interno, não de input do utilizador
        String sql = "SELECT DATE_FORMAT(dataInicio, '" + fmt + "') AS periodo, "
                   + "       COUNT(*)                               AS total, "
                   + "       COALESCE(SUM(precoTotal), 0)           AS receita "
                   + "FROM reserva "
                   + "GROUP BY periodo "
                   + "ORDER BY periodo DESC "
                   + "LIMIT 24";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString("periodo"),
                    rs.getInt("total"),
                    rs.getDouble("receita")
                });
            }
        }
        return lista;
    }

    /**
     * Reservas e receita agrupadas por marca de veículo.
     * Inclui veículos sem reservas (COUNT = 0) via LEFT JOIN.
     * @return Lista de Object[]{String marca, int total, double receita}
     */
    public List<Object[]> estatisticasPorMarca() throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT v.marca, "
                   + "       COUNT(r.id)                    AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0) AS receita "
                   + "FROM veiculo v "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id "
                   + "GROUP BY v.marca "
                   + "ORDER BY total DESC, v.marca";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString("marca"),
                    rs.getInt("total"),
                    rs.getDouble("receita")
                });
            }
        }
        return lista;
    }

    /**
     * Reservas e receita agrupadas por região (campo localizacao do veículo).
     * Inclui regiões sem reservas via LEFT JOIN.
     * @return Lista de Object[]{String regiao, int total, double receita}
     */
    public List<Object[]> estatisticasPorRegiao() throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT v.localizacao               AS regiao, "
                   + "       COUNT(r.id)                    AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0) AS receita "
                   + "FROM veiculo v "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id "
                   + "GROUP BY v.localizacao "
                   + "ORDER BY total DESC, v.localizacao";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString("regiao"),
                    rs.getInt("total"),
                    rs.getDouble("receita")
                });
            }
        }
        return lista;
    }

    // =========================================================================
    // HELPER PRIVADO
    // =========================================================================

    /**
     * Mapeia uma linha do ResultSet da tabela `utilizadores` para um objeto User.
     * Colunas usadas: id, email, nome, nif, numero_carta, validade_carta,
     *                 password_hash, saldo, perfil, ativo, data_criacao
     */
    private User mapUser(ResultSet rs) throws SQLException {
        Date      validadeDate = rs.getDate("validade_carta");
        Timestamp dataCriacao  = rs.getTimestamp("data_criacao");
        User user = new User(
            rs.getInt("id"),
            rs.getString("email"),
            rs.getString("nome"),
            rs.getString("nif"),
            rs.getString("numero_carta"),
            validadeDate != null ? validadeDate.toLocalDate()       : null,
            rs.getString("password_hash"),
            rs.getBigDecimal("saldo"),
            rs.getString("perfil"),
            rs.getBoolean("ativo"),
            dataCriacao  != null ? dataCriacao.toLocalDateTime()    : null
        );
        user.setTipo(rs.getString("tipo"));
        return user;
    }
}
