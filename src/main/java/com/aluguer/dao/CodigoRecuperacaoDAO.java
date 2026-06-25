package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.aluguer.util.DatabaseConnection;

/**
 * DAO para os códigos de recuperação de password enviados por email.
 * Cria a tabela automaticamente se não existir, seguindo o mesmo padrão
 * usado em MensagemDAO / NotificacaoService.
 */
public class CodigoRecuperacaoDAO {

    public CodigoRecuperacaoDAO() {
        criarTabelaSeNecessario();
    }

    private void criarTabelaSeNecessario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS codigos_recuperacao (
                id             INT AUTO_INCREMENT PRIMARY KEY,
                utilizadorId   INT          NOT NULL,
                codigo         VARCHAR(6)   NOT NULL,
                dataCriacao    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                dataExpiracao  DATETIME     NOT NULL,
                usado          TINYINT(1)   NOT NULL DEFAULT 0,
                INDEX idx_codigos_utilizador (utilizadorId)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CodigoRecuperacaoDAO] Erro ao criar tabela: " + e.getMessage());
        }
    }

    /** Insere um novo código, válido por 15 minutos a partir de agora. */
    public boolean inserir(int utilizadorId, String codigo) throws SQLException {
        String sql = "INSERT INTO codigos_recuperacao (utilizadorId, codigo, dataExpiracao) "
                   + "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, codigo);
            return ps.executeUpdate() > 0;
        }
    }

    /** Invalida (marca como usados) todos os códigos anteriores ainda não usados deste utilizador. */
    public void invalidarAnteriores(int utilizadorId) throws SQLException {
        String sql = "UPDATE codigos_recuperacao SET usado = 1 WHERE utilizadorId = ? AND usado = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.executeUpdate();
        }
    }

    /**
     * Verifica se o código é válido para este utilizador: existe, não expirou,
     * não foi usado, e corresponde exatamente. Não o marca como usado (isso é
     * feito separadamente em marcarComoUsado, só depois da password mudar).
     */
    public boolean isValido(int utilizadorId, String codigo) throws SQLException {
        String sql = "SELECT id FROM codigos_recuperacao "
                   + "WHERE utilizadorId = ? AND codigo = ? AND usado = 0 AND dataExpiracao >= NOW() "
                   + "ORDER BY dataCriacao DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Marca o código (mais recente válido) como usado, para não poder ser reutilizado. */
    public void marcarComoUsado(int utilizadorId, String codigo) throws SQLException {
        String sql = "UPDATE codigos_recuperacao SET usado = 1 "
                   + "WHERE utilizadorId = ? AND codigo = ? AND usado = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, codigo);
            ps.executeUpdate();
        }
    }
}