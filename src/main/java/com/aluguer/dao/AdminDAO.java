package com.aluguer.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.User;
import com.aluguer.service.NotificacaoService;
import com.aluguer.util.DatabaseConnection;


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
     * Atualiza o tipo de um utilizador (coluna `tipo`: proprietario/locatario/admin),
     * mantendo o `perfil` sincronizado:
     *   - tipo = "admin"          → perfil = "ADMINISTRADOR"
     *   - tipo = qualquer outro   → perfil = "UTILIZADOR"
     * (o `perfil` é o campo que efetivamente controla as permissões na aplicação).
     *
     * @param userId   id do utilizador a atualizar
     * @param novoTipo "proprietario" | "locatario" | "admin"
     * @return true se uma linha foi atualizada
     */
    public boolean atualizarTipoUtilizador(int userId, String novoTipo) throws SQLException {
        String novoPerfil = "admin".equalsIgnoreCase(novoTipo) ? "ADMINISTRADOR" : "UTILIZADOR";

        String sql = "UPDATE utilizadores SET tipo = ?, perfil = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoTipo);
            ps.setString(2, novoPerfil);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
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


public boolean emitirAviso(int userId, String motivo) throws SQLException {
    String ins = "INSERT INTO avisos_admin (utilizadorId, motivo, data_aviso) VALUES (?, ?, NOW())";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(ins)) {
        ps.setInt(1, userId);
        ps.setString(2, motivo);
        ps.executeUpdate();
    }

    // enviar email de aviso
    try {
        new com.aluguer.dao.UserDAO().findById(userId).ifPresent(u ->
            com.aluguer.util.EmailService.enviarAvisoAdmin(
                u.getEmail(), u.getNome(), motivo, 0)
        );
    } catch (Exception e) {
        System.err.println("[AdminDAO] Falha ao enviar email de aviso: " + e.getMessage());
    }

    // ✅ ADICIONA ISTO — criar notificação no sino do utilizador
    NotificacaoService.getInstance().criarNotificacao(
        userId,
        "AVISO",
        "⚠️ Recebeste um aviso da administração: " + motivo
    );

    int total = contarAvisos(userId);
    if (total >= 3) {
        setAtivo(userId, false);

        // ✅ OPCIONAL — notificar que foi banido
        NotificacaoService.getInstance().criarNotificacao(
            userId,
            "AVISO",
            "🚫 A tua conta foi suspensa após 3 avisos."
        );

        return true;
    }
    return false;
}


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


    public List<Object[]> estatisticasPorMarca() throws SQLException {
        return estatisticasPorMarca(null, null);
    }

    /**
     * Faturamento de uma marca específica ao longo do tempo (por mês).
     * Se inicio/fim forem null, usa todos os registos.
     */
    public List<Object[]> faturamentoPorDataMarca(String marca, LocalDate inicio, LocalDate fim) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        boolean comFiltroData = inicio != null && fim != null;

        String sql = "SELECT DATE_FORMAT(r.dataInicio, '%Y-%m') AS periodo, "
                   + "       COUNT(r.id)                         AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0)      AS receita "
                   + "FROM reserva r "
                   + "JOIN veiculo v ON r.veiculoId = v.id "
                   + "WHERE v.marca = ? "
                   + (comFiltroData ? "AND r.dataInicio BETWEEN ? AND ? " : "")
                   + "GROUP BY periodo "
                   + "ORDER BY periodo ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, marca);
            if (comFiltroData) { ps.setDate(idx++, Date.valueOf(inicio)); ps.setDate(idx++, Date.valueOf(fim)); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getString("periodo"),
                        rs.getInt("total"),
                        rs.getDouble("receita")
                    });
                }
            }
        }
        return lista;
    }

    /**
     * Faturamento de uma região específica ao longo do tempo (por mês).
     * Se inicio/fim forem null, usa todos os registos.
     */
    public List<Object[]> faturamentoPorDataRegiao(String regiao, LocalDate inicio, LocalDate fim) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        boolean comFiltroData = inicio != null && fim != null;

        String sql = "SELECT DATE_FORMAT(r.dataInicio, '%Y-%m') AS periodo, "
                   + "       COUNT(r.id)                         AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0)      AS receita "
                   + "FROM reserva r "
                   + "JOIN veiculo v ON r.veiculoId = v.id "
                   + "WHERE v.localizacao = ? "
                   + (comFiltroData ? "AND r.dataInicio BETWEEN ? AND ? " : "")
                   + "GROUP BY periodo "
                   + "ORDER BY periodo ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, regiao);
            if (comFiltroData) { ps.setDate(idx++, Date.valueOf(inicio)); ps.setDate(idx++, Date.valueOf(fim)); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getString("periodo"),
                        rs.getInt("total"),
                        rs.getDouble("receita")
                    });
                }
            }
        }
        return lista;
    }


    public List<Object[]> estatisticasPorMarca(LocalDate inicio, LocalDate fim) throws SQLException {
        return estatisticasPorMarca(inicio, fim, null);
    }

    public List<Object[]> estatisticasPorMarca(LocalDate inicio, LocalDate fim, String marcaFiltro) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        boolean comFiltroData  = inicio != null && fim != null;
        boolean comFiltroMarca = marcaFiltro != null && !marcaFiltro.isBlank();

        String sql = "SELECT v.marca, "
                   + "       COUNT(r.id)                    AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0) AS receita "
                   + "FROM veiculo v "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id"
                   + (comFiltroData  ? " AND r.dataInicio BETWEEN ? AND ?" : "")
                   + (comFiltroMarca ? " WHERE v.marca = ?" : "")
                   + " GROUP BY v.marca "
                   + "ORDER BY total DESC, v.marca";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (comFiltroData)  { ps.setDate(idx++, Date.valueOf(inicio)); ps.setDate(idx++, Date.valueOf(fim)); }
            if (comFiltroMarca)   ps.setString(idx, marcaFiltro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getString("marca"),
                        rs.getInt("total"),
                        rs.getDouble("receita")
                    });
                }
            }
        }
        return lista;
    }

    public List<Object[]> estatisticasPorRegiao() throws SQLException {
        return estatisticasPorRegiao(null, null);
    }

    /** Retorna a lista de marcas distintas existentes na tabela veiculo. */
    public List<String> listarMarcas() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM veiculo ORDER BY marca";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString(1));
        }
        return lista;
    }

    /** Retorna a lista de regiões/localizações distintas existentes na tabela veiculo. */
    public List<String> listarRegioes() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT localizacao FROM veiculo ORDER BY localizacao";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString(1));
        }
        return lista;
    }

    public List<Object[]> estatisticasPorRegiao(LocalDate inicio, LocalDate fim) throws SQLException {
        return estatisticasPorRegiao(inicio, fim, null);
    }

    public List<Object[]> estatisticasPorRegiao(LocalDate inicio, LocalDate fim, String regiaoFiltro) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        boolean comFiltroData   = inicio != null && fim != null;
        boolean comFiltroRegiao = regiaoFiltro != null && !regiaoFiltro.isBlank();

        String sql = "SELECT v.localizacao               AS regiao, "
                   + "       COUNT(r.id)                    AS total, "
                   + "       COALESCE(SUM(r.precoTotal), 0) AS receita "
                   + "FROM veiculo v "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id"
                   + (comFiltroData   ? " AND r.dataInicio BETWEEN ? AND ?" : "")
                   + (comFiltroRegiao ? " WHERE v.localizacao = ?" : "")
                   + " GROUP BY v.localizacao "
                   + "ORDER BY total DESC, v.localizacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (comFiltroData)   { ps.setDate(idx++, Date.valueOf(inicio)); ps.setDate(idx++, Date.valueOf(fim)); }
            if (comFiltroRegiao)   ps.setString(idx, regiaoFiltro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getString("regiao"),
                        rs.getInt("total"),
                        rs.getDouble("receita")
                    });
                }
            }
        }
        return lista;
    }


    // =========================================================================
    // 5. MÉTRICAS POR VEÍCULO (visão global — usado no separador Estatísticas)
    // =========================================================================

    /**
     * Devolve, para cada veículo da plataforma, o total de reservas
     * (qualquer estado) e a receita gerada (apenas reservas ACEITE ou
     * CONCLUIDO), com marca/modelo e nome do proprietário.
     *
     * Cada linha do Object[]: {id, marca, modelo, proprietario, totalReservas, receitaTotal}
     */
    public List<Object[]> metricasPorVeiculo() throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT v.id, v.marca, v.modelo, u.nome AS proprietario, "
                   + "       COUNT(r.id) AS totalReservas, "
                   + "       COALESCE(SUM(CASE WHEN r.estado IN ('ACEITE', 'CONCLUIDO') "
                   + "                          THEN r.precoTotal ELSE 0 END), 0) AS receitaTotal "
                   + "FROM veiculo v "
                   + "JOIN utilizadores u ON v.proprietarioId = u.id "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id "
                   + "GROUP BY v.id, v.marca, v.modelo, u.nome "
                   + "ORDER BY receitaTotal DESC, v.marca, v.modelo";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getString("proprietario"),
                    rs.getInt("totalReservas"),
                    rs.getDouble("receitaTotal")
                });
            }
        }
        return lista;
    }

    /**
     * Os N veículos com maior receita gerada (apenas ACEITE/CONCLUIDO).
     * Atalho sobre metricasPorVeiculo() — já vem ordenado por receita.
     */
    public List<Object[]> topVeiculosPorReceita(int limite) throws SQLException {
        List<Object[]> todos = metricasPorVeiculo();
        return todos.subList(0, Math.min(limite, todos.size()));
    }

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