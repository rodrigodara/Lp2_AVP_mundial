package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.util.DatabaseConnection;

public class FavoritoDAO {

    /** Marca um veículo como favorito. Não faz nada (devolve false) se já existir. */
    public boolean adicionar(int userId, int veiculoId) throws SQLException {
        if (isFavorito(userId, veiculoId)) return false;

        String sql = "INSERT INTO favorito (user_id, veiculo_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, veiculoId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Remove um veículo dos favoritos do utilizador. */
    public boolean remover(int userId, int veiculoId) throws SQLException {
        String sql = "DELETE FROM favorito WHERE user_id = ? AND veiculo_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, veiculoId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Verifica se o veículo já está marcado como favorito por este utilizador. */
    public boolean isFavorito(int userId, int veiculoId) throws SQLException {
        String sql = "SELECT 1 FROM favorito WHERE user_id = ? AND veiculo_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Devolve os IDs de todos os veículos favoritos de um utilizador (mais recentes primeiro). */
    public List<Integer> listarVeiculoIdsFavoritos(int userId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT veiculo_id FROM favorito WHERE user_id = ? ORDER BY data_criacao DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("veiculo_id"));
            }
        }
        return ids;
    }

    /** Quantos favoritos diferentes este veículo tem (útil para estatísticas/relevância). */
    public int contarFavoritosDoVeiculo(int veiculoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorito WHERE veiculo_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}