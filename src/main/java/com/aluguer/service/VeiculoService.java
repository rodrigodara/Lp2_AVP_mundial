package com.aluguer.service;

import java.util.List;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;

public class VeiculoService {

    private final VeiculoDAO dao = new VeiculoDAO();

    public List<Veiculo> getAllVehicles() throws Exception {
        return dao.listarTodos();
    }
}