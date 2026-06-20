package com.aluguer.service;

import java.util.List;

import com.aluguer.dao.AvaliacaoDAO;
import com.aluguer.model.Avaliacao;

public class AvaliacaoService {

    private final AvaliacaoDAO dao = new AvaliacaoDAO();

    /**
     * Submete uma avaliação a um veículo.
     * Retorna false se o utilizador já avaliou esta reserva.
     */
    public boolean avaliar(int reservaId, int utilizadorId, int veiculoId,
                           int classificacao, String comentario) throws Exception {
        if (classificacao < 1 || classificacao > 5) {
            throw new IllegalArgumentException("A classificação deve ser entre 1 e 5.");
        }
        Avaliacao a = new Avaliacao(reservaId, utilizadorId, veiculoId, classificacao, comentario);
        return dao.inserir(a);
    }

    /** Verifica se o utilizador já avaliou esta reserva. */
    public boolean jaAvaliou(int reservaId, int utilizadorId) throws Exception {
        return dao.jaAvaliou(reservaId, utilizadorId);
    }

    /** Todas as avaliações de um veículo específico. */
    public List<Avaliacao> getAvaliacoesVeiculo(int veiculoId) throws Exception {
        return dao.listarPorVeiculo(veiculoId);
    }

    /** Todas as avaliações recebidas por um proprietário (em todos os seus veículos). */
    public List<Avaliacao> getAvaliacoesProprietario(int proprietarioId) throws Exception {
        return dao.listarPorProprietario(proprietarioId);
    }

    /** Média de classificações de um veículo. Retorna -1 se ainda não tiver avaliações. */
    public double getMediaVeiculo(int veiculoId) throws Exception {
        return dao.mediaPorVeiculo(veiculoId);
    }

    /**
     * Média de classificações de um proprietário, agregando todas as
     * avaliações de todos os seus veículos. Retorna -1 se ainda não
     * tiver nenhuma avaliação.
     */
    public double getMediaProprietario(int proprietarioId) throws Exception {
        return dao.mediaPorProprietario(proprietarioId);
    }

    public int getTotalAvaliacoesVeiculo(int veiculoId) throws Exception {
        return dao.totalPorVeiculo(veiculoId);
    }

    public int getTotalAvaliacoesProprietario(int proprietarioId) throws Exception {
        return dao.totalPorProprietario(proprietarioId);
    }
}