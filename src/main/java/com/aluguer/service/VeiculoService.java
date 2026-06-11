package com.aluguer.service;

import java.util.List;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;

public class VeiculoService {

    private final VeiculoDAO dao = new VeiculoDAO();

    public List<Veiculo> getAllVehicles() throws Exception {
        return dao.listarTodos();
    }

    public List<String> getMarcas() throws Exception {
        return dao.listarMarcas();
    }

    public List<String> getLocalizacoes() throws Exception {
        return dao.listarLocalizacoes();
    }

    public List<Veiculo> getVehiclesComFiltros(String marca, Double precoMax, String localizacao) throws Exception {
        return dao.listarComFiltros(marca, precoMax, localizacao);
    }
}