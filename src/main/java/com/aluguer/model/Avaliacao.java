package com.aluguer.model;

import java.time.LocalDateTime;

/**
 * Avaliação (rating de 1 a 5) deixada por um utilizador a um veículo
 * que alugou, após a reserva ter sido CONCLUÍDA.
 *
 * A média de um proprietário é a agregação de todas as avaliações
 * de todos os veículos dele (ver AvaliacaoDAO.mediaPorProprietario).
 */
public class Avaliacao {

    private int id;
    private int reservaId;
    private int utilizadorId; // quem avalia (o locatário)
    private int veiculoId;    // o veículo avaliado
    private int classificacao; // 1 a 5
    private String comentario;
    private LocalDateTime dataAvaliacao;

    public Avaliacao() {
    }

    /** Construtor usado ao criar uma nova avaliação (antes de inserir na BD). */
    public Avaliacao(int reservaId, int utilizadorId, int veiculoId, int classificacao, String comentario) {
        this.reservaId = reservaId;
        this.utilizadorId = utilizadorId;
        this.veiculoId = veiculoId;
        setClassificacao(classificacao);
        this.comentario = comentario;
    }

    /** Construtor completo, usado ao ler uma linha da base de dados. */
    public Avaliacao(int id, int reservaId, int utilizadorId, int veiculoId,
                      int classificacao, String comentario, LocalDateTime dataAvaliacao) {
        this.id = id;
        this.reservaId = reservaId;
        this.utilizadorId = utilizadorId;
        this.veiculoId = veiculoId;
        this.classificacao = classificacao;
        this.comentario = comentario;
        this.dataAvaliacao = dataAvaliacao;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReservaId() { return reservaId; }
    public void setReservaId(int reservaId) { this.reservaId = reservaId; }

    public int getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(int utilizadorId) { this.utilizadorId = utilizadorId; }

    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }

    public int getClassificacao() { return classificacao; }
    public void setClassificacao(int classificacao) {
        if (classificacao < 1 || classificacao > 5) {
            throw new IllegalArgumentException("Classificação deve ser entre 1 e 5.");
        }
        this.classificacao = classificacao;
    }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
}