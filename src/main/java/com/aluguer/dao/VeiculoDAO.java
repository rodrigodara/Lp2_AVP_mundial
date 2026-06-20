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

    public boolean inserir(Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculo "
                   + "(marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado, matricula, tipoVeiculo, lugares, transmissao, consumo, quilometragem, foto1, foto2, foto3, foto4) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setString(9, v.getMatricula());
            stmt.setString(10, v.getTipoVeiculo());
            stmt.setInt(11, v.getLugares());
            stmt.setString(12, v.getTransmissao());
            stmt.setDouble(13, v.getConsumo());
            stmt.setInt(14, v.getQuilometragem());
            stmt.setBytes(15, v.getFoto1());
            stmt.setBytes(16, v.getFoto2());
            stmt.setBytes(17, v.getFoto3());
            stmt.setBytes(18, v.getFoto4());
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Veiculo> listarTodos() throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM veiculo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        }
        return lista;
    }

    /** Busca um veículo por ID, incluindo as fotos (usado no ecrã de detalhe). */
    public Veiculo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM veiculo WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowComFotos(rs);
            }
        }
        return null;
    }

    public boolean atualizar(Veiculo v) throws SQLException {
        String sql = "UPDATE veiculo SET marca=?, modelo=?, ano=?, combustivel=?, precoDiario=?, localizacao=?, "
                   + "proprietarioId=?, estado=?, matricula=?, tipoVeiculo=?, lugares=?, transmissao=?, consumo=?, quilometragem=?, "
                   + "foto1=?, foto2=?, foto3=?, foto4=? WHERE id=?";
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
            stmt.setString(9, v.getMatricula());
            stmt.setString(10, v.getTipoVeiculo());
            stmt.setInt(11, v.getLugares());
            stmt.setString(12, v.getTransmissao());
            stmt.setDouble(13, v.getConsumo());
            stmt.setInt(14, v.getQuilometragem());
            stmt.setBytes(15, v.getFoto1());
            stmt.setBytes(16, v.getFoto2());
            stmt.setBytes(17, v.getFoto3());
            stmt.setBytes(18, v.getFoto4());
            stmt.setInt(19, v.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean apagar(int id) throws SQLException {
        String sql = "DELETE FROM veiculo WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Veiculo> listarPorProprietario(int proprietarioId) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM veiculo WHERE proprietarioId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proprietarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    public List<String> listarMarcas() throws SQLException {
        List<String> marcas = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM veiculo ORDER BY marca";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) marcas.add(rs.getString("marca"));
        }
        return marcas;
    }

    public List<String> listarLocalizacoes() throws SQLException {
        List<String> localizacoes = new ArrayList<>();
        String sql = "SELECT DISTINCT localizacao FROM veiculo ORDER BY localizacao";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) localizacoes.add(rs.getString("localizacao"));
        }
        return localizacoes;
    }

    public List<String> listarTiposVeiculo() throws SQLException {
        List<String> tipos = new ArrayList<>();
        String sql = "SELECT DISTINCT tipoVeiculo FROM veiculo WHERE tipoVeiculo IS NOT NULL ORDER BY tipoVeiculo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) tipos.add(rs.getString("tipoVeiculo"));
        }
        return tipos;
    }

    public List<String> listarCombustiveis() throws SQLException {
        List<String> combustiveis = new ArrayList<>();
        String sql = "SELECT DISTINCT combustivel FROM veiculo ORDER BY combustivel";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) combustiveis.add(rs.getString("combustivel"));
        }
        return combustiveis;
    }

    public List<String> listarTransmissoes() throws SQLException {
        List<String> transmissoes = new ArrayList<>();
        String sql = "SELECT DISTINCT transmissao FROM veiculo WHERE transmissao IS NOT NULL ORDER BY transmissao";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) transmissoes.add(rs.getString("transmissao"));
        }
        return transmissoes;
    }

    /** Lista os modelos distintos de uma marca específica, para preencher o ComboBox de modelo dependente. */
    public List<String> listarModelosPorMarca(String marca) throws SQLException {
        List<String> modelos = new ArrayList<>();
        String sql = "SELECT DISTINCT modelo FROM veiculo WHERE marca = ? ORDER BY modelo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, marca);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) modelos.add(rs.getString("modelo"));
            }
        }
        return modelos;
    }

    /**
     * Pesquisa de veículos com todos os filtros opcionais. Qualquer parâmetro
     * a null (ou -1 nos inteiros) é ignorado na pesquisa.
     *
     * O intervalo de avaliação é aplicado com um LEFT JOIN + HAVING sobre a
     * média de classificações em `avaliacao`, para incluir também veículos
     * sem nenhuma avaliação quando nem avaliacaoMin nem avaliacaoMax são indicados.
     *
     * @param marca           filtro exato de marca, ou null
     * @param modelo          filtro exato de modelo, ou null
     * @param precoMin        preço diário mínimo, ou null
     * @param precoMax        preço diário máximo, ou null
     * @param localizacao     filtro exato de localização, ou null
     * @param tipoVeiculo     filtro exato de tipo de veículo, ou null
     * @param combustivel     filtro exato de combustível, ou null
     * @param lugares         número exato de lugares, ou null
     * @param transmissao     filtro exato de transmissão, ou null
     * @param avaliacaoMinima nota média mínima (1-5), ou null para não filtrar
     * @param avaliacaoMaxima nota média máxima (1-5), ou null para não filtrar
     */
    public List<Veiculo> listarComFiltros(String marca, String modelo, Double precoMin, Double precoMax,
            String localizacao, String tipoVeiculo, String combustivel,
            Integer lugares, String transmissao, Double avaliacaoMinima, Double avaliacaoMaxima) throws SQLException {

        List<Veiculo> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT v.*, AVG(a.classificacao) AS avaliacaoMedia, COUNT(a.id) AS totalAvaliacoes "
              + "FROM veiculo v LEFT JOIN avaliacao a ON a.veiculoId = v.id WHERE 1=1");

        if (marca != null)        sql.append(" AND v.marca = ?");
        if (modelo != null)       sql.append(" AND v.modelo = ?");
        if (precoMin != null)     sql.append(" AND v.precoDiario >= ?");
        if (precoMax != null)     sql.append(" AND v.precoDiario <= ?");
        if (localizacao != null)  sql.append(" AND v.localizacao = ?");
        if (tipoVeiculo != null)  sql.append(" AND v.tipoVeiculo = ?");
        if (combustivel != null)  sql.append(" AND v.combustivel = ?");
        if (lugares != null)      sql.append(" AND v.lugares = ?");
        if (transmissao != null)  sql.append(" AND v.transmissao = ?");

        sql.append(" GROUP BY v.id");

        if (avaliacaoMinima != null || avaliacaoMaxima != null) {
            sql.append(" HAVING 1=1");
            if (avaliacaoMinima != null) sql.append(" AND AVG(a.classificacao) >= ?");
            if (avaliacaoMaxima != null) sql.append(" AND AVG(a.classificacao) <= ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (marca != null)        stmt.setString(idx++, marca);
            if (modelo != null)       stmt.setString(idx++, modelo);
            if (precoMin != null)     stmt.setDouble(idx++, precoMin);
            if (precoMax != null)     stmt.setDouble(idx++, precoMax);
            if (localizacao != null)  stmt.setString(idx++, localizacao);
            if (tipoVeiculo != null)  stmt.setString(idx++, tipoVeiculo);
            if (combustivel != null)  stmt.setString(idx++, combustivel);
            if (lugares != null)      stmt.setInt(idx++, lugares);
            if (transmissao != null)  stmt.setString(idx++, transmissao);
            if (avaliacaoMinima != null) stmt.setDouble(idx++, avaliacaoMinima);
            if (avaliacaoMaxima != null) stmt.setDouble(idx++, avaliacaoMaxima);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Veiculo veic = mapRow(rs);
                    double media = rs.getDouble("avaliacaoMedia");
                    veic.setAvaliacaoMedia(rs.wasNull() ? -1 : media);
                    veic.setTotalAvaliacoes(rs.getInt("totalAvaliacoes"));
                    lista.add(veic);
                }
            }
        }
        return lista;
    }

    public List<Veiculo> pesquisar(String termo) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        String like = "%" + termo.trim() + "%";
        String sql = "SELECT * FROM veiculo WHERE marca LIKE ? OR modelo LIKE ? OR localizacao LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, like);
            stmt.setString(2, like);
            stmt.setString(3, like);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    public boolean atualizarKm(int veiculoId, int novoKm) {
        String sql = "UPDATE veiculo SET quilometragem = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, novoKm);
            stmt.setInt(2, veiculoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[VeiculoDAO] Erro ao atualizar quilometragem: " + e.getMessage());
            return false;
        }
    }

    public int buscarKmAtual(int veiculoId) {
        String sql = "SELECT quilometragem FROM veiculo WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, veiculoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("quilometragem");
            }
        } catch (SQLException e) {
            System.err.println("[VeiculoDAO] Erro ao buscar quilometragem: " + e.getMessage());
        }
        return 0;
    }

    /** Mapeia uma linha sem carregar as fotos — usado em listagens (mais leve). */
    private Veiculo mapRow(ResultSet rs) throws SQLException {
        Veiculo v = new Veiculo(
            rs.getInt("id"),
            rs.getString("marca"),
            rs.getString("modelo"),
            rs.getInt("ano"),
            rs.getString("combustivel"),
            rs.getDouble("precoDiario"),
            rs.getString("localizacao"),
            rs.getInt("proprietarioId"),
            rs.getString("estado"),
            rs.getString("matricula"),
            rs.getString("tipoVeiculo"),
            rs.getInt("lugares"),
            rs.getString("transmissao"),
            rs.getDouble("consumo")
        );
        v.setQuilometragem(rs.getInt("quilometragem"));
        return v;
    }

    /** Mapeia uma linha incluindo as 4 fotos — usado quando se vai mostrar o detalhe de 1 veículo. */
    private Veiculo mapRowComFotos(ResultSet rs) throws SQLException {
        Veiculo v = mapRow(rs);
        v.setFoto1(rs.getBytes("foto1"));
        v.setFoto2(rs.getBytes("foto2"));
        v.setFoto3(rs.getBytes("foto3"));
        v.setFoto4(rs.getBytes("foto4"));
        return v;
    }
}