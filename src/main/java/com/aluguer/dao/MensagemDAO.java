package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Mensagem;
import com.aluguer.util.DatabaseConnection;

/**
 * DAO para as mensagens de chat entre locatário e proprietário, associadas
 * a uma reserva. Cria a tabela automaticamente se não existir, seguindo o
 * mesmo padrão usado em NotificacaoService.
 */
public class MensagemDAO {

    public MensagemDAO() {
        criarTabelaSeNecessario();
    }

    private void criarTabelaSeNecessario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS mensagens (
                id             INT AUTO_INCREMENT PRIMARY KEY,
                reservaId      INT          NOT NULL,
                remetenteId    INT          NOT NULL,
                destinatarioId INT          NOT NULL,
                conteudo       TEXT         NOT NULL,
                dataEnvio      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                lida           TINYINT(1)   NOT NULL DEFAULT 0,
                INDEX idx_mensagens_reserva (reservaId)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MensagemDAO] Erro ao criar tabela: " + e.getMessage());
        }
    }

    /** Insere uma nova mensagem. Preenche o id gerado no próprio objeto. */
    public boolean inserir(Mensagem m) throws SQLException {
        String sql = "INSERT INTO mensagens (reservaId, remetenteId, destinatarioId, conteudo) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, m.getReservaId());
            stmt.setInt(2, m.getRemetenteId());
            stmt.setInt(3, m.getDestinatarioId());
            stmt.setString(4, m.getConteudo());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        m.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /** Todas as mensagens de uma reserva, ordenadas da mais antiga para a mais recente. */
    public List<Mensagem> listarPorReserva(int reservaId) throws SQLException {
        List<Mensagem> lista = new ArrayList<>();
        String sql = "SELECT * FROM mensagens WHERE reservaId = ? ORDER BY dataEnvio ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    /** Número de mensagens não lidas numa reserva, destinadas a este utilizador. */
    public int contarNaoLidas(int reservaId, int destinatarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mensagens WHERE reservaId = ? AND destinatarioId = ? AND lida = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            stmt.setInt(2, destinatarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Número total de mensagens não lidas em todas as reservas, para um utilizador (ex.: badge). */
    public int contarNaoLidasTotal(int destinatarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mensagens WHERE destinatarioId = ? AND lida = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, destinatarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Marca como lidas todas as mensagens de uma reserva destinadas a este utilizador. */
    public boolean marcarComoLidas(int reservaId, int destinatarioId) throws SQLException {
        String sql = "UPDATE mensagens SET lida = 1 WHERE reservaId = ? AND destinatarioId = ? AND lida = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            stmt.setInt(2, destinatarioId);
            stmt.executeUpdate();
            return true;
        }
    }

    private Mensagem mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("dataEnvio");
        return new Mensagem(
                rs.getInt("id"),
                rs.getInt("reservaId"),
                rs.getInt("remetenteId"),
                rs.getInt("destinatarioId"),
                rs.getString("conteudo"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getBoolean("lida")
        );
    }
}