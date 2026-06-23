package com.aluguer.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.util.DatabaseConnection;

/**
 * NotificacaoService — Notificações in-app.
 *
 * ACEITE / REJEITADO — lidas diretamente da tabela reserva (via estado_data + notif_lida)
 * AVISO / PROPOSTA   — continuam na tabela notificacoes (não têm linha própria em reserva)
 *
 * O sino agrega os dois tipos. Desaparecem quando:
 *   - notif_lida = 1  (marcado como lido)
 *   - estado_data / data_criacao > 24h
 */
public class NotificacaoService {

    private static NotificacaoService instance;

    private NotificacaoService() {
        criarTabelaSeNecessario();
    }

    public static synchronized NotificacaoService getInstance() {
        if (instance == null) instance = new NotificacaoService();
        return instance;
    }

    // =========================================================================
    // Tabela notificacoes (AVISO + PROPOSTA)
    // =========================================================================

    private void criarTabelaSeNecessario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS notificacoes (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                utilizadorId INT         NOT NULL,
                tipo         VARCHAR(20) NOT NULL,
                mensagem     TEXT        NOT NULL,
                lida         TINYINT(1)  NOT NULL DEFAULT 0,
                data_criacao DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao criar tabela: " + e.getMessage());
        }
    }

    /** Cria notificação manual — usado para AVISO (admin) e PROPOSTA (proprietário).
     *  Se mensagem for null (ex.: chamadas para ACEITE/REJEITADO/CANCELADO, que na
     *  prática já são lidas diretamente da tabela reserva), gera uma mensagem por
     *  defeito a partir do tipo — a coluna `mensagem` é NOT NULL na base de dados. */
    public void criarNotificacao(int utilizadorId, String tipo, String mensagem) {
        String mensagemFinal = (mensagem != null && !mensagem.isBlank())
            ? mensagem
            : mensagemPorDefeito(tipo);

        String sql = "INSERT INTO notificacoes (utilizadorId, tipo, mensagem) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, tipo);
            ps.setString(3, mensagemFinal);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao criar notificação: " + e.getMessage());
        }
    }

    private String mensagemPorDefeito(String tipo) {
        if (tipo == null) return "Tem uma nova notificação.";
        return switch (tipo) {
            case "ACEITE"    -> "A sua reserva foi aceite.";
            case "REJEITADO" -> "A sua reserva foi recusada.";
            case "CANCELADO" -> "A sua reserva foi cancelada.";
            default          -> "Tem uma nova notificação.";
        };
    }

    // =========================================================================
    // Listar para o sino — agrega reserva (ACEITE/REJEITADO) + notificacoes (AVISO/PROPOSTA)
    // =========================================================================

    public List<Notificacao> listar(int utilizadorId) {
        List<Notificacao> lista = new ArrayList<>();

        // 1) ACEITE, REJEITADO e CANCELADO — vêm da tabela reserva
        String sqlReserva = """
            SELECT r.id, r.estado AS tipo, r.estado_data AS data_criacao,
                   CONCAT(v.marca, ' ', v.modelo) AS nomeVeiculo,
                   r.dataInicio, r.dataFim
            FROM reserva r
            JOIN veiculo v ON v.id = r.veiculoId
            WHERE r.utilizadorId = ?
              AND r.estado IN ('ACEITE', 'REJEITADO', 'CANCELADO')
              AND r.notif_lida = 0
              AND r.estado_data >= NOW() - INTERVAL 24 HOUR
            ORDER BY r.estado_data DESC
            LIMIT 50
            """;

        // 2) AVISO e PROPOSTA — vêm da tabela notificacoes
        // (exclui ACEITE/REJEITADO/CANCELADO mesmo que existam aqui também,
        //  para não duplicar o que já vem da tabela reserva acima)
        String sqlNotif = """
            SELECT id, tipo, mensagem, lida, data_criacao
            FROM notificacoes
            WHERE utilizadorId = ?
              AND lida = 0
              AND tipo NOT IN ('ACEITE', 'REJEITADO', 'CANCELADO')
              AND data_criacao >= NOW() - INTERVAL 24 HOUR
            ORDER BY data_criacao DESC
            LIMIT 50
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlReserva)) {
                ps.setInt(1, utilizadorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new Notificacao(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            null,
                            rs.getString("nomeVeiculo"),
                            rs.getString("dataInicio"),
                            rs.getString("dataFim"),
                            rs.getString("data_criacao"),
                            false,
                            true   // isReserva = true
                        ));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlNotif)) {
                ps.setInt(1, utilizadorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new Notificacao(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("mensagem"),
                            null, null, null,
                            rs.getString("data_criacao"),
                            rs.getBoolean("lida"),
                            false  // isReserva = false
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao listar: " + e.getMessage());
        }

        // ordenar por data decrescente
        lista.sort((a, b) -> {
            String da = a.dataCriacao != null ? a.dataCriacao : "";
            String db = b.dataCriacao != null ? b.dataCriacao : "";
            return db.compareTo(da);
        });

        return lista;
    }

    // =========================================================================
    // Contar não lidas (badge) — soma dos dois tipos
    // =========================================================================

    public int contarNaoLidas(int utilizadorId) {
        int total = 0;

        String sqlReserva = """
            SELECT COUNT(*) FROM reserva
            WHERE utilizadorId = ?
              AND estado IN ('ACEITE', 'REJEITADO', 'CANCELADO')
              AND notif_lida = 0
              AND estado_data >= NOW() - INTERVAL 24 HOUR
            """;
        String sqlNotif = """
            SELECT COUNT(*) FROM notificacoes
            WHERE utilizadorId = ? AND lida = 0
              AND tipo NOT IN ('ACEITE', 'REJEITADO', 'CANCELADO')
              AND data_criacao >= NOW() - INTERVAL 24 HOUR
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlReserva)) {
                ps.setInt(1, utilizadorId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total += rs.getInt(1); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlNotif)) {
                ps.setInt(1, utilizadorId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total += rs.getInt(1); }
            }
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao contar: " + e.getMessage());
        }
        return total;
    }

    // =========================================================================
    // Marcar como lida
    // =========================================================================

    /** Marca uma notificação como lida. isReserva indica de qual tabela. */
    public void marcarComoLida(int id, boolean isReserva) {
        String sql = isReserva
            ? "UPDATE reserva SET notif_lida = 1 WHERE id = ?"
            : "UPDATE notificacoes SET lida = 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao marcar como lida: " + e.getMessage());
        }
    }

    public void marcarTodasComoLidas(int utilizadorId) {
        String[] sqls = {
            "UPDATE reserva SET notif_lida = 1 WHERE utilizadorId = ? AND notif_lida = 0",
            "UPDATE notificacoes SET lida = 1 WHERE utilizadorId = ? AND lida = 0"
        };
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String sql : sqls) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, utilizadorId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificacaoService] Erro ao marcar todas como lidas: " + e.getMessage());
        }
    }

    // =========================================================================
    // Modelo de notificação unificado
    // =========================================================================

    public static class Notificacao {
        public final int     id;
        public final String  tipo;
        public final String  mensagem;      // preenchido para AVISO/PROPOSTA
        public final String  nomeVeiculo;   // preenchido para ACEITE/REJEITADO
        public final String  dataInicio;
        public final String  dataFim;
        public final String  dataCriacao;
        public final boolean lida;
        public final boolean isReserva;     // true = veio da tabela reserva

        public Notificacao(int id, String tipo, String mensagem,
                           String nomeVeiculo, String dataInicio, String dataFim,
                           String dataCriacao, boolean lida, boolean isReserva) {
            this.id          = id;
            this.tipo        = tipo;
            this.mensagem    = mensagem;
            this.nomeVeiculo = nomeVeiculo;
            this.dataInicio  = dataInicio;
            this.dataFim     = dataFim;
            this.dataCriacao = dataCriacao;
            this.lida        = lida;
            this.isReserva   = isReserva;
        }

        public String getMensagem() {
            if (mensagem != null) return mensagem;
            return switch (tipo) {
                case "ACEITE"    -> "A tua reserva do " + nomeVeiculo
                                    + " (" + dataInicio + " a " + dataFim + ") foi aceite.";
                case "REJEITADO" -> "A tua reserva do " + nomeVeiculo
                                    + " (" + dataInicio + " a " + dataFim + ") foi recusada.";
                default          -> "Notificação";
            };
        }
    }
}