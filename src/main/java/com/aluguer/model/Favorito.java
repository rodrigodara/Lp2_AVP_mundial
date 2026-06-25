package com.aluguer.model;

import java.sql.Timestamp;

/**
 * Representa um veículo marcado como favorito por um utilizador.
 * Tabela N-N entre `utilizadores` e `veiculo`.
 */
public class Favorito {

    private int id;
    private int userId;
    private int veiculoId;
    private Timestamp dataCriacao;

    public Favorito(int id, int userId, int veiculoId, Timestamp dataCriacao) {
        this.id = id;
        this.userId = userId;
        this.veiculoId = veiculoId;
        this.dataCriacao = dataCriacao;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getVeiculoId() { return veiculoId; }
    public Timestamp getDataCriacao() { return dataCriacao; }
}