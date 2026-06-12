package com.aluguer.model;

import java.time.LocalDateTime;

public class Avaliacao {

    public enum TipoAvaliado { PROPRIETARIO, LOCATARIO }

    private int id;
    private int reservaId;
    private int avaliadorId;
    private int avaliadoId;
    private TipoAvaliado tipo;
    private int nota; // 1 a 5
    private String comentario;
    private LocalDateTime dataCriacao;

    public Avaliacao() {}

    /** Construtor para inserção */
    public Avaliacao(int reservaId, int avaliadorId, int avaliadoId,
                     TipoAvaliado tipo, int nota, String comentario) {
        this.reservaId   = reservaId;
        this.avaliadorId = avaliadorId;
        this.avaliadoId  = avaliadoId;
        this.tipo        = tipo;
        this.nota        = nota;
        this.comentario  = comentario;
    }

    /** Construtor completo — carregado da BD */
    public Avaliacao(int id, int reservaId, int avaliadorId, int avaliadoId,
                     TipoAvaliado tipo, int nota, String comentario, LocalDateTime dataCriacao) {
        this.id          = id;
        this.reservaId   = reservaId;
        this.avaliadorId = avaliadorId;
        this.avaliadoId  = avaliadoId;
        this.tipo        = tipo;
        this.nota        = nota;
        this.comentario  = comentario;
        this.dataCriacao = dataCriacao;
    }

    // Getters e Setters
    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }

    public int getReservaId()                       { return reservaId; }
    public void setReservaId(int reservaId)         { this.reservaId = reservaId; }

    public int getAvaliadorId()                     { return avaliadorId; }
    public void setAvaliadorId(int avaliadorId)     { this.avaliadorId = avaliadorId; }

    public int getAvaliadoId()                      { return avaliadoId; }
    public void setAvaliadoId(int avaliadoId)       { this.avaliadoId = avaliadoId; }

    public TipoAvaliado getTipo()                   { return tipo; }
    public void setTipo(TipoAvaliado tipo)           { this.tipo = tipo; }

    public int getNota()                            { return nota; }
    public void setNota(int nota)                   { this.nota = nota; }

    public String getComentario()                   { return comentario; }
    public void setComentario(String comentario)    { this.comentario = comentario; }

    public LocalDateTime getDataCriacao()                       { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao)       { this.dataCriacao = dataCriacao; }
}
