package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

public class VeiculoDAO {

    // ============================
    // 1. INSERIR VEÍCULO
    // ============================
    public boolean inserir(Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculo (marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setString(4, v.getCombustivel());
            stmt.setDouble(5, v.getPrecoDiario());
            stmt.setString(6, v.getLocalizacao());
            stmt.setInt(7, v.getProprietarioId());
            stmt.setString(8, v.getEstado());

            return stmt.executeUpdate() > 0;
        }
    }

    // ============================
    // 2. LISTAR TODOS
    // ============================
    public List<Veiculo> listarTodos() throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM veiculo";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    // ============================
    // 3. BUSCAR POR ID
    // ============================
    public Veiculo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM veiculo WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ============================
    // 4. ATUALIZAR
    // ============================
    public boolean atualizar(Veiculo v) throws SQLException {
        String sql = "UPDATE veiculo SET marca=?, modelo=?, ano=?, combustivel=?, precoDiario=?, localizacao=?, proprietarioId=?, estado=? "
                   + "WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setString(4, v.getCombustivel());
            stmt.setDouble(5, v.getPrecoDiario());
            stmt.setString(6, v.getLocalizacao());
            stmt.setInt(7, v.getProprietarioId());
            stmt.setString(8, v.getEstado());
            stmt.setInt(9, v.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    // ============================
    // 5. APAGAR
    // ============================
    public boolean apagar(int id) throws SQLException {
        String sql = "DELETE FROM veiculo WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ============================
    // MAPEAMENTO ResultSet → Veiculo
    // ============================
    private Veiculo mapRow(ResultSet rs) throws SQLException {
        return new Veiculo(
            rs.getInt("id"),
            rs.getString("marca"),
            rs.getString("modelo"),
            rs.getInt("ano"),
            rs.getString("combustivel"),
            rs.getDouble("precoDiario"),
            rs.getString("localizacao"),
            rs.getInt("proprietarioId"),
            rs.getString("estado")
        );
    }
}