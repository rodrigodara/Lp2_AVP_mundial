package com.aluguer.service;

import java.sql.SQLException;
import java.util.List;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;

public class VeiculoService {

    private final VeiculoDAO dao = new VeiculoDAO();

    public List<Veiculo> getAllVehicles() throws Exception {
        return dao.listarTodos();
    }

    public boolean inserir(Veiculo v) throws SQLException {
        return dao.inserir(v);
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
     * Mantido para compatibilidade com chamadas existentes (ex:
     * ProcurarVeiculosView). Equivale a chamar a versão completa com os
     * restantes filtros a null.
     */
    public List<Veiculo> getVehiclesComFiltros(String marca, Double precoMax, String localizacao) throws Exception {
        return dao.listarComFiltros(marca, null, null, precoMax, localizacao, null, null, null, null, null, null);
    }

    /**
     * Pesquisa completa de veículos com todos os filtros disponíveis. Qualquer
     * parâmetro a null é ignorado na pesquisa.
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
}
