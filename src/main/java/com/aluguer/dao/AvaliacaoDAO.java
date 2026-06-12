package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Avaliacao;
import com.aluguer.util.DatabaseConnection;

public class AvaliacaoDAO {

    // ============================
    // 1. INSERIR AVALIAÇÃO
    // ============================
    public boolean inserir(Avaliacao a) throws SQLException {
        if (jaAvaliou(a.getReservaId(), a.getAvaliadorId())) {
            return false;
        }

        String sql = "INSERT INTO avaliacao (reservaId, avaliadorId, avaliadoId, tipo, nota, comentario) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, a.getReservaId());
            stmt.setInt(2, a.getAvaliadorId());
            stmt.setInt(3, a.getAvaliadoId());
            stmt.setString(4, a.getTipo().name());
            stmt.setInt(5, a.getNota());
            stmt.setString(6, a.getComentario());

            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) a.setId(keys.getInt(1));
                }
                return true;
            }
        }
        return false;
    }

    // ============================
    // 2. JÁ AVALIOU?
    // Um avaliador só pode avaliar uma vez por reserva
    // ============================
    public boolean jaAvaliou(int reservaId, int avaliadorId) throws SQLException {
        String sql = "SELECT 1 FROM avaliacao WHERE reservaId = ? AND avaliadorId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservaId);
            stmt.setInt(2, avaliadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ============================
    // 3. LISTAR AVALIAÇÕES DE UM UTILIZADOR
    // ============================
    public List<Avaliacao> listarPorAvaliado(int avaliadoId) throws SQLException {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM avaliacao WHERE avaliadoId = ? ORDER BY dataCriacao DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, avaliadoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    // ============================
    // 4. CALCULAR MÉDIA DE UM UTILIZADOR
    // Devolve -1 se não tiver avaliações
    // ============================
    public double calcularMedia(int avaliadoId) throws SQLException {
        String sql = "SELECT AVG(nota) AS media FROM avaliacao WHERE avaliadoId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, avaliadoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getObject("media") != null) {
                    return rs.getDouble("media");
                }
            }
        }
        return -1;
    }

    // ============================
    // 5. LISTAR AVALIAÇÕES DE UMA RESERVA
    // ============================
    public List<Avaliacao> listarPorReserva(int reservaId) throws SQLException {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM avaliacao WHERE reservaId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    // ============================
    // MAPEAMENTO ResultSet → Avaliacao
    // ============================
    private Avaliacao mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("dataCriacao");
        return new Avaliacao(
            rs.getInt("id"),
            rs.getInt("reservaId"),
            rs.getInt("avaliadorId"),
            rs.getInt("avaliadoId"),
            Avaliacao.TipoAvaliado.valueOf(rs.getString("tipo")),
            rs.getInt("nota"),
            rs.getString("comentario"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
