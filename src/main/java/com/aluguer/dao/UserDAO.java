package com.aluguer.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

import com.aluguer.model.User;
import com.aluguer.util.DatabaseConnection;

public class UserDAO {

    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT 1 FROM utilizadores WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean nifExiste(String nif) throws SQLException {
        String sql = "SELECT 1 FROM utilizadores WHERE nif = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nif);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int registar(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            throw new IllegalArgumentException("O email já está registado: " + user.getEmail());
        }
        if (nifExiste(user.getNif())) {
            throw new IllegalArgumentException("O NIF já está registado: " + user.getNif());
        }

        String sql = """
                INSERT INTO utilizadores
                    (email, nome, nif, numero_carta, validade_carta, password_hash,
                     tipo, saldo, perfil, ativo, data_criacao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getNome());
            ps.setString(3, user.getNif());
            ps.setString(4, user.getNumeroCarta());
            ps.setDate(5, Date.valueOf(user.getValidadeCarta()));
            ps.setString(6, user.getPasswordHash());
            ps.setString(7, "locatario");
            ps.setBigDecimal(8, user.getSaldo());
            ps.setString(9, user.getPerfil());
            ps.setBoolean(10, user.isAtivo());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Falha ao obter o id gerado após registo.");
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean updatePassword(String email, String newHashedPassword) throws SQLException {
        String sql = "UPDATE utilizadores SET password_hash = ? WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Atualiza os dados de perfil do utilizador (nome, email, NIF, carta de
     * condução e foto). A foto pode ser null (sem foto).
     */
    public boolean atualizarPerfil(int id, String nome, String email, String nif,
                                    String numeroCarta, java.sql.Date validadeCarta,
                                    byte[] foto) throws SQLException {
        String sql = """
                UPDATE utilizadores
                SET nome = ?, email = ?, nif = ?, numero_carta = ?, validade_carta = ?, foto = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, email);
            ps.setString(3, nif);
            ps.setString(4, numeroCarta);
            ps.setDate(5, validadeCarta);
            ps.setBytes(6, foto);
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Date validadeDate = rs.getDate("validade_carta");
        Timestamp dataCriacao = rs.getTimestamp("data_criacao");

        User user = new User(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("nome"),
                rs.getString("nif"),
                rs.getString("numero_carta"),
                validadeDate != null ? validadeDate.toLocalDate() : null,
                rs.getString("password_hash"),
                rs.getBigDecimal("saldo"),
                rs.getString("perfil"),
                rs.getBoolean("ativo"),
                dataCriacao != null ? dataCriacao.toLocalDateTime() : null
        );
        user.setTipo(rs.getString("tipo"));
        try {
            user.setFoto(rs.getBytes("foto"));
        } catch (SQLException semColunaFoto) {
            // Coluna "foto" ainda não existe na BD — ver migração SQL fornecida.
            user.setFoto(null);
        }
        try {
            user.setSaldoPendente(rs.getBigDecimal("saldo_pendente"));
        } catch (SQLException semColuna) {
            user.setSaldoPendente(java.math.BigDecimal.ZERO);
        }
        return user;
    }

    /**
     * Atualiza o saldo do utilizador na BD.
     * @param utilizadorId id do utilizador
     * @param novoSaldo novo saldo a definir
     * @return true se atualizado com sucesso
     */
    public boolean atualizarSaldo(int utilizadorId, java.math.BigDecimal novoSaldo) {
        String sql = "UPDATE utilizadores SET saldo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, novoSaldo);
            ps.setInt(2, utilizadorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Erro ao atualizar saldo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recalcula e atualiza o saldo_pendente do utilizador.
     * O saldo pendente é o maior valor (precoTotal + caucao) entre todas
     * as reservas PENDENTES do utilizador.
     * Exemplo: reservas pendentes de 200€, 500€ e 100€ → saldo_pendente = 500€
     * @param utilizadorId id do utilizador
     * @param conn conexão BD (para usar na mesma transação)
     */
    public void recalcularSaldoPendente(int utilizadorId, Connection conn) {
        String sql = """
            SELECT COALESCE(MAX(precoTotal + caucao), 0) AS maior_pendente
            FROM reserva
            WHERE utilizadorId = ? AND estado = 'PENDENTE'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal pendente = rs.getBigDecimal("maior_pendente");
                    String upd = "UPDATE utilizadores SET saldo_pendente = ? WHERE id = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(upd)) {
                        psUpd.setBigDecimal(1, pendente);
                        psUpd.setInt(2, utilizadorId);
                        psUpd.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Erro ao recalcular saldo pendente: " + e.getMessage());
        }
    }}