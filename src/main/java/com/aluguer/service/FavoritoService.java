package com.aluguer.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.dao.FavoritoDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;

public class FavoritoService {

    private final FavoritoDAO favoritoDAO = new FavoritoDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    /** Alterna o estado de favorito: adiciona se não existir, remove se já existir. Devolve o novo estado. */
    public boolean alternar(int userId, int veiculoId) throws SQLException {
        if (favoritoDAO.isFavorito(userId, veiculoId)) {
            favoritoDAO.remover(userId, veiculoId);
            return false;
        }
        favoritoDAO.adicionar(userId, veiculoId);
        return true;
    }

    public boolean isFavorito(int userId, int veiculoId) throws SQLException {
        return favoritoDAO.isFavorito(userId, veiculoId);
    }

    /**
     * Devolve a lista completa de veículos favoritos do utilizador, já com fotos
     * (reaproveita VeiculoDAO.buscarPorId, tal como o detalhe de veículo).
     * Se um veículo favorito tiver sido apagado entretanto, é ignorado silenciosamente.
     */
    public List<Veiculo> listarVeiculosFavoritos(int userId) throws SQLException {
        List<Integer> ids = favoritoDAO.listarVeiculoIdsFavoritos(userId);
        List<Veiculo> veiculos = new ArrayList<>();
        for (int veiculoId : ids) {
            Veiculo v = veiculoDAO.buscarPorId(veiculoId);
            if (v != null) veiculos.add(v);
        }
        return veiculos;
    }
}