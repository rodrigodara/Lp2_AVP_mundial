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
    // 6. LISTAR MARCAS DISTINTAS
    // ============================
    public List<String> listarMarcas() throws SQLException {
        List<String> marcas = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM veiculo ORDER BY marca";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                marcas.add(rs.getString("marca"));
            }
        }
        return marcas;
    }

    // ============================
    // 7. LISTAR LOCALIZAÇÕES DISTINTAS
    // ============================
    public List<String> listarLocalizacoes() throws SQLException {
        List<String> localizacoes = new ArrayList<>();
        String sql = "SELECT DISTINCT localizacao FROM veiculo ORDER BY localizacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                localizacoes.add(rs.getString("localizacao"));
            }
        }
        return localizacoes;
    }

    // ============================
    // 8. LISTAR COM FILTROS COMBINADOS
    // marca, precoMax e localizacao são opcionais (null = ignorar)
    // ============================
    public List<Veiculo> listarComFiltros(String marca, Double precoMax, String localizacao) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM veiculo WHERE 1=1");
        if (marca != null)       sql.append(" AND marca = ?");
        if (precoMax != null)    sql.append(" AND precoDiario <= ?");
        if (localizacao != null) sql.append(" AND localizacao = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (marca != null)       stmt.setString(idx++, marca);
            if (precoMax != null)    stmt.setDouble(idx++, precoMax);
            if (localizacao != null) stmt.setString(idx++, localizacao);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        }
        return lista;
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