package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Avaliacao;
import com.aluguer.util.DatabaseConnection;

public class AvaliacaoDAO {

    /**
     * Insere uma nova avaliação. Devolve false se este utilizador já
     * avaliou esta reserva (evita avaliações duplicadas).
     */
    public boolean inserir(Avaliacao a) throws SQLException {
        if (jaAvaliou(a.getReservaId(), a.getUtilizadorId())) {
            return false;
        }

        String sql = "INSERT INTO avaliacao (reservaId, utilizadorId, veiculoId, classificacao, comentario) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, a.getReservaId());
            stmt.setInt(2, a.getUtilizadorId());
            stmt.setInt(3, a.getVeiculoId());
            stmt.setInt(4, a.getClassificacao());
            stmt.setString(5, a.getComentario());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        a.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /** Verifica se este utilizador já avaliou esta reserva. */
    public boolean jaAvaliou(int reservaId, int utilizadorId) throws SQLException {
        String sql = "SELECT 1 FROM avaliacao WHERE reservaId = ? AND utilizadorId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            stmt.setInt(2, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Todas as avaliações de um veículo específico, mais recentes primeiro. */
    public List<Avaliacao> listarPorVeiculo(int veiculoId) throws SQLException {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM avaliacao WHERE veiculoId = ? ORDER BY dataAvaliacao DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    /** Todas as avaliações recebidas por um proprietário, em qualquer um dos seus veículos. */
    public List<Avaliacao> listarPorProprietario(int proprietarioId) throws SQLException {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = """
                SELECT av.* FROM avaliacao av
                JOIN veiculo v ON av.veiculoId = v.id
                WHERE v.proprietarioId = ?
                ORDER BY av.dataAvaliacao DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    /** Média de classificações de um veículo. Devolve -1 se não houver nenhuma avaliação. */
    public double mediaPorVeiculo(int veiculoId) throws SQLException {
        String sql = "SELECT AVG(classificacao) AS media FROM avaliacao WHERE veiculoId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double media = rs.getDouble("media");
                    return rs.wasNull() ? -1 : media;
                }
            }
        }
        return -1;
    }

    /**
     * Média de classificações de um proprietário, calculada sobre TODAS as
     * avaliações individuais de TODOS os seus veículos (não é a média das
     * médias dos carros — um carro com mais reviews pesa mais).
     * Devolve -1 se não houver nenhuma avaliação.
     */
    public double mediaPorProprietario(int proprietarioId) throws SQLException {
        String sql = """
                SELECT AVG(av.classificacao) AS media
                FROM avaliacao av
                JOIN veiculo v ON av.veiculoId = v.id
                WHERE v.proprietarioId = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double media = rs.getDouble("media");
                    return rs.wasNull() ? -1 : media;
                }
            }
        }
        return -1;
    }

    /** Número total de avaliações de um veículo. */
    public int totalPorVeiculo(int veiculoId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM avaliacao WHERE veiculoId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        }
        return 0;
    }

    /** Número total de avaliações recebidas por um proprietário. */
    public int totalPorProprietario(int proprietarioId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM avaliacao av
                JOIN veiculo v ON av.veiculoId = v.id
                WHERE v.proprietarioId = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        }
        return 0;
    }

    private Avaliacao mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("dataAvaliacao");
        return new Avaliacao(
                rs.getInt("id"),
                rs.getInt("reservaId"),
                rs.getInt("utilizadorId"),
                rs.getInt("veiculoId"),
                rs.getInt("classificacao"),
                rs.getString("comentario"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}