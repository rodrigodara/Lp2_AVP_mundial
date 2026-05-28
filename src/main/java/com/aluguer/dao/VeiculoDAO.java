package com.aluguer.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Veiculo;

public class VeiculoDAO {

    private Connection conn;

    public VeiculoDAO(Connection conn) {
        this.conn = conn;
    }

    // ============================
    // 1. INSERIR VEÍCULO
    // ============================
    public boolean inserir(Veiculo v) {
        String sql = "INSERT INTO veiculo (marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setString(4, v.getCombustivel());
            stmt.setDouble(5, v.getPrecoDiario());
            stmt.setString(6, v.getLocalizacao());
            stmt.setInt(7, v.getProprietarioId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================
    // 2. LISTAR TODOS
    // ============================
    public List<Veiculo> listarTodos() {
        List<Veiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM veiculo";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Veiculo v = new Veiculo(
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getInt("ano"),
                    rs.getString("combustivel"),
                    rs.getDouble("precoDiario"),
                    rs.getString("localizacao"),
                    rs.getInt("proprietarioId")
                );
                lista.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    // ============================
    // 3. BUSCAR POR ID
    // ============================
    public Veiculo buscarPorId(int id) {
        String sql = "SELECT * FROM veiculo WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Veiculo(
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getInt("ano"),
                    rs.getString("combustivel"),
                    rs.getDouble("precoDiario"),
                    rs.getString("localizacao"),
                    rs.getInt("proprietarioId")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ============================
    // 4. ATUALIZAR
    // ============================
    public boolean atualizar(Veiculo v) {
        String sql = "UPDATE veiculo SET marca=?, modelo=?, ano=?, combustivel=?, precoDiario=?, localizacao=?, proprietarioId=? "
                   + "WHERE id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setString(4, v.getCombustivel());
            stmt.setDouble(5, v.getPrecoDiario());
            stmt.setString(6, v.getLocalizacao());
            stmt.setInt(7, v.getProprietarioId());
            stmt.setInt(8, v.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================
    // 5. APAGAR
    // ============================
    public boolean apagar(int id) {
        String sql = "DELETE FROM veiculo WHERE id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
