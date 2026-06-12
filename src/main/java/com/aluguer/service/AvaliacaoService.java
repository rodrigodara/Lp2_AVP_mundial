package com.aluguer.service;

import java.util.List;

import com.aluguer.dao.AvaliacaoDAO;
import com.aluguer.model.Avaliacao;

public class AvaliacaoService {

    private final AvaliacaoDAO dao = new AvaliacaoDAO();

    /**
     * Submete uma avaliação.
     * Retorna false se o utilizador já avaliou esta reserva.
     */
    public boolean avaliar(int reservaId, int avaliadorId, int avaliadoId,
                           Avaliacao.TipoAvaliado tipo, int nota, String comentario) throws Exception {
        if (nota < 1 || nota > 5) throw new IllegalArgumentException("A nota deve ser entre 1 e 5.");
        Avaliacao a = new Avaliacao(reservaId, avaliadorId, avaliadoId, tipo, nota, comentario);
        return dao.inserir(a);
    }

    /** Verifica se o avaliador já avaliou esta reserva */
    public boolean jaAvaliou(int reservaId, int avaliadorId) throws Exception {
        return dao.jaAvaliou(reservaId, avaliadorId);
    }

    /** Devolve todas as avaliações recebidas por um utilizador */
    public List<Avaliacao> getAvaliacoes(int avaliadoId) throws Exception {
        return dao.listarPorAvaliado(avaliadoId);
    }

    /**
     * Devolve a média das avaliações de um utilizador.
     * Retorna -1 se ainda não tiver avaliações.
     */
    public double getMedia(int avaliadoId) throws Exception {
        return dao.calcularMedia(avaliadoId);
    }
}
