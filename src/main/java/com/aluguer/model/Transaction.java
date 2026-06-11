package com.aluguer.model;

import java.time.LocalDateTime;

public class Transaction {

    public enum Tipo {
        PAGAMENTO,
        RECEBIMENTO
    }

    private int id;
    private int reservaId;
    private int utilizadorId;
    private Tipo tipo;
    private double valor;
    private LocalDateTime data;
    private String descricao;

    // Construtor completo (usado pelo DAO ao mapear da BD)
    public Transaction(int id, int reservaId, int utilizadorId,
                       Tipo tipo, double valor,
                       LocalDateTime data, String descricao) {
        this.id = id;
        this.reservaId = reservaId;
        this.utilizadorId = utilizadorId;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
    }

    // Construtor sem id (para inserir na BD)
    public Transaction(int reservaId, int utilizadorId,
                       Tipo tipo, double valor, String descricao) {
        this.reservaId = reservaId;
        this.utilizadorId = utilizadorId;
        this.tipo = tipo;
        this.valor = valor;
        this.data = LocalDateTime.now();
        this.descricao = descricao;
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReservaId() { return reservaId; }
    public void setReservaId(int reservaId) { this.reservaId = reservaId; }

    public int getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(int utilizadorId) { this.utilizadorId = utilizadorId; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    @Override
    public String toString() {
        return "Transaction #" + id +
                " | Reserva: " + reservaId +
                " | Utilizador: " + utilizadorId +
                " | Tipo: " + tipo +
                " | Valor: " + String.format("%.2f€", valor) +
                " | Data: " + data +
                " | " + descricao;
    }
}
