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
                     tipo, saldo, perfil, ativo, data_criacao, security_question, security_answer)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)
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
            ps.setString(11, user.getSecurityQuestion());
            ps.setString(12, user.getSecurityAnswer());

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

    public String getSecurityQuestion(String email) throws SQLException {
        String sql = "SELECT security_question FROM utilizadores WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String q = rs.getString("security_question");
                return (q == null || q.isBlank()) ? "" : q;
            }
        }
    }

    public String getSecurityAnswerHash(String email) throws SQLException {
        String sql = "SELECT security_answer FROM utilizadores WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("security_answer");
            }
        }
        return null;
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

    private User mapRow(ResultSet rs) throws SQLException {
        Date validadeDate = rs.getDate("validade_carta");
        Timestamp dataCriacao = rs.getTimestamp("data_criacao");

        return new User(
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
    }
}