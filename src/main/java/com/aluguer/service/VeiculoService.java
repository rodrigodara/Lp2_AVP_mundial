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

    // ALV-176 — endpoint GET myVehicles
    public List<Veiculo> listarPorProprietario(int proprietarioId) throws SQLException {
        return dao.listarPorProprietario(proprietarioId);
    }

    // ALV-179 — ação remover veículo
    public boolean remover(int veiculoId) throws SQLException {
        return dao.apagar(veiculoId);
    }
}
