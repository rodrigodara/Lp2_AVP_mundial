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

/**
 * ALV-27 / ALV-28 — DAO para operações de utilizador na base de dados.
 *
 * Responsabilidades:
 *   - Registar novo utilizador (insert)
 *   - Verificar se email já existe (ALV-28)
 *   - Buscar utilizador por email (para login)
 *   - Buscar utilizador por id
 */
public class UserDAO {

    // -----------------------------------------------------------------
    // ALV-28: Validar email único
    // -----------------------------------------------------------------

    /**
     * Verifica se já existe um utilizador com o email dado.
     *
     * @param email email a verificar
     * @return true se o email já estiver registado
     */
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

    // -----------------------------------------------------------------
    // ALV-27: Endpoint de registo — inserir novo utilizador
    // -----------------------------------------------------------------

    /**
     * Insere um novo utilizador na base de dados.
     * A password já deve vir em hash BCrypt (gerada em ALV-26).
     *
     * @param user utilizador a registar
     * @return id gerado pelo MySQL
     * @throws IllegalArgumentException se o email já existir
     */
    public int registar(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            throw new IllegalArgumentException("O email já está registado: " + user.getEmail());
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

    // -----------------------------------------------------------------
    // Buscar por email — usado no login (ALV-30/31)
    // -----------------------------------------------------------------

    /**
     * Devolve o utilizador com o email dado, se existir.
     *
     * @param email email de login
     * @return Optional com o utilizador ou vazio se não encontrado
     */
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

    /**
     * Devolve o utilizador com o id dado, se existir.
     */
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

    // -----------------------------------------------------------------
    // Mapeamento ResultSet → User
    // -----------------------------------------------------------------

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
