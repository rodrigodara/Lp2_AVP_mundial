package com.aluguer.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;
import com.aluguer.model.VeiculoMetricas;
import com.aluguer.util.DatabaseConnection;

public class VeiculoService {

    private final VeiculoDAO dao = new VeiculoDAO();

    public List<Veiculo> getAllVehicles() throws Exception {
        return dao.listarTodos();
    }

    public boolean inserir(Veiculo v) throws SQLException {
        return dao.inserir(v);
    }

    public boolean atualizar(Veiculo v) throws SQLException {
        return dao.atualizar(v);
    }

    // ALV-176 — endpoint GET myVehicles
    public List<Veiculo> listarPorProprietario(int proprietarioId) throws SQLException {
        return dao.listarPorProprietario(proprietarioId);
    }

    // ALV-179 — ação remover veículo
    public boolean remover(int veiculoId) throws SQLException {
        return dao.apagar(veiculoId);
    }

    public List<String> getMarcas() throws Exception {
        return dao.listarMarcas();
    }

    public List<String> getLocalizacoes() throws Exception {
        return dao.listarLocalizacoes();
    }

    public List<String> getTiposVeiculo() throws Exception {
        return dao.listarTiposVeiculo();
    }

    public List<String> getCombustiveis() throws Exception {
        return dao.listarCombustiveis();
    }

    public List<String> getTransmissoes() throws Exception {
        return dao.listarTransmissoes();
    }

    public List<String> getModelosPorMarca(String marca) throws Exception {
        return dao.listarModelosPorMarca(marca);
    }

    /**
     * Mantido para compatibilidade com chamadas existentes (ex: ProcurarVeiculosView).
     * Equivale a chamar a versão completa com os restantes filtros a null.
     */
    public List<Veiculo> getVehiclesComFiltros(String marca, Double precoMax, String localizacao) throws Exception {
        return dao.listarComFiltros(marca, null, null, precoMax, localizacao, null, null, null, null, null, null);
    }

    /**
     * Pesquisa completa de veículos com todos os filtros disponíveis.
     * Qualquer parâmetro a null é ignorado na pesquisa.
     */
    public List<Veiculo> getVehiclesComFiltros(String marca, String modelo, Double precoMin, Double precoMax,
            String localizacao, String tipoVeiculo, String combustivel,
            Integer lugares, String transmissao, Double avaliacaoMinima, Double avaliacaoMaxima) throws Exception {
        return dao.listarComFiltros(marca, modelo, precoMin, precoMax, localizacao,
                tipoVeiculo, combustivel, lugares, transmissao, avaliacaoMinima, avaliacaoMaxima);
    }

    public List<Veiculo> pesquisar(String termo) throws Exception {
        return dao.pesquisar(termo);
    }

    // -------------------------------------------------------------------
    // Métricas — reservas e receita por veículo de um proprietário
    // -------------------------------------------------------------------

    /**
     * Devolve, para cada veículo do proprietário indicado, o total de
     * reservas (qualquer estado) e a receita total gerada (apenas
     * reservas ACEITE ou CONCLUIDO — mesma regra usada no AdminDAO).
     *
     * Inclui veículos sem nenhuma reserva (LEFT JOIN), com 0 reservas
     * e 0€ de receita, para que a tabela "Os Meus Veículos" mostre
     * sempre uma linha por veículo.
     */
    public List<VeiculoMetricas> obterMetricasPorProprietario(int proprietarioId) throws SQLException {
        List<VeiculoMetricas> lista = new ArrayList<>();
        String sql = "SELECT v.id, v.marca, v.modelo, "
                   + "       COUNT(r.id) AS totalReservas, "
                   + "       COALESCE(SUM(CASE WHEN r.estado IN ('ACEITE', 'CONCLUIDO') "
                   + "                          THEN r.precoTotal ELSE 0 END), 0) AS receitaTotal "
                   + "FROM veiculo v "
                   + "LEFT JOIN reserva r ON r.veiculoId = v.id "
                   + "WHERE v.proprietarioId = ? "
                   + "GROUP BY v.id, v.marca, v.modelo "
                   + "ORDER BY v.marca, v.modelo";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proprietarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new VeiculoMetricas(
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("totalReservas"),
                        rs.getDouble("receitaTotal")
                    ));
                }
            }
        }
        return lista;
    }
}